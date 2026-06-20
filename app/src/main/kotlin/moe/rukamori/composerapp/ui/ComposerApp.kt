package moe.rukamori.composerapp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import moe.rukamori.composerapp.R
import moe.rukamori.composerapp.domain.model.LyricsFormat
import moe.rukamori.composerapp.viewmodel.ComposerViewModel
import moe.rukamori.composerapp.viewmodel.EditorScreenState
import moe.rukamori.composerapp.viewmodel.EditorUiModel
import moe.rukamori.composerapp.viewmodel.ExportScreenState
import moe.rukamori.composerapp.viewmodel.ExportUiModel
import moe.rukamori.composerapp.viewmodel.ImportScreenState
import moe.rukamori.composerapp.viewmodel.ImportUiModel
import moe.rukamori.composerapp.viewmodel.LineUiModel
import moe.rukamori.composerapp.viewmodel.PreviewScreenState
import moe.rukamori.composerapp.viewmodel.PreviewUiModel
import moe.rukamori.composerapp.viewmodel.ProjectSummaryUiModel
import moe.rukamori.composerapp.viewmodel.ProjectsScreenState
import moe.rukamori.composerapp.viewmodel.ProjectsUiModel
import moe.rukamori.composerapp.viewmodel.SettingsScreenState
import moe.rukamori.composerapp.viewmodel.SettingsUiModel
import moe.rukamori.composerapp.viewmodel.SyncScreenState
import moe.rukamori.composerapp.viewmodel.SyncUiModel
import moe.rukamori.composerapp.viewmodel.TimelineScreenState
import moe.rukamori.composerapp.viewmodel.TimelineUiModel
import moe.rukamori.composerapp.viewmodel.WordUiModel
import kotlin.math.max

private val LocalOpenNavigationDrawer = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun ComposerApp(
    modifier: Modifier = Modifier,
    viewModel: ComposerViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val destinations = remember { ComposerDestination.entries }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val navigate: (ComposerDestination) -> Unit = { destination ->
            navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        if (useRail) {
            CompositionLocalProvider(LocalOpenNavigationDrawer provides null) {
                PermanentNavigationDrawer(
                    drawerContent = {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            destinations.forEach { destination ->
                                NavigationRailItem(
                                    selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                                    onClick = { navigate(destination) },
                                    icon = { Icon(destination.icon, contentDescription = null) },
                                    label = { Text(stringResource(destination.titleResId)) },
                                )
                            }
                        }
                    },
                ) {
                    ComposerNavHost(viewModel, Modifier.fillMaxSize(), navController)
                }
            }
        } else {
            val drawerState = androidx.compose.material3.rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            CompositionLocalProvider(
                LocalOpenNavigationDrawer provides { scope.launch { drawerState.open() } },
            ) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            destinations.forEach { destination ->
                                NavigationDrawerItem(
                                    selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                                    onClick = {
                                        navigate(destination)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(destination.icon, contentDescription = null) },
                                    label = { Text(stringResource(destination.titleResId)) },
                                    modifier = Modifier.padding(horizontal = MdSpacing.xs),
                                )
                            }
                        }
                    },
                ) {
                    ComposerNavHost(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        navController = navController,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactNavigationIcon() {
    val openDrawer = LocalOpenNavigationDrawer.current
    if (openDrawer != null) {
        IconButton(onClick = openDrawer) {
            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.navigation))
        }
    }
}

