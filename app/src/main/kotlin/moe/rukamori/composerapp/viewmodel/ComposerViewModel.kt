package moe.rukamori.composerapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.composerapp.R
import moe.rukamori.composerapp.domain.model.ComposerAgent
import moe.rukamori.composerapp.domain.model.ComposerLine
import moe.rukamori.composerapp.domain.model.ComposerProject
import moe.rukamori.composerapp.domain.model.ComposerWord
import moe.rukamori.composerapp.domain.model.LyricsFormat
import moe.rukamori.composerapp.domain.model.TimelineSelection
import moe.rukamori.composerapp.domain.repository.SettingsRepository
import moe.rukamori.composerapp.domain.usecase.CreateProjectUseCase
import moe.rukamori.composerapp.domain.usecase.DeleteProjectUseCase
import moe.rukamori.composerapp.domain.usecase.DuplicateProjectUseCase
import moe.rukamori.composerapp.domain.usecase.ExportProjectUseCase
import moe.rukamori.composerapp.domain.usecase.ExportTtmlUseCase
import moe.rukamori.composerapp.domain.usecase.ImportAudioUseCase
import moe.rukamori.composerapp.domain.usecase.ImportLyricsUseCase
import moe.rukamori.composerapp.domain.usecase.ObserveProjectsUseCase
import moe.rukamori.composerapp.domain.usecase.PreviewTtmlUseCase
import moe.rukamori.composerapp.domain.usecase.SaveProjectUseCase
import moe.rukamori.composerapp.domain.usecase.UpdateTimelineWordUseCase
import moe.rukamori.composerapp.domain.usecase.mergeWordWithNext
import moe.rukamori.composerapp.domain.usecase.splitWord
import moe.rukamori.composerapp.domain.usecase.tapNextWord
import moe.rukamori.composerapp.domain.usecase.touch
import moe.rukamori.composerapp.domain.usecase.updateLine
import moe.rukamori.composerapp.domain.usecase.withAudio
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max

sealed interface ProjectsScreenState {
    data object Loading : ProjectsScreenState

    data class Success(
        val model: ProjectsUiModel,
    ) : ProjectsScreenState

    data object Empty : ProjectsScreenState

    data class Error(
        @StringRes val messageResId: Int,
    ) : ProjectsScreenState
}

sealed interface ImportScreenState {
    data object Loading : ImportScreenState

    data class Success(
        val model: ImportUiModel,
    ) : ImportScreenState

    data object Empty : ImportScreenState

    data class Error(
        @StringRes val messageResId: Int,
    ) : ImportScreenState
}

sealed interface EditorScreenState {
    data object Loading : EditorScreenState

    data class Success(
        val model: EditorUiModel,
    ) : EditorScreenState

    data object Empty : EditorScreenState

    data class Error(
        @StringRes val messageResId: Int,
    ) : EditorScreenState
}

sealed interface SyncScreenState {
    data object Loading : SyncScreenState

    data class Success(
        val model: SyncUiModel,
    ) : SyncScreenState

    data object Empty : SyncScreenState

    data class Error(
        @StringRes val messageResId: Int,
    ) : SyncScreenState
}

sealed interface TimelineScreenState {
    data object Loading : TimelineScreenState

    data class Success(
        val model: TimelineUiModel,
    ) : TimelineScreenState

    data object Empty : TimelineScreenState

    data class Error(
        @StringRes val messageResId: Int,
    ) : TimelineScreenState
}

sealed interface PreviewScreenState {
    data object Loading : PreviewScreenState

    data class Success(
        val model: PreviewUiModel,
    ) : PreviewScreenState

    data object Empty : PreviewScreenState

    data class Error(
        @StringRes val messageResId: Int,
    ) : PreviewScreenState
}

sealed interface ExportScreenState {
    data object Loading : ExportScreenState

    data class Success(
        val model: ExportUiModel,
    ) : ExportScreenState

    data object Empty : ExportScreenState

    data class Error(
        @StringRes val messageResId: Int,
    ) : ExportScreenState
}

sealed interface SettingsScreenState {
    data object Loading : SettingsScreenState

