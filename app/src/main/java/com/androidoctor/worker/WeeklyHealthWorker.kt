package com.androidoctor.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.androidoctor.DoctorApp
import com.androidoctor.MainActivity
import com.androidoctor.R
import com.androidoctor.data.local.dao.ScoreSnapshotDao
import com.androidoctor.domain.usecase.DiagnoseUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class WeeklyHealthWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val diagnose: DiagnoseUseCase,
    private val snapshotDao: ScoreSnapshotDao,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val previous = snapshotDao.latest()
            val result = diagnose()
            val current = snapshotDao.latest()
            val variant = selectVariant(previous, current, result.verdict.overallScore, result.verdict.topIssues.size)
            postNotification(variant)
            Result.success()
        }.getOrElse {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun selectVariant(
        previous: com.androidoctor.data.local.entity.ScoreSnapshotEntity?,
        current: com.androidoctor.data.local.entity.ScoreSnapshotEntity?,
        overallScore: Int,
        issueCount: Int,
    ): NotifVariant {
        val prevScore = previous?.overall ?: overallScore
        val delta = overallScore - prevScore
        return when {
            delta <= -5 -> NotifVariant.Trend(-delta)
            issueCount >= 3 -> NotifVariant.Issues(issueCount)
            delta in -4..4 && overallScore >= 75 -> NotifVariant.Praise(overallScore)
            else -> NotifVariant.Action
        }
    }

    private fun postNotification(variant: NotifVariant) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val (title, body) = when (variant) {
            is NotifVariant.Issues -> ctx.getString(R.string.weekly_notif_title_issues, variant.count) to ctx.getString(R.string.weekly_notif_body)
            is NotifVariant.Trend -> ctx.getString(R.string.weekly_notif_title_trend, variant.dropPts) to ctx.getString(R.string.weekly_notif_body)
            is NotifVariant.Action -> ctx.getString(R.string.weekly_notif_title_action) to ctx.getString(R.string.weekly_notif_body)
            is NotifVariant.Praise -> ctx.getString(R.string.weekly_notif_title_praise, variant.score) to ctx.getString(R.string.weekly_notif_body)
        }

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(ctx, DoctorApp.WEEKLY_HEALTH_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(ctx).notify(NOTIF_ID, notif)
    }

    sealed class NotifVariant {
        data class Issues(val count: Int) : NotifVariant()
        data class Trend(val dropPts: Int) : NotifVariant()
        data object Action : NotifVariant()
        data class Praise(val score: Int) : NotifVariant()
    }

    companion object {
        const val NOTIF_ID = 1001
        val BACKOFF_DELAY_MS = TimeUnit.HOURS.toMillis(6)
    }
}
