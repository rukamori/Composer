package moe.rukamori.composerapp.domain.usecase

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import moe.rukamori.composerapp.domain.export.TtmlExporter
import moe.rukamori.composerapp.domain.lyrics.LyricsParser
import moe.rukamori.composerapp.domain.model.ComposerLine
import moe.rukamori.composerapp.domain.model.ComposerProject
import moe.rukamori.composerapp.domain.model.ComposerSyllable
import moe.rukamori.composerapp.domain.model.ComposerWord
import moe.rukamori.composerapp.domain.model.LyricsFormat
import moe.rukamori.composerapp.domain.repository.AudioRepository
import moe.rukamori.composerapp.domain.repository.ExportRepository
import moe.rukamori.composerapp.domain.repository.ProjectRepository
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

class ObserveProjectsUseCase
    @Inject
    constructor(
        private val repository: ProjectRepository,
    ) {
        operator fun invoke(): Flow<List<ComposerProject>> = repository.observeProjects()
    }

class CreateProjectUseCase
    @Inject
    constructor(
        private val repository: ProjectRepository,
    ) {
        suspend operator fun invoke(title: String): ComposerProject = ComposerProject.empty(title).also { repository.saveProject(it) }
    }

class SaveProjectUseCase
    @Inject
    constructor(
        private val repository: ProjectRepository,
    ) {
        suspend operator fun invoke(project: ComposerProject) {
            repository.saveProject(project.touch())
        }
    }

class DeleteProjectUseCase
    @Inject
    constructor(
        private val repository: ProjectRepository,
    ) {
        suspend operator fun invoke(id: String) {
            repository.deleteProject(id)
        }
    }

class DuplicateProjectUseCase
    @Inject
    constructor(
        private val repository: ProjectRepository,
    ) {
        suspend operator fun invoke(project: ComposerProject): ComposerProject {
            val now = System.currentTimeMillis()
            val copy =
                project.copy(
                    metadata =
                        project.metadata.copy(
                            id = UUID.randomUUID().toString(),
                            title = "${project.metadata.title} Copy",
                            createdAtMs = now,
                            updatedAtMs = now,
                        ),
                )
            repository.saveProject(copy)
            return copy
        }
    }

class ImportLyricsUseCase
    @Inject
    constructor(
        private val parser: LyricsParser,
        private val repository: ProjectRepository,
    ) {
        suspend operator fun invoke(
            text: String,
            format: LyricsFormat,
            title: String,
        ): ComposerProject = parser.parse(text, format, title).also { repository.saveProject(it) }
    }

class ImportAudioUseCase
    @Inject
    constructor(
        private val audioRepository: AudioRepository,
    ) {
        suspend operator fun invoke(uri: Uri) = audioRepository.inspectAudio(uri)
    }

class ExportTtmlUseCase
    @Inject
    constructor(
        private val repository: ExportRepository,
    ) {
        suspend operator fun invoke(project: ComposerProject): File = repository.writeTtml(project)
    }

class ExportProjectUseCase
    @Inject
    constructor(
        private val repository: ExportRepository,
    ) {
        suspend operator fun invoke(project: ComposerProject): File = repository.writeProject(project)
    }

class PreviewTtmlUseCase
    @Inject
    constructor(
        private val exporter: TtmlExporter,
    ) {
        operator fun invoke(project: ComposerProject): String = exporter.export(project)
    }

class UpdateTimelineWordUseCase
    @Inject
    constructor() {
        operator fun invoke(
            project: ComposerProject,
            lineId: String,
            wordId: String,
            startMs: Long,
            endMs: Long,
            snapEnabled: Boolean,
        ): ComposerProject {
            val line = project.lines.firstOrNull { it.id == lineId } ?: return project
            val index = line.words.indexOfFirst { it.id == wordId }
            if (index < 0) return project
            val previousEnd = line.words.getOrNull(index - 1)?.endMs ?: 0L
            val nextStart = line.words.getOrNull(index + 1)?.startMs ?: Long.MAX_VALUE
            val snappedStart = if (snapEnabled) project.snap(startMs) else startMs
            val snappedEnd = if (snapEnabled) project.snap(endMs) else endMs
            val safeStart = snappedStart.coerceAtLeast(previousEnd)
            val safeEnd = max(safeStart + MinimumWordDurationMs, snappedEnd.coerceAtMost(nextStart))
            return project
                .updateWord(lineId, wordId) { word ->
                    word.copy(startMs = safeStart, endMs = safeEnd)
                }.touch()
        }

        private fun ComposerProject.snap(timeMs: Long): Long {
            val nearest = snapPoints.minByOrNull { abs(it.timeMs - timeMs) } ?: return timeMs
            return if (abs(nearest.timeMs - timeMs) <= SnapThresholdMs) nearest.timeMs else timeMs
        }

        private companion object {
            const val MinimumWordDurationMs = 80L
            const val SnapThresholdMs = 90L
        }
    }

