package moe.rukamori.composerapp.domain.export

import moe.rukamori.composerapp.domain.model.ComposerLine
import moe.rukamori.composerapp.domain.model.ComposerProject
import moe.rukamori.composerapp.domain.model.ComposerWord
import javax.inject.Inject

class TtmlExporter
    @Inject
    constructor() {
        fun export(project: ComposerProject): String {
            val agents =
                project.agents.joinToString("\n") { agent ->
                    """      <ttm:agent xml:id="${agent.id.escapeXml()}" type="person"><ttm:name>${agent.name.escapeXml()}</ttm:name></ttm:agent>"""
                }
            val body = project.lines.joinToString("\n") { line -> line.toTtmlLine() }
            return """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<tt xmlns="http://www.w3.org/ns/ttml"
            |    xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
            |    xmlns:itunes="http://music.apple.com/lyric-ttml-internal"
            |    xmlns:composer="https://composer.boidu.dev/ns"
            |    xml:lang="und">
            |  <head>
            |    <metadata>
            |      <ttm:title>${project.metadata.title.escapeXml()}</ttm:title>
            |$agents
            |    </metadata>
            |  </head>
            |  <body>
            |    <div>
            |$body
            |    </div>
            |  </body>
            |</tt>
                """.trimMargin()
        }

        private fun ComposerLine.toTtmlLine(): String {
            val start = words.mapNotNull(ComposerWord::startMs).minOrNull() ?: 0L
            val end = words.mapNotNull(ComposerWord::endMs).maxOrNull() ?: start + 1_000L
            val role = if (isBackground) """ itunes:role="x-bg"""" else ""
            val group = linkedGroupId?.let { """ composer:group="${it.escapeXml()}"""" }.orEmpty()
            val spans =
                words.joinToString(" ") { word ->
                    val begin = word.startMs ?: start
                    val finish = word.endMs ?: end
                    val text =
                        if (word.syllables.isEmpty()) {
                            word.text.escapeXml()
                        } else {
                            word.syllables.joinToString("") { it.text.escapeXml() }
                        }
                    """<span begin="${begin.toClock()}" end="${finish.toClock()}">$text</span>"""
                }
            return """      <p begin="${start.toClock()}" end="${end.toClock()}" ttm:agent="${agentId.escapeXml()}"$role$group>$spans</p>"""
        }

        private fun Long.toClock(): String {
            val hours = this / 3_600_000L
            val minutes = (this / 60_000L) % 60L
            val seconds = (this / 1_000L) % 60L
            val millis = this % 1_000L
            return "%02d:%02d:%02d.%03d".format(hours, minutes, seconds, millis)
        }

        private fun String.escapeXml(): String =
            replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
    }
