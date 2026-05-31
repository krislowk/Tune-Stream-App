package com.vynce.music.service.dsp

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * A real-time synthesizer that generates raw PCM auditory data playing through Android's AudioTrack.
 * Contains pre-programmed rhythmic beats and vocalic modulating hums that match a built-in demo song perfectly.
 * Provides a real-time playback API and exposes visual peaks for waveform sync rendering.
 */
class SynthAudioTrack {

    companion object {
        const val SAMPLE_RATE = 16000
        const val TOTAL_DURATION_SEC = 30
        const val TOTAL_SAMPLES = SAMPLE_RATE * TOTAL_DURATION_SEC

        // A beautiful demo lyric set that matches the synthesized vocal tracks perfectly
        val DEMO_LYRICS = """
            [00:01.00]Ready in five, four, three, two, one...
            [00:06.50]Welcome to the DSP sync zone!
            [00:10.00]Watch the lines jump on the beat.
            [00:13.50]Tones are flowing smooth and sweet.
            [00:17.00]Press the TAP button to snap the text.
            [00:20.50]Your LRC output is ready next!
            [00:24.00]Let the synthetic rhythm take control.
            [00:27.50]This is the end of the spectral scroll.
        """.trimIndent()

        val DEMO_RAW_LYRICS = """
            Ready in five, four, three, two, one...
            Welcome to the DSP sync zone!
            Watch the lines jump on the beat.
            Tones are flowing smooth and sweet.
            Press the TAP button to snap the text.
            Your LRC output is ready next!
            Let the synthetic rhythm take control.
            This is the end of the spectral scroll.
        """.trimIndent()

        // Ground-truth vocal times matching the demo lyrics (for synthesis)
        val VOCAL_TIMINGS = listOf(
            VocalTrigger(1000L, 2500L, 340f), // Ready in five, four...
            VocalTrigger(6500L, 2200L, 260f), // Welcome to the DSP...
            VocalTrigger(10000L, 2300L, 300f), // Watch the lines...
            VocalTrigger(13500L, 2200L, 280f), // Tones are flowing...
            VocalTrigger(17000L, 2300L, 320f), // Press the TAP...
            VocalTrigger(20500L, 2200L, 300f), // Your LRC...
            VocalTrigger(24000L, 24000L + 2200L, 240f), // Let the synthetic...
            VocalTrigger(27500L, 27500L + 2100L, 220f)  // This is the end...
        )
    }

    data class VocalTrigger(val startMs: Long, val durationMs: Long, val freqHz: Float)

    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Holds the full synthesized 30-second audio track in memory.
     * This makes it lighting-fast to analyze offline, seek, and draw beautifully.
     */
    val fullBuffer: ShortArray by lazy {
        generateEntireTrack()
    }

    // List of playback position listeners (positionMs)
    private var onPlaybackPositionChangedListener: ((Long) -> Unit)? = null
    private var isPlaying = false
    private var playheadSample = 0

    fun setOnPlaybackPositionChangedListener(listener: (Long) -> Unit) {
        this.onPlaybackPositionChangedListener = listener
    }