    data class Success(
        val model: SettingsUiModel,
    ) : SettingsScreenState

    data object Empty : SettingsScreenState

    data class Error(
        @StringRes val messageResId: Int,
    ) : SettingsScreenState
}

@Immutable
data class ProjectsUiModel(
    val projects: ProjectSummaryCollection,
    val activeProjectId: String?,
    val pendingDeleteProjectId: String?,
)

@Immutable
data class ProjectSummaryUiModel(
    val id: String,
    val title: String,
    val artist: String,
    val updatedAtMs: Long,
    val lineCount: Int,
    val wordCount: Int,
    val durationLabel: String,
)

@Immutable
data class ProjectSummaryCollection private constructor(
    private val values: List<ProjectSummaryUiModel>,
) {
    val size: Int get() = values.size
    val isEmpty: Boolean get() = values.isEmpty()

    operator fun get(index: Int): ProjectSummaryUiModel = values[index]

    fun asList(): List<ProjectSummaryUiModel> = values

    companion object {
        val Empty = ProjectSummaryCollection(emptyList())

        fun from(values: List<ProjectSummaryUiModel>) = ProjectSummaryCollection(values.toList())
    }
}

@Immutable
data class ImportUiModel(
    val activeProjectTitle: String?,
    val activeAudioName: String?,
    val selectedFormat: LyricsFormat,
    val lyricsText: String,
    val titleDraft: String,
    val artistDraft: String,
    val albumDraft: String,
    val isImporting: Boolean,
)

@Immutable
data class EditorUiModel(
    val projectTitle: String,
    val artist: String,
    val album: String,
    val lineCount: Int,
    val wordCount: Int,
    val agents: AgentUiCollection,
    val lines: LineUiCollection,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val selection: TimelineSelection,
)

@Immutable
data class AgentUiModel(
    val id: String,
    val name: String,
    val colorArgb: Long,
)

@Immutable
data class AgentUiCollection private constructor(
    private val values: List<AgentUiModel>,
) {
    val size: Int get() = values.size

    operator fun get(index: Int): AgentUiModel = values[index]

    fun asList(): List<AgentUiModel> = values

    companion object {
        fun from(values: List<AgentUiModel>) = AgentUiCollection(values.toList())
    }
}

@Immutable
data class LineUiModel(
    val id: String,
    val text: String,
    val agentId: String,
    val agentName: String,
    val isBackground: Boolean,
    val words: WordUiCollection,
)

@Immutable
data class LineUiCollection private constructor(
    private val values: List<LineUiModel>,
) {
    val size: Int get() = values.size
    val isEmpty: Boolean get() = values.isEmpty()

    operator fun get(index: Int): LineUiModel = values[index]

    fun asList(): List<LineUiModel> = values

    companion object {
        val Empty = LineUiCollection(emptyList())

        fun from(values: List<LineUiModel>) = LineUiCollection(values.toList())
    }
}

@Immutable
data class WordUiModel(
    val id: String,
    val text: String,
    val startMs: Long?,
    val endMs: Long?,
    val timeLabel: String,
    val isSelected: Boolean,
    val syllableCount: Int,
)

@Immutable
data class WordUiCollection private constructor(
    private val values: List<WordUiModel>,
) {
    val size: Int get() = values.size
    val isEmpty: Boolean get() = values.isEmpty()

    operator fun get(index: Int): WordUiModel = values[index]

    fun asList(): List<WordUiModel> = values

    companion object {
        fun from(values: List<WordUiModel>) = WordUiCollection(values.toList())
    }
}

@Immutable
data class SyncUiModel(
    val projectTitle: String,
    val audioName: String?,
    val playbackPositionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val nextWord: WordUiModel?,
    val selectedWord: WordUiModel?,
    val canUndo: Boolean,
    val canRedo: Boolean,
)

@Immutable
data class TimelineUiModel(
    val projectTitle: String,
    val durationMs: Long,
    val playbackPositionMs: Long,
    val waveformPeaks: List<Float>,
    val lines: LineUiCollection,
    val selectedLineId: String?,
    val selectedWordId: String?,
    val snapEnabled: Boolean,
)

