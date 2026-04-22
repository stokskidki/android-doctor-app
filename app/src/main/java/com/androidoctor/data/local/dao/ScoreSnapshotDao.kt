package com.androidoctor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.androidoctor.data.local.entity.ScoreSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreSnapshotDao {

    @Insert
    suspend fun insert(snapshot: ScoreSnapshotEntity): Long

    @Query("SELECT * FROM score_snapshots WHERE timestamp >= :sinceMs ORDER BY timestamp ASC")
    fun observeSince(sinceMs: Long): Flow<List<ScoreSnapshotEntity>>

    @Query("SELECT * FROM score_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(): ScoreSnapshotEntity?

    @Query("SELECT * FROM score_snapshots WHERE timestamp >= :sinceMs ORDER BY timestamp ASC")
    suspend fun since(sinceMs: Long): List<ScoreSnapshotEntity>

    @Query("SELECT COUNT(*) FROM score_snapshots")
    suspend fun count(): Int

    @Query("DELETE FROM score_snapshots WHERE timestamp < :beforeMs")
    suspend fun pruneBefore(beforeMs: Long): Int

    @Query("DELETE FROM score_snapshots WHERE id IN (SELECT id FROM score_snapshots ORDER BY timestamp ASC LIMIT :n)")
    suspend fun deleteOldest(n: Int): Int
}
