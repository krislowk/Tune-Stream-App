package com.vynce.music.service.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DspEngineTest {

    @Test
    fun `test lrc timestamp formatting`() {
        val ms = 65430L

        println("Input ms = $ms")

        val formatted = DspEngine.formatLrcTimestamp(ms)

        println("Formatted timestamp = $formatted")

        assertEquals("[01:05.43]", formatted)
    }

    @Test
    fun `test lrc parsing`() {
        val lrc = """
            [ar:Test Artist]
            [ti:Test Title]
            [00:10.50] First line
            [00:15.00] Second line
        """.trimIndent()

        println("LRC Input:")
        println(lrc)

        val parsed = DspEngine.parseLrc(lrc)

        println("Parsed entries:")
        parsed.forEachIndexed { index, pair ->
            println("$index -> time=${pair.first}, text='${pair.second}'")
        }

        assertEquals(2, parsed.size)
        assertEquals(10500L, parsed[0].first)
        assertEquals("First line", parsed[0].second)
        assertEquals(15000L, parsed[1].first)
        assertEquals("Second line", parsed[1].second)
    }

    @Test
    fun `test heuristic distribution`() {
        val lyrics = """
            Line one
            Line two is longer
            Line three.
        """.trimIndent()

        val duration = 30000L

        println("Lyrics:")
        println(lyrics)
        println("Duration = $duration")

        val result = DspEngine.distributeTimestampsHeuristically(
            lyrics,
            duration
        )

        println("Generated timestamps:")
        result.forEach {
            println("${it.first} -> ${it.second}")
        }

        assertEquals(3, result.size)
        assertTrue(result[0].second < result[1].second)
        assertTrue(result[1].second < result[2].second)
        assertTrue(result[2].second < duration)
        assertTrue(result[0].second >= 1500L)
    }

    @Test
    fun `test tap snapping to onset`() {
        val onsets = listOf(
            1000L,
            2000L,
            3000L
        )

        println("Onsets = $onsets")

        val snapped1 =
            DspEngine.snapTapToOnset(
                2100L,
                onsets,
                windowMs = 300L
            )

        println("Tap 2100 -> $snapped1")

        assertEquals(2000L, snapped1)

        val snapped2 =
            DspEngine.snapTapToOnset(
                2500L,
                onsets,
                windowMs = 300L
            )

        println("Tap 2500 -> $snapped2")

        assertEquals(2500L, snapped2)

        val snapped3 =
            DspEngine.snapTapToOnset(
                3000L,
                onsets,
                windowMs = 300L
            )

        println("Tap 3000 -> $snapped3")

        assertEquals(3000L, snapped3)
    }

    @Test
    fun `test empty inputs`() {
        println("Testing empty inputs")

        val parsed = DspEngine.parseLrc("")
        println("Parsed empty LRC = $parsed")

        val distributed =
            DspEngine.distributeTimestampsHeuristically(
                "",
                30000L
            )

        println("Distributed empty lyrics = $distributed")

        val snapped =
            DspEngine.snapTapToOnset(
                1234L,
                emptyList()
            )

        println("Snapped with empty onsets = $snapped")

        assertTrue(parsed.isEmpty())
        assertTrue(distributed.isEmpty())
        assertEquals(1234L, snapped)
    }

    @Test
    fun `test heuristic distribution with punctuation`() {
        val lyrics = """
            Hello!
            How are you, today?
            I'm fine.
        """.trimIndent()

        println("Punctuation lyrics:")
        println(lyrics)

        val result =
            DspEngine.distributeTimestampsHeuristically(
                lyrics,
                20000L
            )

        result.forEach {
            println("${it.first} -> ${it.second}")
        }

        assertEquals(3, result.size)
        assertTrue(result[0].second < result[1].second)
        assertTrue(result[1].second < result[2].second)
    }

    @Test
    fun `test syllable estimation`() {
        val lyrics = """
            Short
            This is a much longer line with many words
        """.trimIndent()

        println("Lyrics:")
        println(lyrics)

        val result =
            DspEngine.distributeTimestampsHeuristically(
                lyrics,
                10000L
            )

        result.forEach {
            println("${it.first} -> ${it.second}")
        }

        val gap1 = result[1].second - result[0].second

        println("Gap between lines = $gap1 ms")

        assertTrue(gap1 > 0)
    }
}