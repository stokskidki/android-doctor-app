package com.androidoctor.domain.model

data class AppBatteryUsage(
    val packageName: String,
    val appLabel: String,
    val consumedPowerMah: Double,
    val percentOfTotal: Double,
)

data class BatteryAttribution(
    val apps: List<AppBatteryUsage>,
    val source: Source,
    val sinceMs: Long,
    val totalPowerMah: Double,
) {
    enum class Source { NATIVE_API_31, SHIZUKU_DUMPSYS, UNAVAILABLE }
}
