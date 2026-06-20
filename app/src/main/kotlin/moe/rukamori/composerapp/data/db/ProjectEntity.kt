package moe.rukamori.composerapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val updatedAtMs: Long,
    val payloadJson: String,
)
