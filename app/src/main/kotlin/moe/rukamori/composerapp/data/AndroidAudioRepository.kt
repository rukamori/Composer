package moe.rukamori.composerapp.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import moe.rukamori.composerapp.domain.model.AudioMetadata
import moe.rukamori.composerapp.domain.model.WaveformSummary
import moe.rukamori.composerapp.domain.repository.AudioRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.sin

@Singleton
class AndroidAudioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioRepository {
    override suspend fun inspectAudio(uri: Uri): AudioMetadata {
        context.contentResolver.takePersistableUriPermissionSafely(uri)
        val duration = readDuration(uri)
        return AudioMetadata(
            uri = uri.toString(),
            displayName = readDisplayName(uri),
            durationMs = duration,
            waveform = buildWaveform(duration),
        )
    }

    private fun readDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }

    private fun readDisplayName(uri: Uri): String {
        val fromResolver = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
        return fromResolver?.takeIf(String::isNotBlank)
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
            ?: "Audio"
    }

    private fun buildWaveform(durationMs: Long): WaveformSummary {
        val count = 160
        val seed = durationMs.coerceAtLeast(1L).toDouble()
        val peaks = List(count) { index ->
            val primary = sin((index + 1) * PI / 9.0 + seed / 100_000.0).absoluteValue
            val secondary = sin((index + 3) * PI / 17.0).absoluteValue * 0.35
            (0.15 + primary * 0.65 + secondary).coerceIn(0.08, 1.0).toFloat()
        }
        return WaveformSummary(durationMs = durationMs, peaks = peaks)
    }

    private fun android.content.ContentResolver.takePersistableUriPermissionSafely(uri: Uri) {
        runCatching {
            takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}