@Immutable
data class PreviewUiModel(
    val projectTitle: String,
    val artist: String,
    val playbackPositionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val activeLine: LineUiModel?,
    val upcomingLine: LineUiModel?,
)

@Immutable
data class ExportUiModel(
    val projectTitle: String,
    val ttmlText: String,
    val projectJsonAvailable: Boolean,
    val lastExportLabel: String?,
    val isExporting: Boolean,
)

@Immutable
data class SettingsUiModel(
    val snapEnabled: Boolean,
    val useDynamicColor: Boolean,
)

sealed interface ComposerEffect {
    data class ShareFile(
        val file: File,
        val mimeType: String,
    ) : ComposerEffect

    data class ShowMessage(
        @StringRes val messageResId: Int,
    ) : ComposerEffect
}

@HiltViewModel
class ComposerViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val observeProjectsUseCase: ObserveProjectsUseCase,
        private val createProjectUseCase: CreateProjectUseCase,
        private val saveProjectUseCase: SaveProjectUseCase,
        private val deleteProjectUseCase: DeleteProjectUseCase,
        private val duplicateProjectUseCase: DuplicateProjectUseCase,
        private val importLyricsUseCase: ImportLyricsUseCase,
        private val importAudioUseCase: ImportAudioUseCase,
        private val updateTimelineWord: UpdateTimelineWordUseCase,
        private val exportTtmlUseCase: ExportTtmlUseCase,
        private val exportProjectUseCase: ExportProjectUseCase,
        private val previewTtml: PreviewTtmlUseCase,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val player = ExoPlayer.Builder(context).build()
        private val undoStack = ArrayDeque<ComposerProject>()
        private val redoStack = ArrayDeque<ComposerProject>()

        private val _projectsState = MutableStateFlow<ProjectsScreenState>(ProjectsScreenState.Loading)
        val projectsState: StateFlow<ProjectsScreenState> = _projectsState.asStateFlow()

        private val _importState = MutableStateFlow<ImportScreenState>(ImportScreenState.Loading)
        val importState: StateFlow<ImportScreenState> = _importState.asStateFlow()

        private val _editorState = MutableStateFlow<EditorScreenState>(EditorScreenState.Loading)
        val editorState: StateFlow<EditorScreenState> = _editorState.asStateFlow()

        private val _syncState = MutableStateFlow<SyncScreenState>(SyncScreenState.Loading)
        val syncState: StateFlow<SyncScreenState> = _syncState.asStateFlow()

        private val _timelineState = MutableStateFlow<TimelineScreenState>(TimelineScreenState.Loading)
        val timelineState: StateFlow<TimelineScreenState> = _timelineState.asStateFlow()

        private val _previewState = MutableStateFlow<PreviewScreenState>(PreviewScreenState.Loading)
        val previewState: StateFlow<PreviewScreenState> = _previewState.asStateFlow()

        private val _exportState = MutableStateFlow<ExportScreenState>(ExportScreenState.Loading)
        val exportState: StateFlow<ExportScreenState> = _exportState.asStateFlow()

        private val _settingsState = MutableStateFlow<SettingsScreenState>(SettingsScreenState.Loading)
        val settingsState: StateFlow<SettingsScreenState> = _settingsState.asStateFlow()

        private val _effects = MutableSharedFlow<ComposerEffect>(extraBufferCapacity = 4)
        val effects: SharedFlow<ComposerEffect> = _effects.asSharedFlow()

        private var projects: List<ComposerProject> = emptyList()
        private var activeProject: ComposerProject? = null
        private var selection = TimelineSelection(lineId = null, wordId = null)
        private var settings = SettingsUiModel(snapEnabled = true, useDynamicColor = true)
        private var importDraft = ImportDraft()
        private var playbackPositionMs = 0L
        private var isPlaying = false
        private var pendingDeleteProjectId: String? = null
        private var isImporting = false
        private var isExporting = false
        private var lastExportLabel: String? = null
        private var writeJob: Job? = null
        private var playbackJob: Job? = null

        init {
            observeSavedProjects()
            observeSettings()
            observePlayback()
            updateAllStates()
        }

        fun requestDeleteProject(id: String) {
            pendingDeleteProjectId = id
            updateProjectsState()
        }

        fun dismissDeleteProject() {
            pendingDeleteProjectId = null
            updateProjectsState()
        }

        fun createProject(title: String) {
            if (writeJob?.isActive == true) return
            writeJob =
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching { createProjectUseCase(title) }
                        .onSuccess { project ->
                            activeProject = project
                            undoStack.clear()
                            redoStack.clear()
                            importDraft = importDraft.copy(title = project.metadata.title)
                            updatePlayerSource(project)
                            updateAllStates()
                        }.onFailure(::handleFailure)
                }
        }

        fun openProject(id: String) {
            projects.firstOrNull { it.metadata.id == id }?.let { project ->
                activeProject = project
                undoStack.clear()
                redoStack.clear()
                selection = TimelineSelection(null, null)
                importDraft =
                    importDraft.copy(
                        title = project.metadata.title,
                        artist = project.metadata.artist,
                        album = project.metadata.album,
                    )
                viewModelScope.launch { updatePlayerSource(project) }
                updateAllStates()
            }
        }

        fun duplicateActiveProject(id: String) {
            val source = projects.firstOrNull { it.metadata.id == id } ?: return
            if (writeJob?.isActive == true) return
            writeJob =
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching { duplicateProjectUseCase(source) }
                        .onSuccess { copy ->
                            activeProject = copy
                            updatePlayerSource(copy)
                            updateAllStates()
                        }.onFailure(::handleFailure)
                }
        }

        fun confirmDeleteProject() {
            val id = pendingDeleteProjectId ?: return
            if (writeJob?.isActive == true) return
            writeJob =
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching { deleteProjectUseCase(id) }
                        .onSuccess {
                            if (activeProject?.metadata?.id == id) activeProject = null
                            pendingDeleteProjectId = null
                            updatePlayerSource(activeProject)
                            updateAllStates()
                        }.onFailure(::handleFailure)
                }
        }

        fun updateImportTitle(value: String) {
            importDraft = importDraft.copy(title = value)
            updateImportState()
        }

        fun updateImportArtist(value: String) {
            importDraft = importDraft.copy(artist = value)
            activeProject?.let { replaceActive(it.copy(metadata = it.metadata.copy(artist = value))) }
            updateImportState()
        }

        fun updateImportAlbum(value: String) {
            importDraft = importDraft.copy(album = value)
            activeProject?.let { replaceActive(it.copy(metadata = it.metadata.copy(album = value))) }
            updateImportState()
        }

        fun updateLyricsText(value: String) {
            importDraft = importDraft.copy(lyricsText = value)
            updateImportState()
        }

        fun selectLyricsFormat(format: LyricsFormat) {
            importDraft = importDraft.copy(format = format)
            updateImportState()
        }

        fun importLyricsFromDraft() {
            if (isImporting || importDraft.lyricsText.isBlank()) return
            isImporting = true
            updateImportState()
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    importLyricsUseCase(importDraft.lyricsText, importDraft.format, importDraft.title)
                }.onSuccess { parsed ->
                    val existing = activeProject
                    val merged =
                        if (existing == null) {
                            parsed.copy(
                                metadata =
                                    parsed.metadata.copy(
                                        artist = importDraft.artist,
                                        album = importDraft.album,
                                    ),
                            )
                        } else {
                            pushUndo(existing)
                            parsed.copy(
                                metadata =
                                    existing.metadata.copy(
                                        title = importDraft.title.ifBlank { existing.metadata.title },
                                        artist = importDraft.artist,
                                        album = importDraft.album,
                                        updatedAtMs = System.currentTimeMillis(),
                                    ),
                                agents = existing.agents.ifEmpty { parsed.agents },
                                waveform = existing.waveform,
                            )
                        }
                    activeProject = merged
                    saveProject(merged)
                }.onFailure(::handleFailure)
                isImporting = false
                updateAllStates()
            }
        }

        fun importAudio(uri: Uri) {
            val project = activeProject ?: return
            if (isImporting) return
            isImporting = true
            updateImportState()
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    val audio = importAudioUseCase(uri)
                    pushUndo(project)
                    project.withAudio(audio.uri, audio.displayName, audio.durationMs, audio.waveform)
                }.onSuccess { updated ->
                    activeProject = updated
                    saveProject(updated)
                    updatePlayerSource(updated)
                }.onFailure(::handleFailure)
                isImporting = false
                updateAllStates()
            }
        }

        fun updateLineText(
            lineId: String,
            text: String,
        ) {
            val project = activeProject ?: return
            pushUndo(project)
            val updated =
                project.updateLine(lineId) { line ->
                    val existingWords = line.words
                    val tokens = text.trim().split(Regex("""\s+""")).filter(String::isNotBlank)
                    line.copy(
                        text = text,
                        words =
                            tokens.mapIndexed { index, token ->
                                existingWords.getOrNull(index)?.copy(text = token, syllables = emptyList())
                                    ?: ComposerWord(UUID.randomUUID().toString(), token, null, null, emptyList())
                            },
                    )
                }
            replaceAndSave(updated)
        }

        fun selectWord(
            lineId: String,
            wordId: String,
        ) {
            selection = TimelineSelection(lineId, wordId)
            updateAllStates()
        }

        fun assignLineAgent(
            lineId: String,
            agentId: String,
        ) {
            val project = activeProject ?: return
            pushUndo(project)
            replaceAndSave(project.updateLine(lineId) { it.copy(agentId = agentId) })
        }

        fun toggleLineBackground(lineId: String) {
            val project = activeProject ?: return
            pushUndo(project)
            replaceAndSave(project.updateLine(lineId) { it.copy(isBackground = !it.isBackground) })
        }

        fun addAgent(name: String) {
            val project = activeProject ?: return
            val trimmed = name.trim()
            if (trimmed.isBlank()) return
            pushUndo(project)
            val palette = listOf(0xFF006A6AL, 0xFF6750A4L, 0xFF8E4A00L, 0xFF386A20L, 0xFF7D5260L)
            val agent =
                ComposerAgent(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                    colorArgb = palette[project.agents.size % palette.size],
                )
            replaceAndSave(project.copy(agents = project.agents + agent).touch())
        }

        fun splitSelectedWord() {
            val project = activeProject ?: return
            val lineId = selection.lineId ?: return
            val wordId = selection.wordId ?: return
            pushUndo(project)
            replaceAndSave(project.splitWord(lineId, wordId))
        }

        fun mergeSelectedWordWithNext() {
            val project = activeProject ?: return
            val lineId = selection.lineId ?: return
            val wordId = selection.wordId ?: return
            pushUndo(project)
            replaceAndSave(project.mergeWordWithNext(lineId, wordId))
        }

        fun tapNextWord() {
            val project = activeProject ?: return
            pushUndo(project)
            replaceAndSave(project.tapNextWord(playbackPositionMs))
        }

        fun nudgeSelectedWord(deltaMs: Long) {
            val project = activeProject ?: return
            val lineId = selection.lineId ?: return
            val wordId = selection.wordId ?: return
            val word =
                project.lines
                    .firstOrNull { it.id == lineId }
                    ?.words
                    ?.firstOrNull { it.id == wordId } ?: return
            val start = (word.startMs ?: playbackPositionMs) + deltaMs
            val end = (word.endMs ?: start + 300L) + deltaMs
            pushUndo(project)
            replaceAndSave(updateTimelineWord(project, lineId, wordId, max(0L, start), max(80L, end), settings.snapEnabled))
        }

        fun updateSelectedWordBounds(
            startMs: Long,
            endMs: Long,
        ) {
            val project = activeProject ?: return
            val lineId = selection.lineId ?: return
            val wordId = selection.wordId ?: return
            pushUndo(project)
            replaceAndSave(updateTimelineWord(project, lineId, wordId, startMs, endMs, settings.snapEnabled))
        }

        fun undo() {
            val current = activeProject ?: return
            val previous = undoStack.removeLastOrNull() ?: return
            redoStack.addLast(current)
            replaceAndSave(previous)
        }

        fun redo() {
            val current = activeProject ?: return
            val next = redoStack.removeLastOrNull() ?: return
            undoStack.addLast(current)
            replaceAndSave(next)
        }

        fun playPause() {
            val project = activeProject ?: return
            if (project.metadata.audioUri.isNullOrBlank()) return
            viewModelScope.launch {
                if (player.currentMediaItem == null) updatePlayerSource(project)
                withContext(Dispatchers.Main) {
                    if (player.isPlaying) player.pause() else player.play()
                }
            }
        }

        fun seekTo(positionMs: Long) {
            val duration = activeProject?.metadata?.durationMs ?: 0L
            val safe = positionMs.coerceIn(0L, max(0L, duration))
            playbackPositionMs = safe
            player.seekTo(safe)
            updatePlaybackStates()
        }

        fun shareTtml() {
            val project = activeProject ?: return
            if (isExporting) return
            isExporting = true
            updateExportState()
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { exportTtmlUseCase(project) }
                    .onSuccess { file ->
                        lastExportLabel = file.name
                        _effects.tryEmit(ComposerEffect.ShareFile(file, "application/ttml+xml"))
                    }.onFailure(::handleFailure)
                isExporting = false
                updateExportState()
            }
        }

        fun shareProject() {
            val project = activeProject ?: return
            if (isExporting) return
            isExporting = true
            updateExportState()
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { exportProjectUseCase(project) }
                    .onSuccess { file ->
                        lastExportLabel = file.name
                        _effects.tryEmit(ComposerEffect.ShareFile(file, "application/json"))
                    }.onFailure(::handleFailure)
                isExporting = false
                updateExportState()
            }
        }

        fun setSnapEnabled(enabled: Boolean) {
            viewModelScope.launch(Dispatchers.IO) {
                settingsRepository.setSnapEnabled(enabled)
            }
        }

        fun setUseDynamicColor(enabled: Boolean) {
            viewModelScope.launch(Dispatchers.IO) {
                settingsRepository.setUseDynamicColor(enabled)
            }
        }

        override fun onCleared() {
            player.release()
            super.onCleared()
        }

        private fun observeSavedProjects() {
            viewModelScope.launch(Dispatchers.IO) {
                observeProjectsUseCase().collectLatest { saved ->
                    projects = saved
                    val activeId = activeProject?.metadata?.id
                    if (activeId != null) {
                        activeProject = saved.firstOrNull { it.metadata.id == activeId } ?: activeProject
                    }
                    updateProjectsState()
                    if (activeProject == null) updateAllStates()
                }
            }
        }

        private fun observeSettings() {
            viewModelScope.launch(Dispatchers.IO) {
                settingsRepository.settings.collectLatest { value ->
                    settings =
                        SettingsUiModel(
                            snapEnabled = value.snapEnabled,
                            useDynamicColor = value.useDynamicColor,
                        )
                    updateSettingsState()
                    updateTimelineState()
                }
            }
        }

        private fun observePlayback() {
            player.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        this@ComposerViewModel.isPlaying = isPlaying
                        if (isPlaying) startPlaybackTicker() else playbackJob?.cancel()
                        updatePlaybackStates()
                    }
                },
            )
        }

        private fun startPlaybackTicker() {
            playbackJob?.cancel()
            playbackJob =
                viewModelScope.launch {
                    while (player.isPlaying) {
                        playbackPositionMs = player.currentPosition.coerceAtLeast(0L)
                        updatePlaybackStates()
                        delay(100L)
                    }
                }
        }

        private suspend fun updatePlayerSource(project: ComposerProject?) {
            withContext(Dispatchers.Main) {
                val uri = project?.metadata?.audioUri
                player.stop()
                player.clearMediaItems()
                playbackPositionMs = 0L
                isPlaying = false
                if (!uri.isNullOrBlank()) {
                    player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
                    player.prepare()
                }
            }
        }

        private fun pushUndo(project: ComposerProject) {
            undoStack.addLast(project)
            while (undoStack.size > MaxUndoDepth) undoStack.removeFirst()
            redoStack.clear()
        }

        private fun replaceAndSave(project: ComposerProject) {
            activeProject = project
            saveActive(project)
            updateAllStates()
        }

        private fun replaceActive(project: ComposerProject) {
            activeProject = project.touch()
            saveActive(activeProject ?: return)
            updateAllStates()
        }

        private fun saveActive(project: ComposerProject) {
            writeJob?.cancel()
            writeJob =
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching { saveProject(project) }.onFailure(::handleFailure)
                }
        }

        private suspend fun saveProject(project: ComposerProject) {
            saveProjectUseCase(project)
        }

        private fun updateAllStates() {
            updateProjectsState()
            updateImportState()
            updateEditorState()
            updateSyncState()
            updateTimelineState()
            updatePreviewState()
            updateExportState()
            updateSettingsState()
        }

        private fun updatePlaybackStates() {
            updateSyncState()
            updateTimelineState()
            updatePreviewState()
        }

        private fun updateProjectsState() {
            val summaries = projects.map { it.toSummaryUiModel() }
            _projectsState.value =
                if (summaries.isEmpty()) {
                    ProjectsScreenState.Empty
                } else {
                    ProjectsScreenState.Success(
                        ProjectsUiModel(
                            projects = ProjectSummaryCollection.from(summaries),
                            activeProjectId = activeProject?.metadata?.id,
                            pendingDeleteProjectId = pendingDeleteProjectId,
                        ),
                    )
                }
        }

        private fun updateImportState() {
            _importState.value =
                ImportScreenState.Success(
                    ImportUiModel(
                        activeProjectTitle = activeProject?.metadata?.title,
                        activeAudioName = activeProject?.metadata?.audioName,
                        selectedFormat = importDraft.format,
                        lyricsText = importDraft.lyricsText,
                        titleDraft = importDraft.title,
                        artistDraft = importDraft.artist,
                        albumDraft = importDraft.album,
                        isImporting = isImporting,
                    ),
                )
        }

        private fun updateEditorState() {
            val project = activeProject
            _editorState.value =
                if (project == null) {
                    EditorScreenState.Empty
                } else {
                    EditorScreenState.Success(project.toEditorUiModel())
                }
        }

        private fun updateSyncState() {
            val project = activeProject
            _syncState.value =
                if (project == null) {
                    SyncScreenState.Empty
                } else {
                    SyncScreenState.Success(project.toSyncUiModel())
                }
        }

        private fun updateTimelineState() {
            val project = activeProject
            _timelineState.value =
                if (project == null) {
                    TimelineScreenState.Empty
                } else {
                    TimelineScreenState.Success(project.toTimelineUiModel())
                }
        }

        private fun updatePreviewState() {
            val project = activeProject
            _previewState.value =
                if (project == null) {
                    PreviewScreenState.Empty
                } else {
                    PreviewScreenState.Success(project.toPreviewUiModel())
                }
        }

        private fun updateExportState() {
            val project = activeProject
            _exportState.value =
                if (project == null) {
                    ExportScreenState.Empty
                } else {
                    ExportScreenState.Success(
                        ExportUiModel(
                            projectTitle = project.metadata.title,
                            ttmlText = previewTtml(project),
                            projectJsonAvailable = true,
                            lastExportLabel = lastExportLabel,
                            isExporting = isExporting,
                        ),
                    )
                }
        }

        private fun updateSettingsState() {
            _settingsState.value = SettingsScreenState.Success(settings)
        }

        private fun handleFailure(throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            _effects.tryEmit(ComposerEffect.ShowMessage(R.string.error_unknown))
        }

        private fun ComposerProject.toSummaryUiModel(): ProjectSummaryUiModel =
            ProjectSummaryUiModel(
                id = metadata.id,
                title = metadata.title,
                artist = metadata.artist,
                updatedAtMs = metadata.updatedAtMs,
                lineCount = lines.size,
                wordCount = lines.sumOf { it.words.size },
                durationLabel = metadata.durationMs.formatTime(),
            )

        private fun ComposerProject.toEditorUiModel(): EditorUiModel =
            EditorUiModel(
                projectTitle = metadata.title,
                artist = metadata.artist,
                album = metadata.album,
                lineCount = lines.size,
                wordCount = lines.sumOf { it.words.size },
                agents = AgentUiCollection.from(agents.map { AgentUiModel(it.id, it.name, it.colorArgb) }),
                lines = toLineUiCollection(),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                selection = selection,
            )

        private fun ComposerProject.toSyncUiModel(): SyncUiModel =
            SyncUiModel(
                projectTitle = metadata.title,
                audioName = metadata.audioName,
                playbackPositionMs = playbackPositionMs,
                durationMs = metadata.durationMs,
                isPlaying = isPlaying,
                nextWord = nextUnsyncedWord()?.toWordUiModel(),
                selectedWord = selectedWord()?.toWordUiModel(isSelected = true),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
            )

        private fun ComposerProject.toTimelineUiModel(): TimelineUiModel =
            TimelineUiModel(
                projectTitle = metadata.title,
                durationMs = metadata.durationMs,
                playbackPositionMs = playbackPositionMs,
                waveformPeaks = waveform?.peaks.orEmpty(),
                lines = toLineUiCollection(),
                selectedLineId = selection.lineId,
                selectedWordId = selection.wordId,
                snapEnabled = settings.snapEnabled,
            )

        private fun ComposerProject.toPreviewUiModel(): PreviewUiModel {
            val activeIndex =
                lines.indexOfLast { line ->
                    val start = line.words.mapNotNull { it.startMs }.minOrNull() ?: Long.MIN_VALUE
                    val end = line.words.mapNotNull { it.endMs }.maxOrNull() ?: Long.MIN_VALUE
                    playbackPositionMs in start..end
                }
            return PreviewUiModel(
                projectTitle = metadata.title,
                artist = metadata.artist,
                playbackPositionMs = playbackPositionMs,
                durationMs = metadata.durationMs,
                isPlaying = isPlaying,
                activeLine = lines.getOrNull(activeIndex)?.toLineUiModel(agents),
                upcomingLine = lines.getOrNull(activeIndex + 1)?.toLineUiModel(agents),
            )
        }

        private fun ComposerProject.toLineUiCollection(): LineUiCollection = LineUiCollection.from(lines.map { it.toLineUiModel(agents) })

        private fun ComposerLine.toLineUiModel(agents: List<ComposerAgent>): LineUiModel {
            val agent = agents.firstOrNull { it.id == agentId } ?: ComposerAgent.Default
            return LineUiModel(
                id = id,
                text = text,
                agentId = agentId,
                agentName = agent.name,
                isBackground = isBackground,
                words = WordUiCollection.from(words.map { it.toWordUiModel(isSelected = selection.wordId == it.id) }),
            )
        }

        private fun ComposerWord.toWordUiModel(isSelected: Boolean = false): WordUiModel =
            WordUiModel(
                id = id,
                text = text,
                startMs = startMs,
                endMs = endMs,
                timeLabel =
                    when {
                        startMs != null && endMs != null -> "${startMs.formatTime()} - ${endMs.formatTime()}"
                        else -> "--:--"
                    },
                isSelected = isSelected,
                syllableCount = syllables.size,
            )

        private fun ComposerProject.nextUnsyncedWord(): ComposerWord? =
            lines.asSequence().flatMap { it.words.asSequence() }.firstOrNull { it.startMs == null || it.endMs == null }

        private fun ComposerProject.selectedWord(): ComposerWord? {
            val lineId = selection.lineId ?: return null
            val wordId = selection.wordId ?: return null
            return lines.firstOrNull { it.id == lineId }?.words?.firstOrNull { it.id == wordId }
        }

        private fun Long.formatTime(): String {
            val safe = coerceAtLeast(0L)
            val totalSeconds = safe / 1_000L
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            val millis = safe % 1_000L
            return "%d:%02d.%03d".format(minutes, seconds, millis)
        }

        private data class ImportDraft(
            val title: String = "Untitled",
            val artist: String = "",
            val album: String = "",
            val format: LyricsFormat = LyricsFormat.PLAIN_TEXT,
            val lyricsText: String = "",
        )

        private companion object {
            const val MaxUndoDepth = 80
        }
    }
