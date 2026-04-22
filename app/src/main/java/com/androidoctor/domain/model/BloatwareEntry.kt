package com.androidoctor.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BloatwareCatalogEntry(
    @SerialName("pkg") val packageName: String,
    val name: String,
    val severity: Severity = Severity.light,
    val safe: Boolean = true,
    val ramMb: Int = 0,
    val batteryPct: Double = 0.0,
    val notes: String = "",
) {
    @Serializable
    enum class Severity { light, moderate, heavy }
}
