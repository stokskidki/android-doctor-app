package com.androidoctor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.androidoctor.data.local.dao.BenchmarkDao
import com.androidoctor.data.local.dao.ScoreSnapshotDao
import com.androidoctor.data.local.entity.BenchmarkEntity
import com.androidoctor.data.local.entity.ScoreSnapshotEntity

@Database(
    entities = [BenchmarkEntity::class, ScoreSnapshotEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun benchmarkDao(): BenchmarkDao
    abstract fun scoreSnapshotDao(): ScoreSnapshotDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS score_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        overall INTEGER NOT NULL,
                        battery INTEGER NOT NULL,
                        storage INTEGER NOT NULL,
                        memory INTEGER NOT NULL,
                        cpu INTEGER NOT NULL,
                        bloatware INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_score_snapshots_timestamp ON score_snapshots(timestamp DESC)")
            }
        }
    }
}
