package moe.rukamori.composerapp.domain.lyrics

import moe.rukamori.composerapp.domain.model.ComposerAgent
import moe.rukamori.composerapp.domain.model.ComposerLine
import moe.rukamori.composerapp.domain.model.ComposerProject
import moe.rukamori.composerapp.domain.model.ComposerWord
import moe.rukamori.composerapp.domain.model.LyricsFormat
import moe.rukamori.composerapp.domain.model.ProjectMetadata
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max

class LyricsParser @Inject constructor() {
    fun parse(
        text: String,
        format: LyricsFormat,
        title: String,
    ): ComposerProject {
        val trimmed = text.trim()
        val lines = when (format) {
            LyricsFormat.PLAIN_TEXT -> parsePlainText(trimmed)
            LyricsFormat.LRC -> parseLrc(trimmed)
            LyricsFormat.SRT -> parseSrt(trimmed)
            LyricsFormat.TTML -> parseTtml(trimmed)
        }
        val now = System.currentTimeMillis()
        return ComposerProject(
            metadata = ProjectMetadata(
                id = UUID.randomUUID().toString(),
                title = title.trim().ifBlank { "Untitled" },
                artist = "",
                album = "",
                audioUri = null,
                audioName = null,
                durationMs = lines.maxOfOrNull { line -> line.words.maxOfOrNull { it.endMs ?: 0L } ?: 0L } ?: 0L,
                createdAtMs = now,
                updatedAtMs = now,
            ),
            agents = listOf(ComposerAgent.Default),
            lines = lines,
            linkedGroups = emptyList(),
            snapPoints = lines.mapNotNull { line -> line.words.firstOrNull()?.startMs }.distinct().map { time ->
                moe.rukamori.composerapp.domain.model.SnapPoint(
                    id = UUID.randomUUID().toString(),
                    timeMs = time,
                    label = formatTime(time),
                )
            },
            waveform = null,
        )
    }

    private fun parsePlainText(text: String): List<ComposerLine> =
        text.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .map { lyricLine ->
                lyricLine.toComposerLine(startMs = null, endMs = null)
            }
            .toList()

    private fun parseLrc(text: String): List<ComposerLine> {
        val timestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
        val parsed = text.lineSequence().mapNotNull { raw ->
            val matches = timestampRegex.findAll(raw).toList()
            if (matches.isEmpty()) return@mapNotNull null
            val lyric = raw.replace(timestampRegex, "").trim()
            if (lyric.isBlank()) return@mapNotNull null
            matches.map { match ->
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                ((minutes * 60L) + seconds) * 1_000L + fraction to lyric
            }
        }.flatten().sortedBy { it.first }.toList()
        return parsed.mapIndexed { index, pair ->
            val nextStart = parsed.getOrNull(index + 1)?.first
            val end = nextStart ?: pair.first + estimateLineDuration(pair.second)
            pair.second.toComposerLine(startMs = pair.first, endMs = end)
        }
    }

    private fun parseSrt(text: String): List<ComposerLine> {
        val blocks = text.split(Regex("""\r?\n\r?\n"""))
        return blocks.mapNotNull { block ->
            val lines = block.lines().map(String::trim).filter(String::isNotBlank)
            val timingLine = lines.firstOrNull { it.contains("-->") } ?: return@mapNotNull null
            val parts = timingLine.split("-->").map(String::trim)
            if (parts.size != 2) return@mapNotNull null
            val start = parseSrtTime(parts[0]) ?: return@mapNotNull null
            val end = parseSrtTime(parts[1]) ?: return@mapNotNull null
            val lyric = lines.dropWhile { !it.contains("-->") }.drop(1).joinToString(" ").trim()
            if (lyric.isBlank()) null else lyric.toComposerLine(startMs = start, endMs = end)
        }
    }

