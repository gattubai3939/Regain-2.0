package com.regain2.app

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.regain2.app.data.AppDatabase
import com.regain2.app.data.AppRule
import com.regain2.app.data.Stats
import com.regain2.app.data.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GuardService : AccessibilityService() {

    companion object {
        fun isEnabled(ctx: Context): Boolean {
            val enabled = Settings.Secure.getString(
                ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val cn = ComponentName(ctx, GuardService::class.java)
            return enabled.split(':').any {
                it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
            }
        }
        private const val PAUSE_GRACE_MS = 10 * 60_000L
        private const val EMERGENCY_MS = 5 * 60_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repo: UsageRepository
    private lateinit var overlay: Overlay

    private var rules: Map<String, AppRule> = emptyMap()
    private var imePkgs: Set<String> = emptySet()
    private var currentPkg: String? = null
    private val pauseUntil = HashMap<String, Long>()
    private val emergencyUntil = HashMap<String, Long>()
    private val ignored = setOf("com.android.systemui", "android")

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_USER_PRESENT -> Stats.inc(c, "unlocks")
                Intent.ACTION_SCREEN_ON -> Stats.inc(c, "pickups")
            }
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            currentPkg?.let { evaluate(it, false) }
            handler.postDelayed(this, 15_000)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        repo = UsageRepository(this)
        overlay = Overlay(this)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imePkgs = imm.enabledInputMethodList.map { it.packageName }.toSet()

        scope.launch {
            AppDatabase.get(this@GuardService).ruleDao().observeAll().collect { list ->
                rules = list.associateBy { it.packageName }
                currentPkg?.let { evaluate(it, false) }
            }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(this, screenReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        handler.postDelayed(tick, 15_000)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) { currentPkg = null; return }
        if (pkg in ignored || pkg in imePkgs) return
        currentPkg = pkg
        if (overlay.isShowing && overlay.showingFor != pkg) overlay.hide()
        evaluate(pkg, true)
    }

    private fun evaluate(pkg: String, allowPause: Boolean) {
        val rule = rules[pkg] ?: return
        if (overlay.isShowing && overlay.showingFor == pkg) return
        val now = System.currentTimeMillis()
        if ((emergencyUntil[pkg] ?: 0L) > now) return

        if (rule.blocked) {
            showBlock(pkg, "App block hai", "Isse tumne khud block kiya hai. Apne goal par wapas jao.")
            return
        }
        scope.launch {
            if (rule.limitMinutes > 0) {
                val used = withContext(Dispatchers.IO) { repo.usageMillisToday(pkg) }
                if (currentPkg != pkg) return@launch
                if (used >= rule.limitMinutes * 60_000L) {
                    showBlock(pkg, "Daily limit khatam", "Aaj ki ${rule.limitMinutes} minute ki limit poori ho gayi.")
                    return@launch
                }
            }
            if (allowPause && rule.pause && !overlay.isShowing &&
                (pauseUntil[pkg] ?: 0L) <= System.currentTimeMillis()
            ) {
                showPause(pkg)
            }
        }
    }

    private fun labelOf(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) { pkg }

    private fun goHome() {
        overlay.hide()
        currentPkg = null
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun showBlock(pkg: String, title: String, message: String) {
        overlay.showBlocked(pkg, title, message,
            onHome = { goHome() },
            onUnlocked = {
                emergencyUntil[pkg] = System.currentTimeMillis() + EMERGENCY_MS
                Stats.inc(this, "emergency_unlocks")
                overlay.hide()
                currentPkg = pkg
            })
    }

    private fun showPause(pkg: String) {
        Stats.inc(this, "pause_shown")
        overlay.showPause(pkg, labelOf(pkg),
            onBack = { Stats.inc(this, "pause_back"); goHome() },
            onContinue = { reason ->
                Stats.inc(this, "pause_continue_$reason")
                pauseUntil[pkg] = System.currentTimeMillis() + PAUSE_GRACE_MS
                overlay.hide()
                currentPkg = pkg
            })
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        if (::overlay.isInitialized) overlay.hide()
        super.onDestroy()
    }
}
