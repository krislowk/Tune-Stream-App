package com.vynce.music.service.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A highly-efficient real-time DSP AudioProcessor for Media3/ExoPlayer.
 * Intercepts decoded 16-bit PCM playback samples, computes real-time signal properties (RMS energy, peaks, estimated onsets),
 * and issues real-time frame event callbacks to listeners without distorting or degrading playback streams.
 */
@UnstableApi
class RealtimeDspAudioProcessor : AudioProcessor {

    interface DspListener {
        /**
         * Triggered on the playback thread as soon as a PCM buffer frame is processed.
         */
        fun onFrameProcessed(timestampMs: Long, rms: Float, isOnset: Boolean)
    }

    private var listener: DspListener? = null
    private var inputFormat: AudioProcessor.AudioFormat? = null
    private var outputFormat: AudioProcessor.AudioFormat? = null
    private var processedBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    private var totalBytesProcessed = 0L

    fun setDspListener(listener: DspListener) {
        this.listener = listener
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        inputFormat = inputAudioFormat
        outputFormat = inputAudioFormat
        return inputFormat!!
    }

    override fun isActive(): Boolean {
        return inputFormat != null
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        // Allocate direct buffer matching capacity
        if (processedBuffer.capacity() < remaining) {
            processedBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            processedBuffer.clear()
        }

        // Extract raw PCM bytes
        val bytes = ByteArray(remaining)
        inputBuffer.get(bytes)

        val samplesCount = remaining / 2
        if (samplesCount > 0) {
            val shorts = ShortArray(samplesCount)
            val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until samplesCount) {
                shorts[i] = byteBuffer.getShort()
            }

            // Real-time Peak and RMS calculations
            val rms = DspEngine.calculateRms(shorts, samplesCount)

            val currentFormat = inputFormat
            val sampleRate = currentFormat?.sampleRate ?: 16000
            val channelCount = currentFormat?.channelCount ?: 1

            // Count timestamp using processed bytes
            val bytesPerFrame = 2 * channelCount
            val durationSec = totalBytesProcessed.toDouble() / (bytesPerFrame * sampleRate)
            val timestampMs = (durationSec * 1000).toLong()

            totalBytesProcessed += remaining

            // Real-time simple adaptive peak onset logic
            val threshold = 0.015f
            val isOnset = rms > threshold

            listener?.onFrameProcessed(timestampMs, rms, isOnset)
        }

        processedBuffer.put(bytes)
        processedBuffer.flip()
        outputBuffer = processedBuffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val buffer = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return buffer
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer == AudioProcessor.EMPTY_BUFFER
    }

    @Deprecated("Deprecated in Java")
    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        totalBytesProcessed = 0L
    }

    override fun reset() {
        flush()
        processedBuffer = AudioProcessor.EMPTY_BUFFER
        inputFormat = null
        outputFormat = null
    }
}
