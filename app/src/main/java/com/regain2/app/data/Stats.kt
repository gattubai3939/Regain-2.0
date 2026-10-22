package com.regain2.app.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tiny per-day counters stored in SharedPreferences. */
object Stats {
    private fun prefs(c: Context) = c.getSharedPreferences("stats", Context.MODE_PRIVATE)
    private fun key(name: String) =
        name + "_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    fun inc(c: Context, name: String) {
        val p = prefs(c)
        p.edit().putInt(key(name), p.getInt(key(name), 0) + 1).apply()
    }

    fun get(c: Context, name: String): Int = prefs(c).getInt(key(name), 0)
}
