package com.vynce.music.service.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A standard Media3 ExoPlayer AudioProcessor that intercepts playing raw 16-bit PCM buffer packets
 * in real-time, allowing online amplitude peaks, energy levels, and onset signals to be computed.
 */
@UnstableApi
class DspAudioProcessor : AudioProcessor {

    private var inputFormat = AudioFormat.NOT_SET
    private var outputFormat = AudioFormat.NOT_SET
    private var buffer = EMPTY_BUFFER
    private var outputBuffer = EMPTY_BUFFER
    private var inputEnded = false

    // Real-time listener calling back with Short PCM segments
    var onPcmFrameProcessed: ((ShortArray, Int, Int) -> Unit)? = null

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        inputFormat = inputAudioFormat
        outputFormat = inputAudioFormat
        return outputFormat
    }

    override fun isActive(): Boolean {
        return inputFormat != AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return

        val shortsToRead = remaining / 2
        val shortArray = ShortArray(shortsToRead)

        val duplicate = inputBuffer.duplicate().order(ByteOrder.nativeOrder())
        try {
            duplicate.asShortBuffer().get(shortArray)
            // Call PCM interceptor. SampleRate can be obtained from inputFormat.sampleRate
            onPcmFrameProcessed?.invoke(shortArray, shortsToRead, inputFormat.sampleRate)
        } catch (_: Exception) {
            // Silently absorb rendering failures
        }

        // Direct feed-through mechanism
        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }
        buffer.put(inputBuffer)
        buffer.flip()
        outputBuffer = buffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer === EMPTY_BUFFER
    }

    @Deprecated("Deprecated in Java")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
    }

    override fun reset() {
        flush()
        buffer = EMPTY_BUFFER
        inputFormat = AudioFormat.NOT_SET
        outputFormat = AudioFormat.NOT_SET
    }
}