@Composable
private fun ComposerNavHost(
    viewModel: ComposerViewModel,
    modifier: Modifier,
    navController: androidx.navigation.NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = ComposerDestination.Projects.route,
        modifier = modifier,
    ) {
        composable(ComposerDestination.Projects.route) {
            val state by viewModel.projectsState.collectAsStateWithLifecycle()
            val untitledProject = stringResource(R.string.untitled_project)
            ProjectsScreen(
                state = state,
                onCreateProject = { viewModel.createProject(untitledProject) },
                onOpenProject = viewModel::openProject,
                onDuplicateProject = viewModel::duplicateActiveProject,
                onRequestDeleteProject = viewModel::requestDeleteProject,
                onDismissDelete = viewModel::dismissDeleteProject,
                onConfirmDelete = viewModel::confirmDeleteProject,
            )
        }
        composable(ComposerDestination.Import.route) {
            val state by viewModel.importState.collectAsStateWithLifecycle()
            ImportScreen(
                state = state,
                onTitleChange = viewModel::updateImportTitle,
                onArtistChange = viewModel::updateImportArtist,
                onAlbumChange = viewModel::updateImportAlbum,
                onLyricsChange = viewModel::updateLyricsText,
                onFormatSelected = viewModel::selectLyricsFormat,
                onImportLyrics = viewModel::importLyricsFromDraft,
                onAudioSelected = viewModel::importAudio,
            )
        }
        composable(ComposerDestination.Edit.route) {
            val state by viewModel.editorState.collectAsStateWithLifecycle()
            val defaultAgentName = stringResource(R.string.default_agent_name)
            EditorScreen(
                state = state,
                onLineTextChange = viewModel::updateLineText,
                onWordSelected = viewModel::selectWord,
                onAgentSelected = viewModel::assignLineAgent,
                onToggleBackground = viewModel::toggleLineBackground,
                onAddAgent = { viewModel.addAgent(defaultAgentName) },
                onSplitWord = viewModel::splitSelectedWord,
                onMergeWord = viewModel::mergeSelectedWordWithNext,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
            )
        }
        composable(ComposerDestination.Sync.route) {
            val state by viewModel.syncState.collectAsStateWithLifecycle()
            SyncScreen(
                state = state,
                onPlayPause = viewModel::playPause,
                onSeek = viewModel::seekTo,
                onTapWord = viewModel::tapNextWord,
                onNudgeBack = { viewModel.nudgeSelectedWord(-100L) },
                onNudgeForward = { viewModel.nudgeSelectedWord(100L) },
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
            )
        }
        composable(ComposerDestination.Timeline.route) {
            val state by viewModel.timelineState.collectAsStateWithLifecycle()
            TimelineScreen(
                state = state,
                onWordSelected = viewModel::selectWord,
                onBoundsChange = viewModel::updateSelectedWordBounds,
            )
        }
        composable(ComposerDestination.Preview.route) {
            val state by viewModel.previewState.collectAsStateWithLifecycle()
            PreviewScreen(
                state = state,
                onPlayPause = viewModel::playPause,
                onSeek = viewModel::seekTo,
            )
        }
        composable(ComposerDestination.Export.route) {
            val state by viewModel.exportState.collectAsStateWithLifecycle()
            ExportScreen(
                state = state,
                onShareTtml = viewModel::shareTtml,
                onShareProject = viewModel::shareProject,
            )
        }
        composable(ComposerDestination.Settings.route) {
            val state by viewModel.settingsState.collectAsStateWithLifecycle()
            SettingsScreen(
                state = state,
                onSnapChanged = viewModel::setSnapEnabled,
                onDynamicColorChanged = viewModel::setUseDynamicColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectsScreen(
    state: ProjectsScreenState,
    onCreateProject: () -> Unit,
    onOpenProject: (String) -> Unit,
    onDuplicateProject: (String) -> Unit,
    onRequestDeleteProject: (String) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    ComposerScaffold(
        titleResId = R.string.projects,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateProject,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_project)) },
            )
        },
    ) { padding ->
        when (state) {
            ProjectsScreenState.Loading -> {
                LoadingState(Modifier.padding(padding))
            }

            ProjectsScreenState.Empty -> {
                EmptyState(
                    titleResId = R.string.empty_projects_title,
                    bodyResId = R.string.empty_projects_body,
                    actionResId = R.string.create_project,
                    onAction = onCreateProject,
                    modifier = Modifier.padding(padding),
                )
            }

            is ProjectsScreenState.Error -> {
                ErrorState(state.messageResId, modifier = Modifier.padding(padding))
            }

            is ProjectsScreenState.Success -> {
                ProjectsContent(
                    model = state.model,
                    onOpenProject = onOpenProject,
                    onDuplicateProject = onDuplicateProject,
                    onRequestDeleteProject = onRequestDeleteProject,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
    if (state is ProjectsScreenState.Success && state.model.pendingDeleteProjectId != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text(stringResource(R.string.delete_project)) },
            text = { Text(stringResource(R.string.delete_project_body)) },
            confirmButton = {
                Button(onClick = onConfirmDelete) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ProjectsContent(
    model: ProjectsUiModel,
    onOpenProject: (String) -> Unit,
    onDuplicateProject: (String) -> Unit,
    onRequestDeleteProject: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        items(
            items = model.projects.asList(),
            key = ProjectSummaryUiModel::id,
            contentType = { "project" },
        ) { project ->
            ProjectCard(
                project = project,
                isActive = project.id == model.activeProjectId,
                onOpen = { onOpenProject(project.id) },
                onDuplicate = { onDuplicateProject(project.id) },
                onDelete = { onRequestDeleteProject(project.id) },
            )
        }
    }
}

@Composable
private fun ProjectCard(
    project: ProjectSummaryUiModel,
    isActive: Boolean,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        ListItem(
            headlineContent = {
                Text(project.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(
                    "${project.lineCount} ${stringResource(
                        R.string.lines,
                    )} · ${project.wordCount} ${stringResource(R.string.words)} · ${project.durationLabel}",
                )
            },
            leadingContent = {
                Icon(Icons.Default.Article, contentDescription = null)
            },
            trailingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                    IconButton(onClick = onOpen) {
                        Icon(Icons.Default.FileOpen, contentDescription = stringResource(R.string.open))
                    }
                    IconButton(onClick = onDuplicate) {
                        Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.duplicate))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            },
        )
    }
}

