package com.regain2.app

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.random.Random

/** Full-screen overlay drawn by the accessibility service (no extra permission needed). */
class Overlay(private val ctx: Context) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: ScrollView? = null
    private var box: LinearLayout? = null
    private var timer: CountDownTimer? = null

    var showingFor: String? = null
        private set
    val isShowing: Boolean get() = root != null

    private val green = Color.parseColor("#7ED957")
    private val dark = Color.parseColor("#262626")
    private val grey = Color.parseColor("#BBBBBB")

    private fun dp(v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    fun hide() {
        timer?.cancel(); timer = null
        root?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        root = null; box = null; showingFor = null
    }

    private fun open(pkg: String) {
        val sv = ScrollView(ctx).apply {
            setBackgroundColor(Color.parseColor("#0E0E0E"))
            isFillViewport = true
        }
        val b = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(48), dp(28), dp(48))
        }
        sv.addView(b)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        )
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        wm.addView(sv, lp)
        root = sv; box = b; showingFor = pkg
    }

    private fun clear() {
        timer?.cancel(); timer = null
        box?.removeAllViews()
    }

    private fun tv(text: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(ctx).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(8))
        }

    private fun btn(text: String, bg: Int, fg: Int, onClick: () -> Unit): Button =
        Button(ctx).apply {
            this.text = text
            isAllCaps = false
            setTextColor(fg)
            background = GradientDrawable().apply { setColor(bg); cornerRadius = dp(28).toFloat() }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
            ).apply { topMargin = dp(10) }
        }

    private fun setOn(b: Button, on: Boolean) {
        b.isEnabled = on
        b.alpha = if (on) 1f else 0.4f
    }

    // ---------------- Pause screen ----------------

    fun showPause(pkg: String, appName: String, onBack: () -> Unit, onContinue: (String) -> Unit) {
        hide(); open(pkg)
        val b = box ?: return
        b.addView(tv("\u23F8", 48f))
        b.addView(tv("Ruko ek second", 26f, bold = true))
        b.addView(tv("Kyun khol rahe ho $appName?", 16f, grey))
        val count = tv("10", 64f, green, true)
        b.addView(count)
        b.addView(btn("Wapas jao", green, Color.BLACK) { onBack() })
        val r1 = btn("Padhai / zaroori kaam ke liye", dark, Color.WHITE) { onContinue("study") }
        val r2 = btn("Bas timepass", dark, Color.WHITE) { onContinue("timepass") }
        setOn(r1, false); setOn(r2, false)
        b.addView(r1); b.addView(r2)
        timer = object : CountDownTimer(10_000, 1000) {
            override fun onTick(ms: Long) { count.text = ((ms + 999) / 1000).toString() }
            override fun onFinish() {
                count.text = "0"
                setOn(r1, true); setOn(r2, true)
            }
        }.start()
    }

    // ---------------- Block / limit + emergency unlock ----------------

    fun showBlocked(pkg: String, title: String, message: String, onHome: () -> Unit, onUnlocked: () -> Unit) {
        hide(); open(pkg)
        blockedPanel(title, message, onHome, onUnlocked)
    }

    private fun blockedPanel(title: String, message: String, onHome: () -> Unit, onUnlocked: () -> Unit) {
        clear()
        val b = box ?: return
        b.addView(tv("\uD83D\uDEAB", 48f))
        b.addView(tv(title, 26f, bold = true))
        b.addView(tv(message, 16f, grey))
        b.addView(btn("Home jao", green, Color.BLACK) { onHome() })
        b.addView(btn("Emergency unlock", dark, Color.WHITE) {
            emergencyPanel(title, message, onHome, onUnlocked)
        })
    }

    private fun emergencyPanel(title: String, message: String, onHome: () -> Unit, onUnlocked: () -> Unit) {
        clear()
        val b = box ?: return
        b.addView(tv("Emergency unlock", 24f, bold = true))
        b.addView(tv("Unlock karne ke liye thodi mehnat karni padegi. Sach mein zaroori ho tabhi.", 15f, grey))
        b.addView(btn("2 minute wait karo", dark, Color.WHITE) { waitPanel(title, message, onHome, onUnlocked) })
        b.addView(btn("Typing challenge", dark, Color.WHITE) { typingPanel(title, message, onHome, onUnlocked) })
        b.addView(btn("Wapas", green, Color.BLACK) { blockedPanel(title, message, onHome, onUnlocked) })
    }

    private fun waitPanel(title: String, message: String, onHome: () -> Unit, onUnlocked: () -> Unit) {
        clear()
        val b = box ?: return
        b.addView(tv("Wait karo\u2026", 24f, bold = true))
        val count = tv("2:00", 56f, green, true)
        b.addView(count)
        val unlock = btn("Unlock (5 minute)", green, Color.BLACK) { onUnlocked() }
        setOn(unlock, false)
        b.addView(unlock)
        b.addView(btn("Cancel", dark, Color.WHITE) { blockedPanel(title, message, onHome, onUnlocked) })
        timer = object : CountDownTimer(120_000, 1000) {
            override fun onTick(ms: Long) {
                val s = (ms + 999) / 1000
                count.text = String.format("%d:%02d", s / 60, s % 60)
            }
            override fun onFinish() {
                count.text = "0:00"
                setOn(unlock, true)
            }
        }.start()
    }

    private fun typingPanel(title: String, message: String, onHome: () -> Unit, onUnlocked: () -> Unit) {
        clear()
        val b = box ?: return
        val chars = "abcdefghjkmnpqrstuvwxyz23456789"
        val phrase = (1..4).joinToString(" ") { (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("") }
        b.addView(tv("Ye exactly type karo:", 18f, grey))
        b.addView(tv(phrase, 26f, green, true).apply { typeface = Typeface.MONOSPACE })
        val input = EditText(ctx).apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "Yahan type karo"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            gravity = Gravity.CENTER
        }
        b.addView(input)
        val unlock = btn("Unlock (5 minute)", green, Color.BLACK) { onUnlocked() }
        setOn(unlock, false)
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b4: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { setOn(unlock, s.toString().trim() == phrase) }
        })
        b.addView(unlock)
        b.addView(btn("Cancel", dark, Color.WHITE) { blockedPanel(title, message, onHome, onUnlocked) })
    }
}
