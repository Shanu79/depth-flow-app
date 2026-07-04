package com.shin.depthflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val imageUrl: String,
    val videoUrl: String,
    val timestamp: Long,
    val motionPath: String,
    val intensity: Float
)







