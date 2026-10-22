package com.regain2.app.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.util.Calendar

class UsageRepository(private val context: Context) {

    private val defaultDistracting = setOf(
        "com.google.android.youtube", "com.instagram.android", "com.facebook.katana",
        "com.snapchat.android", "com.twitter.android", "com.reddit.frontpage",
        "com.netflix.mediaclient", "com.zhiliaoapp.musically", "com.pinterest",
        "in.mohalla.sharechat", "com.mxtech.videoplayer.ad"
    )

    fun hasUsagePermission(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun defaultCategory(pkg: String): AppCategory = when {
        pkg == context.packageName -> AppCategory.PRODUCTIVE
        pkg in defaultDistracting -> AppCategory.DISTRACTING
        else -> AppCategory.OTHERS
    }

    /** Today's usage (midnight until now), merged with user's category overrides. */
    fun loadToday(overrides: Map<String, AppCategory>): UsageSummary {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val stats = usm.queryAndAggregateUsageStats(start, System.currentTimeMillis())
        val pm = context.packageManager

        val apps = stats.values
            .filter { it.totalTimeInForeground >= 60_000 }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(it.packageName, 0)).toString()
                } catch (e: Exception) { it.packageName }
                AppUsage(
                    it.packageName, label, it.totalTimeInForeground,
                    overrides[it.packageName] ?: defaultCategory(it.packageName)
                )
            }
            .sortedByDescending { it.millis }

        var d = 0L; var p = 0L; var o = 0L
        for (a in apps) when (a.category) {
            AppCategory.DISTRACTING -> d += a.millis
            AppCategory.PRODUCTIVE -> p += a.millis
            AppCategory.MIXED -> { d += a.millis / 2; p += a.millis / 2 }
            AppCategory.OTHERS -> o += a.millis
        }
        return UsageSummary(d + p + o, d, p, o, apps)
    }
}
