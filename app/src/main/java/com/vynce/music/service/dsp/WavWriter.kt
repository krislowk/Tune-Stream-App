package com.vynce.music.service.dsp

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Helper to write standard RIFF WAV files from 16-bit PCM Short data.
 */
object WavWriter {

    fun writeWavFile(pcmData: ShortArray, sampleRate: Int, outputFile: File) {
        val totalAudioLen = pcmData.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        outputFile.outputStream().use { out ->
            // RIFF header
            out.write("RIFF".toByteArray())
            out.write(intToByteArray(totalDataLen))
            out.write("WAVE".toByteArray())

            // Format chunk
            out.write("fmt ".toByteArray())
            out.write(intToByteArray(16)) // sub-chunk block size
            out.write(shortToByteArray(1)) // PCM linear format = 1
            out.write(shortToByteArray(channels.toShort()))
            out.write(intToByteArray(sampleRate))
            out.write(intToByteArray(byteRate))
            out.write(shortToByteArray(2)) // Block align: channels * bitsPerSample / 8
            out.write(shortToByteArray(16)) // bits per sample

            // Data chunk
            out.write("data".toByteArray())
            out.write(intToByteArray(totalAudioLen))

            // Write PCM data in Little Endian format
            val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            byteBuffer.asShortBuffer().put(pcmData)
            out.write(byteBuffer.array())
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        val b = ByteArray(4)
        b[0] = (value and 0xFF).toByte()
        b[1] = ((value shr 8) and 0xFF).toByte()
        b[2] = ((value shr 16) and 0xFF).toByte()
        b[3] = ((value shr 24) and 0xFF).toByte()
        return b
    }

    private fun shortToByteArray(value: Short): ByteArray {
        val b = ByteArray(2)
        b[0] = (value.toInt() and 0xFF).toByte()
        b[1] = ((value.toInt() shr 8) and 0xFF).toByte()
        return b
    }
}
