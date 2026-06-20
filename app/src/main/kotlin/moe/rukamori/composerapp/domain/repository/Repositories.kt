package moe.rukamori.composerapp.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import moe.rukamori.composerapp.domain.model.AudioMetadata
import moe.rukamori.composerapp.domain.model.ComposerProject
import java.io.File

interface ProjectRepository {
    fun observeProjects(): Flow<List<ComposerProject>>

    suspend fun getProject(id: String): ComposerProject?

    suspend fun saveProject(project: ComposerProject)

    suspend fun deleteProject(id: String)
}

interface AudioRepository {
    suspend fun inspectAudio(uri: Uri): AudioMetadata
}

interface ExportRepository {
    suspend fun writeTtml(project: ComposerProject): File

    suspend fun writeProject(project: ComposerProject): File
}

interface SettingsRepository {
    val settings: Flow<ComposerSettings>

    suspend fun setSnapEnabled(enabled: Boolean)

    suspend fun setUseDynamicColor(enabled: Boolean)
}

data class ComposerSettings(
    val snapEnabled: Boolean,
    val useDynamicColor: Boolean,
)
