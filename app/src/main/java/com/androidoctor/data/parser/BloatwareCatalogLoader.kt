package com.androidoctor.data.parser

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import java.util.Locale
import com.androidoctor.domain.model.BloatwareCatalogEntry
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class BloatwareCatalogResult(
    val oem: String,
    val country: String,
    val entries: List<BloatwareCatalogEntry>,
    val installedEntries: List<BloatwareCatalogEntry>,
)

@Singleton
class BloatwareCatalogLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var cache: BloatwareCatalogResult? = null

    suspend fun load(): BloatwareCatalogResult = withContext(Dispatchers.IO) {
        cache?.let { return@withContext it }

        val oem = detectOem()
        val country = detectCountry()
        val entries = buildList {
            addAll(loadFile("generic.json"))
            addAll(loadFile("$oem.json"))
            if (country.isNotEmpty()) {
                addAll(loadFile("country-$country.json"))
            }
        }.distinctBy { it.packageName }

        val pm = context.packageManager
        val installed = entries.filter { entry ->
            runCatching { pm.getPackageInfo(entry.packageName, 0) }.isSuccess
        }

        BloatwareCatalogResult(
            oem = oem,
            country = country,
            entries = entries,
            installedEntries = installed,
        ).also { cache = it }
    }

    private fun detectCountry(): String {
        // Prefer SIM country (carrier preload matches), fall back to locale
        val tm = runCatching {
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        }.getOrNull()
        val simCountry = tm?.networkCountryIso?.lowercase(Locale.ROOT) ?: ""
        if (simCountry.isNotBlank()) return simCountry
        return Locale.getDefault().country.lowercase(Locale.ROOT)
    }

    private fun detectOem(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("samsung") -> "samsung"
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> "xiaomi"
            manufacturer.contains("oneplus") -> "oneplus"
            manufacturer.contains("oppo") -> "oppo"
            manufacturer.contains("realme") -> "realme"
            else -> "generic"
        }
    }

    private fun loadFile(name: String): List<BloatwareCatalogEntry> = runCatching {
        context.assets.open("bloatware/$name").use { stream ->
            json.decodeFromString<List<BloatwareCatalogEntry>>(stream.bufferedReader().readText())
        }
    }.getOrElse { e ->
        Firebase.crashlytics.recordException(RuntimeException("bloatware catalog parse failed: $name", e))
        emptyList()
    }
}
