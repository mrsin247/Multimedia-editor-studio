package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ProjectItem
import com.example.model.StudioType

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val studioType: String,
    val updatedAt: Long,
    val description: String,
    val durationOrLayers: String
) {
    fun toProjectItem(): ProjectItem {
        val type = try {
            StudioType.valueOf(studioType)
        } catch (e: Exception) {
            StudioType.PHOTO
        }
        return ProjectItem(
            id = id,
            title = title,
            type = type,
            updatedAt = updatedAt,
            description = description,
            durationOrLayers = durationOrLayers
        )
    }

    companion object {
        fun fromProjectItem(item: ProjectItem): ProjectEntity {
            return ProjectEntity(
                id = item.id,
                title = item.title,
                studioType = item.type.name,
                updatedAt = item.updatedAt,
                description = item.description,
                durationOrLayers = item.durationOrLayers
            )
        }
    }
}
