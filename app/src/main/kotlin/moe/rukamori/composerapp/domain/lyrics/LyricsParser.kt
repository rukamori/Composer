package moe.rukamori.composerapp.domain.lyrics

import moe.rukamori.composerapp.domain.model.ComposerAgent
import moe.rukamori.composerapp.domain.model.ComposerLine
import moe.rukamori.composerapp.domain.model.ComposerProject
import moe.rukamori.composerapp.domain.model.ComposerWord
import moe.rukamori.composerapp.domain.model.LyricsFormat
import moe.rukamori.composerapp.domain.model.ProjectMetadata
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max

class LyricsParser
    @Inject
    constructor() {
        fun parse(
            text: String,
            format: LyricsFormat,
            title: String,
        ): ComposerProject {
            val trimmed = text.trim()
            val lines =
                when (format) {
                    LyricsFormat.PLAIN_TEXT -> parsePlainText(trimmed)
                    LyricsFormat.LRC -> parseLrc(trimmed)
                    LyricsFormat.SRT -> parseSrt(trimmed)
                    LyricsFormat.TTML -> parseTtml(trimmed)
                }
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
                        durationMs = lines.maxOfOrNull { line -> line.words.maxOfOrNull { it.endMs ?: 0L } ?: 0L } ?: 0L,
                        createdAtMs = now,
                        updatedAtMs = now,
                    ),
                agents = listOf(ComposerAgent.Default),
                lines = lines,
                linkedGroups = emptyList(),
                snapPoints =
                    lines.mapNotNull { line -> line.words.firstOrNull()?.startMs }.distinct().map { time ->
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
            text
                .lineSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .map { lyricLine ->
                    lyricLine.toComposerLine(startMs = null, endMs = null)
                }.toList()

        private fun parseLrc(text: String): List<ComposerLine> {
            val timestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
            val parsed =
                text
                    .lineSequence()
                    .mapNotNull { raw ->
                        val matches = timestampRegex.findAll(raw).toList()
                        if (matches.isEmpty()) return@mapNotNull null
                        val lyric = raw.replace(timestampRegex, "").trim()
                        if (lyric.isBlank()) return@mapNotNull null
                        matches.map { match ->
                            val minutes = match.groupValues[1].toLong()
                            val seconds = match.groupValues[2].toLong()
                            val fraction =
                                match.groupValues[3]
                                    .padEnd(3, '0')
                                    .take(3)
                                    .toLongOrNull() ?: 0L
                            ((minutes * 60L) + seconds) * 1_000L + fraction to lyric
                        }
                    }.flatten()
                    .sortedBy { it.first }
                    .toList()
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
                val lyric =
                    lines
                        .dropWhile { !it.contains("-->") }
                        .drop(1)
                        .joinToString(" ")
                        .trim()
                if (lyric.isBlank()) null else lyric.toComposerLine(startMs = start, endMs = end)
            }
        }

        private fun parseTtml(text: String): List<ComposerLine> {
            val parser =
                XmlPullParserFactory
                    .newInstance()
                    .apply { isNamespaceAware = true }
                    .newPullParser()
                    .apply { setInput(StringReader(text)) }
            val lines = mutableListOf<ComposerLine>()
            var paragraph: TtmlParagraph? = null
            var span: TtmlSpan? = null
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.localName()) {
                            "p" -> {
                                val start = parser.attributeTime("begin")
                                val end = parser.attributeTime("end") ?: parser.attributeTime("dur")?.let { duration -> start?.plus(duration) }
                                paragraph =
                                    TtmlParagraph(
                                        startMs = start,
                                        endMs = end,
                                        agentId = parser.attributeValue("agent") ?: ComposerAgent.Default.id,
                                        isBackground = parser.isBackgroundRole(),
                                    )
                            }

                            "span" -> {
                                paragraph?.let {
                                    span =
                                        TtmlSpan(
                                            startMs = parser.attributeTime("begin") ?: it.startMs,
                                            endMs = parser.attributeTime("end") ?: parser.attributeTime("dur")?.let { duration ->
                                                (parser.attributeTime("begin") ?: it.startMs)?.plus(duration)
                                            } ?: it.endMs,
                                        )
                                }
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val value = parser.text.orEmpty()
                        span?.text?.append(value)
                        if (span == null) paragraph?.text?.append(value)
                    }

                    XmlPullParser.END_TAG -> {
                        when (parser.name.localName()) {
                            "span" -> {
                                val currentSpan = span
                                val currentParagraph = paragraph
                                if (currentSpan != null && currentParagraph != null) {
                                    val spanText = currentSpan.text.toString().trim()
                                    if (spanText.isNotBlank()) {
                                        currentParagraph.words += spanText.toWords(currentSpan.startMs, currentSpan.endMs)
                                        if (currentParagraph.text.isNotEmpty()) currentParagraph.text.append(' ')
                                        currentParagraph.text.append(spanText)
                                    }
                                }
                                span = null
                            }

                            "p" -> {
                                paragraph?.toComposerLine()?.let(lines::add)
                                paragraph = null
                                span = null
                            }
                        }
                    }
                }
                event = parser.next()
            }
            return lines
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

        private fun String.removeBackgroundBrackets(): String = if (isBackgroundLine()) trim().drop(1).dropLast(1).trim() else this

        private fun estimateLineDuration(line: String): Long = max(1_500L, line.split(Regex("""\s+""")).size * 420L)

        private fun parseSrtTime(value: String): Long? {
            val match = Regex("""(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})""").find(value) ?: return null
            val hours = match.groupValues[1].toLong()
            val minutes = match.groupValues[2].toLong()
            val seconds = match.groupValues[3].toLong()
            val millis =
                match.groupValues[4]
                    .padEnd(3, '0')
                    .take(3)
                    .toLong()
            return (((hours * 60L + minutes) * 60L) + seconds) * 1_000L + millis
        }

        private fun XmlPullParser.attributeTime(name: String): Long? = attributeValue(name)?.let(::parseClockTime)

        private fun XmlPullParser.attributeValue(name: String): String? =
            (0 until attributeCount)
                .firstNotNullOfOrNull { index ->
                    val attributeName = getAttributeName(index).localName()
                    getAttributeValue(index).takeIf { attributeName.equals(name, ignoreCase = true) && it.isNotBlank() }
                }

        private fun XmlPullParser.isBackgroundRole(): Boolean {
            val role = attributeValue("role").orEmpty()
            val type = attributeValue("type").orEmpty()
            return role.contains("background", ignoreCase = true) ||
                role.contains("x-bg", ignoreCase = true) ||
                type.contains("background", ignoreCase = true) ||
                type.contains("x-bg", ignoreCase = true)
        }

        private fun TtmlParagraph.toComposerLine(): ComposerLine? {
            val lineText = text.toString().trim()
            if (lineText.isBlank()) return null
            return ComposerLine(
                id = UUID.randomUUID().toString(),
                text = lineText,
                agentId = agentId,
                isBackground = isBackground,
                linkedGroupId = null,
                words = words.ifEmpty { lineText.toWords(startMs, endMs) },
            )
        }

        private fun parseClockTime(value: String): Long? {
            if (value.endsWith("ms")) return value.removeSuffix("ms").toLongOrNull()
            if (value.endsWith("s")) return (value.removeSuffix("s").toDoubleOrNull()?.times(1_000.0))?.toLong()
            val match = Regex("""(\d{1,2}):(\d{2}):(\d{2})(?:[,.](\d{1,3}))?""").find(value)
            if (match == null) {
                val minuteMatch = Regex("""(\d{1,2}):(\d{2})(?:[,.](\d{1,3}))?""").find(value) ?: return null
                val minutes = minuteMatch.groupValues[1].toLong()
                val seconds = minuteMatch.groupValues[2].toLong()
                val millis =
                    minuteMatch.groupValues[3]
                        .padEnd(3, '0')
                        .take(3)
                        .toLongOrNull() ?: 0L
                return ((minutes * 60L) + seconds) * 1_000L + millis
            }
            val hours = match.groupValues[1].toLong()
            val minutes = match.groupValues[2].toLong()
            val seconds = match.groupValues[3].toLong()
            val millis =
                match.groupValues[4]
                    .padEnd(3, '0')
                    .take(3)
                    .toLongOrNull() ?: 0L
            return (((hours * 60L + minutes) * 60L) + seconds) * 1_000L + millis
        }

        private fun String.localName(): String = substringAfter(':')

        private data class TtmlParagraph(
            val startMs: Long?,
            val endMs: Long?,
            val agentId: String,
            val isBackground: Boolean,
            val text: StringBuilder = StringBuilder(),
            val words: MutableList<ComposerWord> = mutableListOf(),
        )

        private data class TtmlSpan(
            val startMs: Long?,
            val endMs: Long?,
            val text: StringBuilder = StringBuilder(),
        )

        private fun formatTime(timeMs: Long): String {
            val seconds = timeMs / 1_000L
            val millis = timeMs % 1_000L
            return "%d:%02d.%03d".format(seconds / 60L, seconds % 60L, millis)
        }
    }
