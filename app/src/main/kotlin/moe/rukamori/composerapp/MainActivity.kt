package moe.rukamori.composerapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import moe.rukamori.composerapp.ui.ComposerApp
import moe.rukamori.composerapp.ui.theme.ComposerTheme
import moe.rukamori.composerapp.viewmodel.ComposerEffect
import moe.rukamori.composerapp.viewmodel.ComposerViewModel
import moe.rukamori.composerapp.viewmodel.SettingsScreenState

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: ComposerViewModel = hiltViewModel()
            val settingsState = viewModel.settingsState.collectAsStateWithLifecycle().value
            val useDynamicColor = (settingsState as? SettingsScreenState.Success)?.model?.useDynamicColor ?: true

            androidx.compose.runtime.LaunchedEffect(viewModel) {
                viewModel.effects.collectLatest { effect ->
                    when (effect) {
                        is ComposerEffect.ShareFile -> shareFile(effect)
                        is ComposerEffect.ShowMessage -> Unit
                    }
                }
            }

            ComposerTheme(useDynamicColor = useDynamicColor) {
                ComposerApp(viewModel = viewModel)
            }
        }
    }

    private fun shareFile(effect: ComposerEffect.ShareFile) {
        val uri = FileProvider.getUriForFile(
            this,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            effect.file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = effect.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.export)))
    }
}
