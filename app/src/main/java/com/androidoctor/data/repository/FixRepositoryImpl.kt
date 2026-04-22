package com.androidoctor.data.repository

import android.content.Context
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.domain.repository.FixRepository
import com.androidoctor.domain.repository.PanicBoostResult
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixRepositoryImpl @Inject constructor(
    private val shell: ShellExecutor,
    @ApplicationContext private val context: Context,
) : FixRepository {

    private val snapshotFile: File
        get() = File(context.filesDir, "rollback_snapshot.json")

    private val changes = mutableListOf<JSONObject>()

    private fun recordChange(action: String, target: String, original: String, newValue: String) {
        val change = JSONObject().apply {
            put("action", action)
            put("target", target)
            put("original", original)
            put("new", newValue)
        }
        changes.add(change)
        saveSnapshot()
    }

    private fun saveSnapshot() {
        val arr = JSONArray()
        changes.forEach { arr.put(it) }
        snapshotFile.writeText(arr.toString(2))
    }

    override suspend fun disablePackage(packageName: String): Boolean {
        val result = shell.exec("pm disable-user --user 0 $packageName")
        if (result.isSuccess || "disabled" in result.output.lowercase()) {
            shell.exec("am force-stop $packageName")
            recordChange("disable_package", packageName, "enabled", "disabled")
            return true
        }
        return false
    }

    override suspend fun enablePackage(packageName: String): Boolean {
        val result = shell.exec("pm enable $packageName")
        return result.isSuccess || "enabled" in result.output.lowercase()
    }

    override suspend fun setAnimationScale(scale: Float): Boolean {
        var success = true
        for (setting in listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")) {
            val original = shell.exec("settings get global $setting").output
            val result = shell.exec("settings put global $setting $scale")
            if (result.isSuccess) {
                recordChange("set_setting", "global:$setting", original, scale.toString())
            } else {
                success = false
            }
        }
        return success
    }

    override suspend fun setBackgroundProcessLimit(limit: Int): Boolean {
        val original = shell.exec("settings get global background_process_limit").output
        val result = shell.exec("settings put global background_process_limit $limit")
        if (result.isSuccess) {
            recordChange("set_setting", "global:background_process_limit", original, limit.toString())
            return true
        }
        return false
    }

    override suspend fun restrictBackground(packageName: String): Boolean {
        val result = shell.exec("cmd appops set $packageName RUN_IN_BACKGROUND deny")
        if (result.isSuccess) {
            recordChange("restrict_background", packageName, "allowed", "denied")
            return true
        }
        return false
    }

    override suspend fun trimCaches(): Long {
        val dfBefore = shell.exec("df /data").output
        shell.exec("pm trim-caches 1099511627776")
        val dfAfter = shell.exec("df /data").output

        // Parse available space difference
        fun parseAvail(df: String): Long {
            for (line in df.lines()) {
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 4 && parts[0] != "Filesystem") {
                    return parts[3].toLongOrNull() ?: continue
                }
            }
            return 0
        }

        val before = parseAvail(dfBefore)
        val after = parseAvail(dfAfter)
        return (after - before).coerceAtLeast(0) * 1024 // KB to bytes
    }

    override suspend fun fstrimData(): Boolean {
        // fstrim recovers write speed on worn eMMC/UFS. Reqs root or signature perm;
        // Shizuku-shell works on most devices.
        val result = shell.exec("fstrim -v /data", timeoutMs = 60_000)
        return result.isSuccess
    }

    override suspend fun killAllApps(): Int {
        // am kill-all force-stops all cached background processes (empty + cached).
        // Safer than `killall` — Android restarts anything essential.
        val before = shell.exec("dumpsys procstats --current 2>/dev/null | wc -l").output.trim().toIntOrNull() ?: 0
        shell.exec("am kill-all")
        val after = shell.exec("dumpsys procstats --current 2>/dev/null | wc -l").output.trim().toIntOrNull() ?: before
        return (before - after).coerceAtLeast(0)
    }

    override suspend fun dropKernelCaches(): Boolean {
        // Usually blocked without root, but worth trying via Shizuku.
        val result = shell.exec("sync && echo 3 > /proc/sys/vm/drop_caches")
        return result.isSuccess
    }

    override suspend fun reoptimizeTopApps(packages: List<String>): Int {
        var count = 0
        for (pkg in packages.take(10)) {
            val r = shell.exec("cmd package compile -m speed -f $pkg", timeoutMs = 60_000)
            if (r.isSuccess) count++
        }
        return count
    }

    override suspend fun setCpuGovernor(governor: String): Int {
        // Write `governor` to each online CPU's scaling_governor.
        // Valid: performance, powersave, ondemand, schedutil, interactive.
        var count = 0
        val cpus = shell.exec("ls /sys/devices/system/cpu/").output
            .split("\\s+".toRegex())
            .filter { it.matches(Regex("cpu\\d+")) }
        for (cpu in cpus) {
            val path = "/sys/devices/system/cpu/$cpu/cpufreq/scaling_governor"
            val original = shell.exec("cat $path").output.trim()
            val r = shell.exec("echo $governor > $path")
            if (r.isSuccess) {
                recordChange("cpu_governor", cpu, original, governor)
                count++
            }
        }
        return count
    }

    override suspend fun getCurrentCpuGovernor(): String? {
        val r = shell.exec("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
        return if (r.isSuccess) r.output.trim().ifBlank { null } else null
    }

    override suspend fun panicBoost(): PanicBoostResult {
        val killed = killAllApps()
        val freed = trimCaches()
        val dropped = dropKernelCaches()
        val animOk = setAnimationScale(0.0f)
        return PanicBoostResult(
            processesKilled = killed,
            cacheFreedBytes = freed,
            kernelCachesDropped = dropped,
            animationsZeroed = animOk,
        )
    }

    override suspend fun rollbackAll(): Int {
        if (!snapshotFile.exists()) return 0

        val arr = JSONArray(snapshotFile.readText())
        var restored = 0

        for (i in arr.length() - 1 downTo 0) {
            val change = arr.getJSONObject(i)
            val action = change.getString("action")
            val target = change.getString("target")
            val original = change.getString("original")

            val success = when (action) {
                "disable_package" -> enablePackage(target)
                "set_setting" -> {
                    val (ns, key) = target.split(":", limit = 2)
                    if (original in listOf("-1", "null")) {
                        shell.exec("settings delete $ns $key").isSuccess
                    } else {
                        shell.exec("settings put $ns $key $original").isSuccess
                    }
                }
                "restrict_background" -> {
                    shell.exec("cmd appops set $target RUN_IN_BACKGROUND allow").isSuccess
                }
                "cpu_governor" -> {
                    val path = "/sys/devices/system/cpu/$target/cpufreq/scaling_governor"
                    shell.exec("echo $original > $path").isSuccess
                }
                else -> false
            }
            if (success) restored++
        }

        snapshotFile.delete()
        changes.clear()
        return restored
    }

    override fun hasRollbackSnapshot(): Boolean = snapshotFile.exists() && snapshotFile.length() > 2
}
