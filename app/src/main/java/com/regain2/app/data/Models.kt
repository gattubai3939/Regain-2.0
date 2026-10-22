package com.regain2.app.data

enum class AppCategory(val label: String) {
    DISTRACTING("Distracting"),
    PRODUCTIVE("Productive"),
    MIXED("Half & Half"),
    OTHERS("Others")
}

data class AppUsage(
    val packageName: String,
    val label: String,
    val millis: Long,
    val category: AppCategory
)

data class UsageSummary(
    val totalMillis: Long = 0,
    val distractingMillis: Long = 0,
    val productiveMillis: Long = 0,
    val othersMillis: Long = 0,
    val apps: List<AppUsage> = emptyList()
)