fun ComposerProject.touch(): ComposerProject = copy(metadata = metadata.copy(updatedAtMs = System.currentTimeMillis()))

fun ComposerProject.withAudio(
    uri: String,
    name: String,
    durationMs: Long,
    waveform: moe.rukamori.composerapp.domain.model.WaveformSummary,
): ComposerProject =
    copy(
        metadata =
            metadata.copy(
                audioUri = uri,
                audioName = name,
                durationMs = durationMs,
                updatedAtMs = System.currentTimeMillis(),
            ),
        waveform = waveform,
    )

fun ComposerProject.replaceLines(lines: List<ComposerLine>): ComposerProject = copy(lines = lines).touch()

fun ComposerProject.updateLine(
    lineId: String,
    transform: (ComposerLine) -> ComposerLine,
): ComposerProject = copy(lines = lines.map { if (it.id == lineId) transform(it) else it }).touch()

fun ComposerProject.updateWord(
    lineId: String,
    wordId: String,
    transform: (ComposerWord) -> ComposerWord,
): ComposerProject =
    updateLine(lineId) { line ->
        line.copy(words = line.words.map { if (it.id == wordId) transform(it) else it })
    }

fun ComposerProject.tapNextWord(positionMs: Long): ComposerProject {
    val target =
        lines
            .asSequence()
            .flatMap { line -> line.words.asSequence().map { line.id to it } }
            .firstOrNull { (_, word) -> word.startMs == null || word.endMs == null }
            ?: return this
    return updateWord(target.first, target.second.id) { word ->
        word.copy(startMs = positionMs, endMs = positionMs + max(240L, word.text.length * 80L))
    }
}

fun ComposerProject.splitWord(
    lineId: String,
    wordId: String,
): ComposerProject =
    updateWord(lineId, wordId) { word ->
        val splitIndex = (word.text.length / 2).coerceAtLeast(1).coerceAtMost(word.text.length)
        val parts = listOf(word.text.take(splitIndex), word.text.drop(splitIndex)).filter(String::isNotBlank)
        if (parts.size < 2) {
            word
        } else {
            val start = word.startMs
            val end = word.endMs
            val midpoint = if (start != null && end != null) (start + end) / 2L else null
            word.copy(
                syllables =
                    listOf(
                        ComposerSyllable(UUID.randomUUID().toString(), parts[0], start, midpoint),
                        ComposerSyllable(UUID.randomUUID().toString(), parts[1], midpoint, end),
                    ),
            )
        }
    }

fun ComposerProject.mergeWordWithNext(
    lineId: String,
    wordId: String,
): ComposerProject =
    updateLine(lineId) { line ->
        val index = line.words.indexOfFirst { it.id == wordId }
        if (index < 0 || index >= line.words.lastIndex) return@updateLine line
        val current = line.words[index]
        val next = line.words[index + 1]
        val merged =
            current.copy(
                text = "${current.text}${next.text}",
                endMs = next.endMs ?: current.endMs,
                syllables = emptyList(),
            )
        line.copy(
            text =
                line.words
                    .mapIndexedNotNull { wordIndex, word ->
                        when (wordIndex) {
                            index -> merged.text
                            index + 1 -> null
                            else -> word.text
                        }
                    }.joinToString(" "),
            words =
                line.words.toMutableList().apply {
                    set(index, merged)
                    removeAt(index + 1)
                },
        )
    }
