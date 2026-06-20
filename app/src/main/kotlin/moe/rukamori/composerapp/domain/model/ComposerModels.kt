package moe.rukamori.composerapp.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ComposerProject(
    val metadata: ProjectMetadata,
    val agents: List<ComposerAgent>,
    val lines: List<ComposerLine>,
    val linkedGroups: List<LinkedGroup>,
    val snapPoints: List<SnapPoint>,
    val waveform: WaveformSummary?,
) {
    companion object {
        fun empty(title: String): ComposerProject {
            val now = System.currentTimeMillis()
            return ComposerProject(
                metadata =
                    ProjectMetadata(
                        id = UUID.randomUUID().toString(),
                        title = title.trim().ifBlank { "Untitled" },
                        artist = "",
                        album = "",
                        audioUri = null,
                        audioName = null,
                        durationMs = 0L,
                        createdAtMs = now,
                        updatedAtMs = now,
                    ),
                agents = listOf(ComposerAgent.Default),
                lines = emptyList(),
                linkedGroups = emptyList(),
                snapPoints = emptyList(),
                waveform = null,
            )
        }
    }
}

@Serializable
data class ProjectMetadata(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val audioUri: String?,
    val audioName: String?,
    val durationMs: Long,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

@Serializable
data class ComposerLine(
    val id: String,
    val text: String,
    val agentId: String,
    val isBackground: Boolean,
    val linkedGroupId: String?,
    val words: List<ComposerWord>,
)

@Serializable
data class ComposerWord(
    val id: String,
    val text: String,
    val startMs: Long?,
    val endMs: Long?,
    val syllables: List<ComposerSyllable>,
)

@Serializable
data class ComposerSyllable(
    val id: String,
    val text: String,
    val startMs: Long?,
    val endMs: Long?,
)

@Serializable
data class ComposerAgent(
    val id: String,
    val name: String,
    val colorArgb: Long,
) {
    companion object {
        val Default =
            ComposerAgent(
                id = "agent-main",
                name = "Main",
                colorArgb = 0xFFBA1A1AL,
            )
    }
}

@Serializable
data class LinkedGroup(
    val id: String,
    val name: String,
    val lineIds: List<String>,
)

@Serializable
data class SnapPoint(
    val id: String,
    val timeMs: Long,
    val label: String,
)

@Serializable
data class TimelineSelection(
    val lineId: String?,
    val wordId: String?,
)

@Serializable
data class WaveformSummary(
    val durationMs: Long,
    val peaks: List<Float>,
)

enum class LyricsFormat {
    PLAIN_TEXT,
    LRC,
    SRT,
    TTML,
}

data class AudioMetadata(
    val uri: String,
    val displayName: String,
    val durationMs: Long,
    val waveform: WaveformSummary,
)