    private fun generateEntireTrack(): ShortArray {
        val buffer = ShortArray(TOTAL_SAMPLES)
        val bpm = 75
        val beatIntervalSamples = (SAMPLE_RATE * 60) / bpm // ~12800 samples or 800ms
        val randomState = Random(42)

        for (n in 0 until TOTAL_SAMPLES) {
            val tSec = n.toDouble() / SAMPLE_RATE
            val tMs = (tSec * 1000).toLong()

            var outValue = 0.0

            // 1. Drum beat component (impulses every beatIntervalSamples)
            val sampleFromLastBeat = n % beatIntervalSamples
            val beatTimeSec = sampleFromLastBeat.toDouble() / SAMPLE_RATE

            // Kick drum: decaying sine sweep
            val kickDecay = exp(-40.0 * beatTimeSec)
            if (kickDecay > 0.001) {
                val kickFreq = 180.0 * exp(-90.0 * beatTimeSec) + 45.0
                outValue += kickDecay * sin(2.0 * PI * kickFreq * beatTimeSec) * 0.45
            }

            // Snare hit: white noise on alternate beats
            val beatIndex = n / beatIntervalSamples
            if (beatIndex % 2 == 1) {
                val snareDecay = exp(-18.0 * beatTimeSec)
                if (snareDecay > 0.001) {
                    val whiteNoise = (randomState.nextFloat() * 2.0f - 1.0f)
                    outValue += snareDecay * whiteNoise * 0.15
                }
            }

            // High hat hit on syncopated/off beats (delay of half beatInterval)
            val hatOffset = beatIntervalSamples / 2
            val sampleFromLastHat = (n + hatOffset) % beatIntervalSamples
            val hatTimeSec = sampleFromLastHat.toDouble() / SAMPLE_RATE
            val hatDecay = exp(-120.0 * hatTimeSec)
            if (hatDecay > 0.001) {
                val blueNoise = (randomState.nextFloat() * 2.0f - 1.0f)
                outValue += hatDecay * blueNoise * 0.08
            }

            // 2. Synthesized Vocal Modulating Component
            for (vocal in VOCAL_TIMINGS) {
                if (tMs >= vocal.startMs && tMs < vocal.startMs + vocal.durationMs) {
                    val vocalRelativeSec = (tMs - vocal.startMs) / 1000.0
                    // Voice envelope: modulating hum + vibrato + amplitude syllable envelopes
                    val vibrato = 1.0 + 0.04 * sin(2.0 * PI * 6.5 * vocalRelativeSec)
                    val freq = vocal.freqHz * vibrato

                    // Generate a rich vocal buzz (combination of fundamental and second harmonic)
                    val fundamental = sin(2.0 * PI * freq * vocalRelativeSec)
                    val harmonic2 = 0.4 * sin(2.0 * PI * (freq * 2) * vocalRelativeSec)
                    val rawWave = fundamental + harmonic2

                    // Syllable gating: multiply by low-frequency modulator to simulate word syllables
                    val syllableEnvelope = 0.5 + 0.5 * sin(2.0 * PI * 4.0 * vocalRelativeSec)
                    // Fade in/fade out envelope
                    val totalVocalSec = vocal.durationMs / 1000.0
                    val fadeEnv = if (vocalRelativeSec < 0.1) {
                        vocalRelativeSec / 0.1
                    } else if (vocalRelativeSec > totalVocalSec - 0.15) {
                        (totalVocalSec - vocalRelativeSec) / 0.15
                    } else {
                        1.0
                    }

                    outValue += rawWave * syllableEnvelope * fadeEnv * 0.32
                    break // play one vocal at a time
                }
            }

            // Clamping the synthesized value to avoid PCM clipping
            val clamped = outValue.coerceIn(-1.0, 1.0)
            buffer[n] = (clamped * 32767.0).toInt().toShort()
        }

        return buffer
    }

    /**
     * Start playing synthesized track.
     */
    fun play(fromMs: Long = 0L) {
        if (isPlaying) return
        isPlaying = true

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize, 4096),
            AudioTrack.MODE_STREAM
        ).apply {
            play()
        }

        playheadSample = (fromMs * SAMPLE_RATE / 1000L).toInt().coerceIn(0, TOTAL_SAMPLES)

        synthJob = scope.launch {
            val chunkSamples = 1024
            val chunkBuffer = ShortArray(chunkSamples)

            while (isPlaying && playheadSample < TOTAL_SAMPLES) {
                val samplesToWrite = minOf(chunkSamples, TOTAL_SAMPLES - playheadSample)
                System.arraycopy(fullBuffer, playheadSample, chunkBuffer, 0, samplesToWrite)

                // Fill remainder if final frame is shorter
                if (samplesToWrite < chunkSamples) {
                    for (i in samplesToWrite until chunkSamples) {
                        chunkBuffer[i] = 0
                    }
                }

                audioTrack?.write(chunkBuffer, 0, chunkSamples)
                playheadSample += samplesToWrite

                val currentMs = (playheadSample.toDouble() / SAMPLE_RATE * 1000).toLong()
                onPlaybackPositionChangedListener?.invoke(currentMs)

                // Low-latency Sleep timer to mimic output streaming speed
                val sleepTime = (chunkSamples.toDouble() / SAMPLE_RATE * 1000).toLong()
                delay(sleepTime)
            }
            isPlaying = false
            onPlaybackPositionChangedListener?.invoke((playheadSample.toDouble() / SAMPLE_RATE * 1000).toLong())
        }
    }

    /**
     * Stop and recycle
     */
    fun stop() {
        isPlaying = false
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e("SynthAudioTrack", "Error stopping track", e)
        }
        audioTrack = null
    }

    fun isPlaying(): Boolean = isPlaying

    fun getDurationMs(): Long = TOTAL_DURATION_SEC * 1000L
}
