package moe.rukamori.composerapp.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.rukamori.composerapp.domain.model.ComposerProject
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class ProjectEnvelope(
    val schemaVersion: Int,
    val project: ComposerProject,
)

@Singleton
class ProjectJsonCodec @Inject constructor() {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun encode(project: ComposerProject): String =
        json.encodeToString(ProjectEnvelope(schemaVersion = CurrentSchemaVersion, project = project))

    fun decode(value: String): ComposerProject {
        val envelope = json.decodeFromString<ProjectEnvelope>(value)
        return when (envelope.schemaVersion) {
            CurrentSchemaVersion -> envelope.project
            else -> envelope.project
        }
    }

    private companion object {
        const val CurrentSchemaVersion = 1
    }
}
