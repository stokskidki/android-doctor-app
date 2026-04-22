package com.androidoctor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score_snapshots")
data class ScoreSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val overall: Int,
    val battery: Int,
    val storage: Int,
    val memory: Int,
    val cpu: Int,
    val bloatware: Int,
)
