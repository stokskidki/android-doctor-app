package com.androidoctor.tile

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.androidoctor.MainActivity
import com.androidoctor.R
import com.androidoctor.data.local.dao.ScoreSnapshotDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class DoctorTileService : TileService() {

    @Inject lateinit var snapshotDao: ScoreSnapshotDao

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                android.app.PendingIntent.getActivity(
                    this, 0, intent,
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        scope.launch {
            val snap = withContext(Dispatchers.IO) { runCatching { snapshotDao.latest() }.getOrNull() }
            if (snap == null) {
                tile.label = getString(R.string.app_name)
                tile.subtitle = getString(R.string.tile_tap_to_diagnose)
                tile.state = Tile.STATE_INACTIVE
            } else {
                tile.label = "Score ${snap.overall}"
                tile.subtitle = tierLabel(snap.overall)
                tile.state = Tile.STATE_ACTIVE
            }
            tile.icon = Icon.createWithResource(this@DoctorTileService, R.drawable.ic_tile)
            tile.updateTile()
        }
    }

    private fun tierLabel(score: Int): String = when {
        score >= 85 -> "Healthy"
        score >= 60 -> "OK"
        else -> "Issues"
    }
}