@Composable
private fun ImportScreen(
    state: ImportScreenState,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onAlbumChange: (String) -> Unit,
    onLyricsChange: (String) -> Unit,
    onFormatSelected: (LyricsFormat) -> Unit,
    onImportLyrics: () -> Unit,
    onAudioSelected: (Uri) -> Unit,
) {
    val audioLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) onAudioSelected(uri)
        }
    ComposerScaffold(titleResId = R.string.import_title) { padding ->
        when (state) {
            ImportScreenState.Loading -> {
                LoadingState(Modifier.padding(padding))
            }

            ImportScreenState.Empty -> {
                EmptyState(R.string.empty_editor_title, R.string.empty_editor_body, modifier = Modifier.padding(padding))
            }

            is ImportScreenState.Error -> {
                ErrorState(state.messageResId, modifier = Modifier.padding(padding))
            }

            is ImportScreenState.Success -> {
                ImportContent(
                    model = state.model,
                    onTitleChange = onTitleChange,
                    onArtistChange = onArtistChange,
                    onAlbumChange = onAlbumChange,
                    onLyricsChange = onLyricsChange,
                    onFormatSelected = onFormatSelected,
                    onImportLyrics = onImportLyrics,
                    onSelectAudio = { audioLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun ImportContent(
    model: ImportUiModel,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onAlbumChange: (String) -> Unit,
    onLyricsChange: (String) -> Unit,
    onFormatSelected: (LyricsFormat) -> Unit,
    onImportLyrics: () -> Unit,
    onSelectAudio: () -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.md),
    ) {
        item(contentType = "metadata") {
            SectionCard(titleResId = R.string.metadata) {
                OutlinedTextField(
                    value = model.titleDraft,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.project_title)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = model.artistDraft,
                    onValueChange = onArtistChange,
                    label = { Text(stringResource(R.string.artist)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = model.albumDraft,
                    onValueChange = onAlbumChange,
                    label = { Text(stringResource(R.string.album)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item(contentType = "audio") {
            SectionCard(titleResId = R.string.audio) {
                ListItem(
                    headlineContent = { Text(model.activeAudioName ?: stringResource(R.string.no_audio)) },
                    leadingContent = { Icon(Icons.Default.AudioFile, contentDescription = null) },
                    trailingContent = {
                        FilledTonalButton(onClick = onSelectAudio) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Text(stringResource(R.string.select_audio))
                        }
                    },
                )
            }
        }
        item(contentType = "lyrics") {
            SectionCard(titleResId = R.string.paste_lyrics) {
                Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                    LyricsFormat.entries.forEach { format ->
                        FilterChip(
                            selected = model.selectedFormat == format,
                            onClick = { onFormatSelected(format) },
                            label = { Text(stringResource(format.titleResId())) },
                            leadingIcon =
                                if (model.selectedFormat == format) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else {
                                    null
                                },
                        )
                    }
                }
                OutlinedTextField(
                    value = model.lyricsText,
                    onValueChange = onLyricsChange,
                    label = { Text(stringResource(R.string.lyrics_text)) },
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = !model.isImporting && model.lyricsText.isNotBlank(),
                    onClick = onImportLyrics,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Text(stringResource(R.string.import_lyrics))
                }
            }
        }
    }
}

@Composable
private fun EditorScreen(
    state: EditorScreenState,
    onLineTextChange: (String, String) -> Unit,
    onWordSelected: (String, String) -> Unit,
    onAgentSelected: (String, String) -> Unit,
    onToggleBackground: (String) -> Unit,
    onAddAgent: () -> Unit,
    onSplitWord: () -> Unit,
    onMergeWord: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    ComposerScaffold(
        titleResId = R.string.edit,
        actions = {
            IconButton(onClick = onUndo, enabled = state is EditorScreenState.Success && state.model.canUndo) {
                Icon(Icons.Default.Replay, contentDescription = stringResource(R.string.undo))
            }
            IconButton(onClick = onRedo, enabled = state is EditorScreenState.Success && state.model.canRedo) {
                Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.redo))
            }
        },
    ) { padding ->
        when (state) {
            EditorScreenState.Loading -> {
                LoadingState(Modifier.padding(padding))
            }

            EditorScreenState.Empty -> {
                EmptyState(R.string.empty_editor_title, R.string.empty_editor_body, modifier = Modifier.padding(padding))
            }

            is EditorScreenState.Error -> {
                ErrorState(state.messageResId, modifier = Modifier.padding(padding))
            }

            is EditorScreenState.Success -> {
                EditorContent(
                    model = state.model,
                    onLineTextChange = onLineTextChange,
                    onWordSelected = onWordSelected,
                    onAgentSelected = onAgentSelected,
                    onToggleBackground = onToggleBackground,
                    onAddAgent = onAddAgent,
                    onSplitWord = onSplitWord,
                    onMergeWord = onMergeWord,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun EditorContent(
    model: EditorUiModel,
    onLineTextChange: (String, String) -> Unit,
    onWordSelected: (String, String) -> Unit,
    onAgentSelected: (String, String) -> Unit,
    onToggleBackground: (String) -> Unit,
    onAddAgent: () -> Unit,
    onSplitWord: () -> Unit,
    onMergeWord: () -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item(contentType = "summary") {
            SummaryCard(model.projectTitle, model.artist, model.lineCount, model.wordCount)
        }
        item(contentType = "agents") {
            SectionCard(titleResId = R.string.agents) {
                Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                    model.agents.asList().forEach { agent ->
                        AssistChip(
                            onClick = {},
                            label = { Text(agent.name) },
                        )
                    }
                    FilledTonalButton(onClick = onAddAgent) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(stringResource(R.string.add_agent))
                    }
                }
            }
        }
        items(
            items = model.lines.asList(),
            key = LineUiModel::id,
            contentType = { "line" },
        ) { line ->
            LyricLineEditor(
                line = line,
                agents = model.agents.asList(),
                onLineTextChange = { onLineTextChange(line.id, it) },
                onWordSelected = { onWordSelected(line.id, it) },
                onAgentSelected = { onAgentSelected(line.id, it) },
                onToggleBackground = { onToggleBackground(line.id) },
                onSplitWord = onSplitWord,
                onMergeWord = onMergeWord,
            )
        }
    }
}

@Composable
private fun LyricLineEditor(
    line: LineUiModel,
    agents: List<moe.rukamori.composerapp.viewmodel.AgentUiModel>,
    onLineTextChange: (String) -> Unit,
    onWordSelected: (String) -> Unit,
    onAgentSelected: (String) -> Unit,
    onToggleBackground: () -> Unit,
    onSplitWord: () -> Unit,
    onMergeWord: () -> Unit,
) {
    OutlinedCard {
        Column(
            modifier = Modifier.padding(MdSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            OutlinedTextField(
                value = line.text,
                onValueChange = onLineTextChange,
                label = { Text(stringResource(R.string.lyrics_text)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                agents.forEach { agent ->
                    FilterChip(
                        selected = line.agentId == agent.id,
                        onClick = { onAgentSelected(agent.id) },
                        label = { Text(agent.name) },
                    )
                }
                FilterChip(
                    selected = line.isBackground,
                    onClick = onToggleBackground,
                    label = { Text(stringResource(R.string.background_vocal)) },
                )
            }
            FlowWords(line.words.asList(), onWordSelected)
            Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                FilledTonalButton(onClick = onSplitWord) {
                    Text(stringResource(R.string.split_word))
                }
                OutlinedButton(onClick = onMergeWord) {
                    Text(stringResource(R.string.merge_next))
                }
            }
        }
    }
}

@Composable
private fun FlowWords(
    words: List<WordUiModel>,
    onWordSelected: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs),
        modifier = Modifier.fillMaxWidth(),
    ) {
        words.take(6).forEach { word ->
            FilterChip(
                selected = word.isSelected,
                onClick = { onWordSelected(word.id) },
                label = { Text(word.text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
private fun SyncScreen(
    state: SyncScreenState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onTapWord: () -> Unit,
    onNudgeBack: () -> Unit,
    onNudgeForward: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    ComposerScaffold(titleResId = R.string.sync) { padding ->
        when (state) {
            SyncScreenState.Loading -> {
                LoadingState(Modifier.padding(padding))
            }

            SyncScreenState.Empty -> {
                EmptyState(R.string.empty_editor_title, R.string.empty_editor_body, modifier = Modifier.padding(padding))
            }

            is SyncScreenState.Error -> {
                ErrorState(state.messageResId, modifier = Modifier.padding(padding))
            }

            is SyncScreenState.Success -> {
                SyncContent(
                    model = state.model,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onTapWord = onTapWord,
                    onNudgeBack = onNudgeBack,
                    onNudgeForward = onNudgeForward,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun SyncContent(
    model: SyncUiModel,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onTapWord: () -> Unit,
    onNudgeBack: () -> Unit,
    onNudgeForward: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.md),
    ) {
        MediaControls(model.isPlaying, model.playbackPositionMs, model.durationMs, onPlayPause, onSeek)
        SectionCard(titleResId = R.string.tap_word) {
            Text(
                text = model.nextWord?.text ?: stringResource(R.string.copy_ready),
                style = MaterialTheme.typography.headlineMedium,
            )
            Button(onClick = onTapWord, enabled = model.nextWord != null) {
                Icon(Icons.Default.Timer, contentDescription = null)
                Text(stringResource(R.string.tap_word))
            }
        }
        SectionCard(titleResId = R.string.selected_word) {
            Text(model.selectedWord?.text ?: stringResource(R.string.empty_editor_title))
            Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                FilledTonalButton(onClick = onNudgeBack, enabled = model.selectedWord != null) {
                    Text(stringResource(R.string.nudge_back))
                }
                FilledTonalButton(onClick = onNudgeForward, enabled = model.selectedWord != null) {
                    Text(stringResource(R.string.nudge_forward))
                }
                OutlinedButton(onClick = onUndo, enabled = model.canUndo) {
                    Text(stringResource(R.string.undo))
                }
                OutlinedButton(onClick = onRedo, enabled = model.canRedo) {
                    Text(stringResource(R.string.redo))
                }
            }
        }
    }
}

@Composable
private fun TimelineScreen(
    state: TimelineScreenState,
    onWordSelected: (String, String) -> Unit,
    onBoundsChange: (Long, Long) -> Unit,
) {
    ComposerScaffold(titleResId = R.string.timeline) { padding ->
        when (state) {
            TimelineScreenState.Loading -> {
                LoadingState(Modifier.padding(padding))
            }

            TimelineScreenState.Empty -> {
                EmptyState(R.string.empty_editor_title, R.string.empty_editor_body, modifier = Modifier.padding(padding))
            }

            is TimelineScreenState.Error -> {
                ErrorState(state.messageResId, modifier = Modifier.padding(padding))
            }

            is TimelineScreenState.Success -> {
                TimelineContent(
                    model = state.model,
                    onWordSelected = onWordSelected,
                    onBoundsChange = onBoundsChange,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun TimelineContent(
    model: TimelineUiModel,
    onWordSelected: (String, String) -> Unit,
    onBoundsChange: (Long, Long) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item(contentType = "waveform") {
            WaveformCard(model.waveformPeaks, model.playbackPositionMs, model.durationMs)
        }
        items(
            items = model.lines.asList(),
            key = LineUiModel::id,
            contentType = { "timeline-line" },
        ) { line ->
            TimelineLine(line, onWordSelected)
        }
        item(contentType = "bounds") {
            val selected =
                model.lines
                    .asList()
                    .firstOrNull { it.id == model.selectedLineId }
                    ?.words
                    ?.asList()
                    ?.firstOrNull { it.id == model.selectedWordId }
            if (selected != null) {
                SectionCard(titleResId = R.string.selected_word) {
                    Text(selected.text, style = MaterialTheme.typography.titleMedium)
                    TimeBoundSlider(R.string.start_time, selected.startMs ?: 0L, model.durationMs) { start ->
                        onBoundsChange(start, selected.endMs ?: (start + 300L))
                    }
                    TimeBoundSlider(R.string.end_time, selected.endMs ?: 0L, model.durationMs) { end ->
                        onBoundsChange(selected.startMs ?: 0L, end)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineLine(
    line: LineUiModel,
    onWordSelected: (String, String) -> Unit,
) {
    OutlinedCard {
        Column(
            modifier = Modifier.padding(MdSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
        ) {
            Text(line.text, style = MaterialTheme.typography.titleMedium)
            line.words.asList().forEach { word ->
                ListItem(
                    headlineContent = { Text(word.text) },
                    supportingContent = { Text(word.timeLabel) },
                    leadingContent = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
                    trailingContent = {
                        FilterChip(
                            selected = word.isSelected,
                            onClick = { onWordSelected(line.id, word.id) },
                            label = { Text(stringResource(R.string.select_audio)) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun PreviewScreen(
    state: PreviewScreenState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    ComposerScaffold(titleResId = R.string.preview) { padding ->
        when (state) {
            PreviewScreenState.Loading -> {
                LoadingState(Modifier.padding(padding))
            }

            PreviewScreenState.Empty -> {
                EmptyState(R.string.empty_editor_title, R.string.empty_editor_body, modifier = Modifier.padding(padding))
            }

            is PreviewScreenState.Error -> {
                ErrorState(state.messageResId, modifier = Modifier.padding(padding))
            }

            is PreviewScreenState.Success -> {
                PreviewContent(
                    model = state.model,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun PreviewContent(
    model: PreviewUiModel,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MediaControls(model.isPlaying, model.playbackPositionMs, model.durationMs, onPlayPause, onSeek)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.widthIn(max = 720.dp),
        ) {
            Column(
                modifier = Modifier.padding(MdSpacing.md),
                verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(model.projectTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    text = model.activeLine?.text ?: stringResource(R.string.loading),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                model.upcomingLine?.let {
                    Text(it.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun ExportScreen(
    state: ExportScreenState,
    onShareTtml: () -> Unit,
    onShareProject: () -> Unit,
) {
    ComposerScaffold(titleResId = R.string.export) { padding ->
        when (state) {
            ExportScreenState.Loading -> {
                LoadingState(Modifier.padding(padding))
            }

            ExportScreenState.Empty -> {
                EmptyState(R.string.empty_editor_title, R.string.empty_editor_body, modifier = Modifier.padding(padding))
            }

            is ExportScreenState.Error -> {
                ErrorState(state.messageResId, modifier = Modifier.padding(padding))
            }

            is ExportScreenState.Success -> {
                ExportContent(
                    model = state.model,
                    onShareTtml = onShareTtml,
                    onShareProject = onShareProject,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun ExportContent(
    model: ExportUiModel,
    onShareTtml: () -> Unit,
    onShareProject: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
            Button(onClick = onShareTtml, enabled = !model.isExporting) {
                Icon(Icons.Default.IosShare, contentDescription = null)
                Text(stringResource(R.string.share_ttml))
            }
            FilledTonalButton(onClick = onShareProject, enabled = !model.isExporting) {
                Icon(Icons.Default.Save, contentDescription = null)
                Text(stringResource(R.string.share_project))
            }
        }
        model.lastExportLabel?.let {
            AssistChip(onClick = {}, label = { Text(it) })
        }
        OutlinedTextField(
            value = model.ttmlText,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.ttml)) },
            minLines = 16,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun SettingsScreen(
    state: SettingsScreenState,
    onSnapChanged: (Boolean) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
) {
    ComposerScaffold(titleResId = R.string.settings) { padding ->
        when (state) {
            SettingsScreenState.Loading -> {
                LoadingState(Modifier.padding(padding))
            }

            SettingsScreenState.Empty -> {
                EmptyState(R.string.empty_editor_title, R.string.empty_editor_body, modifier = Modifier.padding(padding))
            }

            is SettingsScreenState.Error -> {
                ErrorState(state.messageResId, modifier = Modifier.padding(padding))
            }

            is SettingsScreenState.Success -> {
                SettingsContent(
                    model = state.model,
                    onSnapChanged = onSnapChanged,
                    onDynamicColorChanged = onDynamicColorChanged,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    model: SettingsUiModel,
    onSnapChanged: (Boolean) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item(contentType = "settings") {
            SectionCard(titleResId = R.string.settings) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.snap_enabled)) },
                    leadingContent = { Icon(Icons.Default.Waves, contentDescription = null) },
                    trailingContent = { Switch(checked = model.snapEnabled, onCheckedChange = onSnapChanged) },
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.theme_system)) },
                    leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                    trailingContent = { Switch(checked = model.useDynamicColor, onCheckedChange = onDynamicColorChanged) },
                )
            }
        }
        item(contentType = "hooks") {
            SectionCard(titleResId = R.string.help) {
                Text(stringResource(R.string.youtube_hook_unavailable))
                Text(stringResource(R.string.vocal_separation_unavailable))
            }
        }
        item(contentType = "standards") {
            SectionCard(titleResId = R.string.standards) {
                Text(stringResource(R.string.standards_body))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposerScaffold(
    titleResId: Int,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleResId), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { CompactNavigationIcon() },
                actions = { actions() },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            )
        },
        floatingActionButton = floatingActionButton,
        contentWindowInsets = WindowInsets.safeDrawing,
        content = content,
    )
}

@Composable
private fun SectionCard(
    titleResId: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MdSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            Text(stringResource(titleResId), style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    artist: String,
    lines: Int,
    words: Int,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(
            modifier = Modifier.padding(MdSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
            if (artist.isNotBlank()) {
                Text(artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(
                "$lines ${stringResource(R.string.lines)} · $words ${stringResource(R.string.words)}",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun MediaControls(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    SectionCard(titleResId = R.string.audio) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                )
            }
            Text(positionMs.formatTime())
        }
        Slider(
            value = positionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..max(1L, durationMs).toFloat(),
        )
    }
}

@Composable
private fun TimeBoundSlider(
    labelResId: Int,
    value: Long,
    durationMs: Long,
    onChange: (Long) -> Unit,
) {
    Column {
        Text("${stringResource(labelResId)} ${value.formatTime()}", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = value.coerceAtLeast(0L).toFloat(),
            onValueChange = { onChange(it.toLong()) },
            valueRange = 0f..max(1L, durationMs).toFloat(),
        )
    }
}

@Composable
private fun WaveformCard(
    peaks: List<Float>,
    positionMs: Long,
    durationMs: Long,
) {
    SectionCard(titleResId = R.string.timeline) {
        val color = MaterialTheme.colorScheme.primary
        val inactive = MaterialTheme.colorScheme.outline
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(96.dp),
        ) {
            val widthStep = size.width / max(1, peaks.size)
            peaks.forEachIndexed { index, peak ->
                val x = index * widthStep
                val half = size.height * peak / 2f
                drawLine(
                    color = inactive,
                    start = Offset(x, size.height / 2f - half),
                    end = Offset(x, size.height / 2f + half),
                    strokeWidth = max(2f, widthStep * 0.55f),
                )
            }
            if (durationMs > 0L) {
                val playheadX = size.width * (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                drawLine(color = color, start = Offset(playheadX, 0f), end = Offset(playheadX, size.height), strokeWidth = 4f)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator()
    }
}

@Composable
private fun EmptyState(
    titleResId: Int,
    bodyResId: Int,
    modifier: Modifier = Modifier,
    actionResId: Int? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(MdSpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.LibraryBooks, contentDescription = null)
        Text(stringResource(titleResId), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(bodyResId), style = MaterialTheme.typography.bodyMedium)
        if (actionResId != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = MdSpacing.sm)) {
                Text(stringResource(actionResId))
            }
        }
    }
}

@Composable
private fun ErrorState(
    messageResId: Int,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        titleResId = messageResId,
        bodyResId = R.string.error_unknown,
        modifier = modifier,
    )
}

private enum class ComposerDestination(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector,
) {
    Projects("projects", R.string.projects, Icons.Default.LibraryBooks),
    Import("import", R.string.import_title, Icons.Default.UploadFile),
    Edit("edit", R.string.edit, Icons.Default.Edit),
    Sync("sync", R.string.sync, Icons.Default.Timer),
    Timeline("timeline", R.string.timeline, Icons.Default.Waves),
    Preview("preview", R.string.preview, Icons.Default.PlayArrow),
    Export("export", R.string.export, Icons.Default.IosShare),
    Settings("settings", R.string.settings, Icons.Default.Settings),
}

private fun LyricsFormat.titleResId(): Int =
    when (this) {
        LyricsFormat.PLAIN_TEXT -> R.string.plain_text
        LyricsFormat.LRC -> R.string.lrc
        LyricsFormat.SRT -> R.string.srt
        LyricsFormat.TTML -> R.string.ttml
    }

private fun Long.formatTime(): String {
    val safe = coerceAtLeast(0L)
    val totalSeconds = safe / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val millis = safe % 1_000L
    return "%d:%02d.%03d".format(minutes, seconds, millis)
}

private object MdSpacing {
    val xs = 8.dp
    val sm = 16.dp
    val md = 24.dp
    val lg = 32.dp
}
