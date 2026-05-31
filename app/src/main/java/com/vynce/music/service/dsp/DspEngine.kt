package com.vynce.music.service.dsp

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * A real-time and offline digital signal processing engine for audio analysis.
 * Features:
 * - RMS energy calculation.
 * - Dynamic onset detection using transient energy derivative.
 * - Adaptive thresholding with moving average filter.
 * - Voice silence gap extraction.
 * - Timing optimization for snap-to-onset tap assistance.
 */
object DspEngine {

    const val SAMPLE_RATE = 16000 // 16 kHz mono raw PCM
    const val FRAME_SIZE = 1024    // Frame duration: 64 ms

    /**
     * Represents a discrete audio feature processed at a specific timestamp.
     */
    data class FrameFeatures(
        val timestampMs: Long,
        val rms: Float,
        val energyDb: Float,
        val isOnset: Boolean,
        val isSilence: Boolean
    )

    /**
     * Compute the Root Mean Square (RMS) energy.
     */
    fun calculateRms(buffer: ShortArray, size: Int): Float {
        if (size <= 0) return 0f
        var sumSquares = 0.0
        for (i in 0 until size) {
            val sample = buffer[i] / 32768.0 // normalize to [-1.0, 1.0]
            sumSquares += sample * sample
        }
        return sqrt(sumSquares / size).toFloat()
    }

    /**
     * Convert linear amplitude/energy to decibels relative to full scale (dBFS).
     */
    fun rmsToDb(rms: Float): Float {
        if (rms < 1e-5f) return -100f
        return (20.0 * log10(rms.toDouble())).toFloat()
    }

    /**
     * Process a stream of audio PCM buffers offline to find major onsets, peaks, and silence phases.
     * This is useful when the entire track is available or when we align historical recordings.
     */
    fun analyzePcmData(pcmData: ShortArray, sampleRate: Int = SAMPLE_RATE, frameSize: Int = FRAME_SIZE): List<FrameFeatures> {
        val totalSamples = pcmData.size
        val hopSize = frameSize // non-overlapping windows for simplicity and speed
        val framesCount = totalSamples / hopSize
        if (framesCount == 0) return emptyList()

        val rmsValues = FloatArray(framesCount)
        val timestampsMs = LongArray(framesCount)

        // Step 1: Calculate RMS for each frame
        val tempBuffer = ShortArray(frameSize)
        for (i in 0 until framesCount) {
            val offset = i * hopSize
            System.arraycopy(pcmData, offset, tempBuffer, 0, frameSize)
            rmsValues[i] = calculateRms(tempBuffer, frameSize)
            timestampsMs[i] = (offset.toDouble() / sampleRate * 1000).toLong()
        }

        // Step 2: Compute energy derivative: dE[n] = max(0, E[n] - E[n-1]) (onset detection function)
        val dE = FloatArray(framesCount)
        for (i in 1 until framesCount) {
            val diff = rmsValues[i] - rmsValues[i - 1]
            dE[i] = if (diff > 0f) diff else 0f
        }

        // Step 3: Adaptive dynamic thresholding for onset detection
        // Moving window threshold: threshold = mean(dE[i-W..i]) + alpha * std(dE[i-W..i]) + min_delta
        val windowSize = 10
        val onsetList = BooleanArray(framesCount)
        val silenceList = BooleanArray(framesCount)

        // Threshold constant parameterization
        val alpha = 1.3f // sensitivity multiplier
        val minDelta = 0.008f // relative baseline threshold
        val noiseGateRms = 0.005f // minimum energy level to trigger anything

        for (i in 0 until framesCount) {
            // Silence detection (noise gate)
            silenceList[i] = rmsValues[i] < noiseGateRms

            if (i < windowSize) {
                // Not enough history, use simple threshold
                onsetList[i] = dE[i] > minDelta && rmsValues[i] > noiseGateRms
            } else {
                // Calculate local mean and deviation
                var sum = 0f
                for (j in (i - windowSize) until i) {
                    sum += dE[j]
                }
                val mean = sum / windowSize

                var sumSqDiff = 0f
                for (j in (i - windowSize) until i) {
                    val diff = dE[j] - mean
                    sumSqDiff += diff * diff
                }
                val std = sqrt(sumSqDiff / windowSize)

                val threshold = mean + alpha * std + minDelta
                onsetList[i] = dE[i] > threshold && rmsValues[i] > noiseGateRms
            }
        }

        // Step 4: Map back into feature entities
        val results = ArrayList<FrameFeatures>(framesCount)
        for (i in 0 until framesCount) {
            // Avoid triggering dual consecutive onsets within 120ms (onset peak lock)
            var finalOnsetStatus = onsetList[i]
            if (finalOnsetStatus && i > 0) {
                // Check if any frame within past 2 frames (approx 130ms) is already an onset
                for (prev in (i - 1) downTo maxOf(0, i - 2)) {
                    if (onsetList[prev] && rmsValues[prev] >= rmsValues[i]) {
                        finalOnsetStatus = false
                        break
                    }
                }
            }

            results.add(
                FrameFeatures(
                    timestampMs = timestampsMs[i],
                    rms = rmsValues[i],
                    energyDb = rmsToDb(rmsValues[i]),
                    isOnset = finalOnsetStatus,
                    isSilence = silenceList[i]
                )
            )
        }

        return results
    }