    private fun parseTtml(text: String): List<ComposerLine> {
        val paragraphRegex = Regex("""<p\b([^>]*)>(.*?)</p>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val spanRegex = Regex("""<span\b([^>]*)>(.*?)</span>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        return paragraphRegex.findAll(text).mapNotNull { paragraph ->
            val attributes = paragraph.groupValues[1]
            val body = paragraph.groupValues[2]
            val start = attributeTime(attributes, "begin")
            val end = attributeTime(attributes, "end")
            val words = spanRegex.findAll(body).map { span ->
                val spanAttributes = span.groupValues[1]
                val wordText = span.groupValues[2].stripTags().decodeXml().trim()
                ComposerWord(
                    id = UUID.randomUUID().toString(),
                    text = wordText,
                    startMs = attributeTime(spanAttributes, "begin") ?: start,
                    endMs = attributeTime(spanAttributes, "end") ?: end,
                    syllables = emptyList(),
                )
            }.filter { it.text.isNotBlank() }.toList()
            val lineText = if (words.isNotEmpty()) words.joinToString(" ") { it.text } else body.stripTags().decodeXml().trim()
            if (lineText.isBlank()) {
                null
            } else {
                ComposerLine(
                    id = UUID.randomUUID().toString(),
                    text = lineText,
                    agentId = ComposerAgent.Default.id,
                    isBackground = attributes.contains("x-bg", ignoreCase = true),
                    linkedGroupId = null,
                    words = words.ifEmpty { lineText.toWords(start, end) },
                )
            }
        }.toList()
    }

    private fun String.toComposerLine(
        startMs: Long?,
        endMs: Long?,
    ): ComposerLine =
        ComposerLine(
            id = UUID.randomUUID().toString(),
            text = this,
            agentId = ComposerAgent.Default.id,
            isBackground = isBackgroundLine(),
            linkedGroupId = null,
            words = removeBackgroundBrackets().toWords(startMs, endMs),
        )

    private fun String.toWords(
        startMs: Long?,
        endMs: Long?,
    ): List<ComposerWord> {
        val tokens = split(Regex("""\s+""")).filter(String::isNotBlank)
        if (tokens.isEmpty()) return emptyList()
        val duration = if (startMs != null && endMs != null) max(1L, endMs - startMs) else 0L
        return tokens.mapIndexed { index, token ->
            val wordStart = if (startMs != null && duration > 0L) startMs + (duration * index / tokens.size) else null
            val wordEnd = if (startMs != null && duration > 0L) startMs + (duration * (index + 1) / tokens.size) else null
            ComposerWord(
                id = UUID.randomUUID().toString(),
                text = token,
                startMs = wordStart,
                endMs = wordEnd,
                syllables = emptyList(),
            )
        }
    }

    private fun String.isBackgroundLine(): Boolean {
        val value = trim()
        return (value.startsWith("(") && value.endsWith(")")) || (value.startsWith("[") && value.endsWith("]"))
    }

    private fun String.removeBackgroundBrackets(): String =
        if (isBackgroundLine()) trim().drop(1).dropLast(1).trim() else this

    private fun estimateLineDuration(line: String): Long = max(1_500L, line.split(Regex("""\s+""")).size * 420L)

    private fun parseSrtTime(value: String): Long? {
        val match = Regex("""(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})""").find(value) ?: return null
        val hours = match.groupValues[1].toLong()
        val minutes = match.groupValues[2].toLong()
        val seconds = match.groupValues[3].toLong()
        val millis = match.groupValues[4].padEnd(3, '0').take(3).toLong()
        return (((hours * 60L + minutes) * 60L) + seconds) * 1_000L + millis
    }

    private fun attributeTime(
        attributes: String,
        name: String,
    ): Long? {
        val value = Regex("""\b$name\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attributes)?.groupValues?.get(1) ?: return null
        return parseClockTime(value)
    }

    private fun parseClockTime(value: String): Long? {
        if (value.endsWith("ms")) return value.removeSuffix("ms").toLongOrNull()
        if (value.endsWith("s")) return (value.removeSuffix("s").toDoubleOrNull()?.times(1_000.0))?.toLong()
        val match = Regex("""(\d{1,2}):(\d{2}):(\d{2})(?:[,.](\d{1,3}))?""").find(value) ?: return null
        val hours = match.groupValues[1].toLong()
        val minutes = match.groupValues[2].toLong()
        val seconds = match.groupValues[3].toLong()
        val millis = match.groupValues[4].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return (((hours * 60L + minutes) * 60L) + seconds) * 1_000L + millis
    }

    private fun String.stripTags(): String = replace(Regex("""<[^>]+>"""), " ")

    private fun String.decodeXml(): String =
        replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")

    private fun formatTime(timeMs: Long): String {
        val seconds = timeMs / 1_000L
        val millis = timeMs % 1_000L
        return "%d:%02d.%03d".format(seconds / 60L, seconds % 60L, millis)
    }
}
