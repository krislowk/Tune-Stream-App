package com.vynce.music.lyrics

import java.util.regex.Pattern

object LyricsParser {
    private val linePattern = Pattern.compile("\\[(\\d+):(\\d+)(?:[.,](\\d+))?\\](.*)")
    private val wordPattern = Pattern.compile("<(\\d+):(\\d+)(?:[.,](\\d+))?>(.[^<]*)")

    fun parse(lrcText: String): List<LyricsEntry> {
        val entries = mutableListOf<LyricsEntry>()
        val lines = lrcText.lines()

        for (line in lines) {
            val lineMatcher = linePattern.matcher(line)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0
            
            while (lineMatcher.find()) {
                val min = lineMatcher.group(1)?.toLong() ?: 0L
                val sec = lineMatcher.group(2)?.toLong() ?: 0L
                val msPart = lineMatcher.group(3)
                val ms = if (msPart != null) {
                    msPart.padEnd(3, '0').substring(0, 3).toLong()
                } else 0L
                
                timestamps.add((min * 60 + sec) * 1000 + ms)
                lastMatchEnd = lineMatcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val content = line.substring(lastMatchEnd).trim()
                if (content.isEmpty()) continue

                if (content.contains("<")) {
                    // Enhanced word-level LRC
                    val words = mutableListOf<WordTimestamp>()
                    val wordMatcher = wordPattern.matcher(content)
                    
                    val startTime = timestamps.first().toDouble() / 1000.0
                    var lastTime = startTime
                    var fullText = ""

                    while (wordMatcher.find()) {
                        val wMin = wordMatcher.group(1)?.toLong() ?: 0L
                        val wSec = wordMatcher.group(2)?.toLong() ?: 0L
                        val wMsPart = wordMatcher.group(3)
                        val wMs = wMsPart?.padEnd(3, '0')?.substring(0, 3)?.toLong() ?: 0L
                        
                        val wTime = (wMin * 60 + wSec).toDouble() + (wMs.toDouble() / 1000.0)
                        val wordText = wordMatcher.group(4) ?: ""
                        
                        words.add(
                            WordTimestamp(
                                text = wordText.trim(),
                                startTime = lastTime,
                                endTime = wTime,
                                hasTrailingSpace = wordText.endsWith(" ")
                            )
                        )
                        fullText += wordText
                        lastTime = wTime
                    }

                    if (words.isEmpty()) {
                        for (time in timestamps) {
                            entries.add(LyricsEntry(time = time, text = content.replace(Regex("<[^>]*>"), "").trim()))
                        }
                    } else {
                        for (time in timestamps) {
                            entries.add(LyricsEntry(time = time, text = fullText.trim(), words = words))
                        }
                    }
                } else {
                    for (time in timestamps) {
                        entries.add(LyricsEntry(time = time, text = content.trim()))
                    }
                }
            }
        }

        if (entries.isEmpty() && lines.any { it.isNotBlank() }) {
            // Fallback for static lyrics
            return lines.filter { it.isNotBlank() }.mapIndexed { index, line ->
                LyricsEntry(time = index * 2000L, text = line.trim())
            }
        }

        // De-duplicate: sometimes the same lyric is provided twice with very close timestamps
        val result = mutableListOf<LyricsEntry>()
        entries.sorted().forEach { entry ->
            val last = result.lastOrNull()
            if (last != null && last.text == entry.text && Math.abs(last.time - entry.time) < 100) {
                // Skip if exact same text and timestamp within 100ms
            } else {
                result.add(entry)
            }
        }

        return result
    }
}