    /**
     * Align manual tapping timings with detected DSP onsets for perfect timing synchronization.
     * Snaps a tap time to the most prominent onset within a +/- search window.
     * If no physical onset is found, it returns the tap duration unchanged.
     */
    fun snapTapToOnset(
        tapTimeMs: Long,
        dspOnsets: List<Long>,
        windowMs: Long = 300L
    ): Long {
        if (dspOnsets.isEmpty()) return tapTimeMs

        var closestOnset = -1L
        var minDiff = Long.MAX_VALUE

        for (onset in dspOnsets) {
            val diff = abs(onset - tapTimeMs)
            if (diff <= windowMs && diff < minDiff) {
                minDiff = diff
                closestOnset = onset
            }
        }

        return if (closestOnset != -1L) closestOnset else tapTimeMs
    }

    /**
     * Non-AI heuristic timing distributions for raw lyrics.
     * Maps lines to the timeline using syllables/line lengths.
     * Takes average reading/vocals pace, adds dynamic breath-gap pauses at punctuation.
     */
    fun distributeTimestampsHeuristically(
        lyricsText: String,
        totalDurationMs: Long
    ): List<Pair<String, Long>> {
        val lines = lyricsText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) return emptyList()
        if (lines.size == 1) return listOf(lines[0] to 0L)

        // Heuristic: Estimate syllables in each line using vowel cluster detection
        fun estimateSyllables(line: String): Int {
            val lowercase = line.lowercase()
            var count = 0
            var i = 0
            val vowels = setOf('a', 'e', 'i', 'o', 'u', 'y')
            while (i < lowercase.length) {
                if (lowercase[i] in vowels) {
                    count++
                    // Consume subsequent vowels (cluster is 1 syllable)
                    while (i < lowercase.length && lowercase[i] in vowels) {
                        i++
                    }
                } else {
                    i++
                }
            }
            // Fallback to characters-based if syllables estimated is 0
            return maxOf(count, 1)
        }

        // Weight elements for each line
        val syllabusCounts = lines.map { estimateSyllables(it) }
        val punctuationWeights = lines.map { line ->
            // Lines ending in punctuation get an automatic brief pause addition
            when {
                line.endsWith(".") || line.endsWith("!") || line.endsWith("?") -> 400L
                line.endsWith(",") || line.endsWith(";") -> 200L
                else -> 0L
            }
        }

        // We distribute totalDurationMs over these weights.
        // Let's preserve a safety margin at start (e.g., 1000ms delay before lyrics begin)
        // and 2000ms pause buffer at the final tail end.
        val startOffsetMs = 1500L
        val activeTimelineMs = maxOf(totalDurationMs - startOffsetMs - 2000L, 5000L)

        // Core weight of lines: proportional to syllable counts + constant layout cost
        val totalSyllables = syllabusCounts.sum()
        val baseLineCostFeedback = 1500.0 // static overhead in ms equivalents for transitions
        val totalStaticCost = lines.size * baseLineCostFeedback
        val totalWeight = totalSyllables + totalStaticCost / 100.0

        var runningMs = startOffsetMs
        val syncedLines = ArrayList<Pair<String, Long>>(lines.size)

        for (idx in lines.indices) {
            syncedLines.add(lines[idx] to runningMs)

            // Calculate duration for this line's span
            val syllableRatio = syllabusCounts[idx] / totalSyllables.toDouble()
            val baseLineWeight = syllabusCounts[idx] + (baseLineCostFeedback / 100.0)
            val proportion = baseLineWeight / (totalSyllables + (totalStaticCost / 100.0))

            val calculatedSpanMs = (proportion * activeTimelineMs).toLong()
            runningMs += calculatedSpanMs + punctuationWeights[idx]
        }

        return syncedLines
    }

    /**
     * Format ms timestamp into standard LRC tag: [mm:ss.xx]
     */
    fun formatLrcTimestamp(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centiseconds = (ms % 1000) / 10
        return String.format("[%02d:%02d.%02d]", minutes, seconds, centiseconds)
    }

    /**
     * Parses standard LRC file format back into lines with timestamps
     */
    fun parseLrc(lrcContent: String): List<Pair<Long, String>> {
        val pattern = Regex("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)")
        val results = ArrayList<Pair<Long, String>>()
        lrcContent.lines().forEach { line ->
            val match = pattern.find(line.trim())
            if (match != null) {
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val centiseconds = match.groupValues[3].toLong()
                val lyricText = match.groupValues[4].trim()

                val timeMs = (minutes * 60 * 1000) + (seconds * 1000) + (centiseconds * 10)
                results.add(timeMs to lyricText)
            }
        }
        return results.sortedBy { it.first }
    }
}
