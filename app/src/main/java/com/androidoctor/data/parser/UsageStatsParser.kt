package com.androidoctor.data.parser

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class HeavyApp(
    val packageName: String,
    val appLabel: String,
    val foregroundTimeMs: Long,
    val lastUsedMs: Long,
)

@Singleton
class UsageStatsParser @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun topHeavyApps(days: Int = 7, limit: Int = 15): List<HeavyApp> = withContext(Dispatchers.IO) {
        if (!hasUsageAccess()) return@withContext emptyList()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext emptyList()

        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(days.toLong())
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            ?: return@withContext emptyList()

        val byPkg = stats
            .filter { it.totalTimeInForeground > 0 }
            .groupBy { it.packageName }
            .mapValues { (_, list) ->
                val fgTotal = list.sumOf { it.totalTimeInForeground }
                val lastUsed = list.maxOf { it.lastTimeUsed }
                fgTotal to lastUsed
            }

        val pm = context.packageManager
        byPkg.entries
            .sortedByDescending { it.value.first }
            .take(limit)
            .mapNotNull { (pkg, pair) ->
                val label = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault(pkg)
                HeavyApp(
                    packageName = pkg,
                    appLabel = label,
                    foregroundTimeMs = pair.first,
                    lastUsedMs = pair.second,
                )
            }
    }
}
