package moe.rukamori.composerapp.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import moe.rukamori.composerapp.domain.export.TtmlExporter
import moe.rukamori.composerapp.domain.model.ComposerProject
import moe.rukamori.composerapp.domain.repository.ExportRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidExportRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val ttmlExporter: TtmlExporter,
        private val codec: ProjectJsonCodec,
    ) : ExportRepository {
        override suspend fun writeTtml(project: ComposerProject): File {
            val file = exportFile(project.metadata.title, "ttml")
            file.writeText(ttmlExporter.export(project), Charsets.UTF_8)
            return file
        }

        override suspend fun writeProject(project: ComposerProject): File {
            val file = exportFile(project.metadata.title, "composer.json")
            file.writeText(codec.encode(project), Charsets.UTF_8)
            return file
        }

        private fun exportFile(
            title: String,
            extension: String,
        ): File {
            val directory = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeTitle = title.ifBlank { "composer-project" }.replace(Regex("""[^\w.-]+"""), "_")
            return File(directory, "$safeTitle.$extension")
        }
    }
