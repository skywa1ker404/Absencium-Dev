package skywa1ker404.absencium.data.ocr

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Turns a recording Absencium made (M4A) into a small mono 16 kHz WAV, which is what OpenAI-style speech APIs accept.
 * Decoding and resampling happen a piece at a time, so a long recording never sits in memory whole.
 */
object AudioWav {
    private const val TARGET_RATE = 16_000
    private const val TIMEOUT_US = 10_000L

    /** Writes the WAV to [out] and returns it. Throws an [IOException] in plain words when the recording cannot be decoded. */
    fun convert(source: File, out: File): File {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(source.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException("No sound was found in this recording.")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            try {
                codec.configure(format, null, null, 0)
                codec.start()
                decode(extractor, codec, format, out)
            } finally {
                codec.release()
            }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("This recording could not be converted to WAV: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            extractor.release()
        }
        return out
    }

    private fun decode(extractor: MediaExtractor, codec: MediaCodec, startFormat: MediaFormat, out: File) {
        var rate = startFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = startFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var step = rate.toDouble() / TARGET_RATE
        var samples = 0L
        // Linear resampling state: [n] is the index of the newest input sample, [nextOut] the input position of the next output sample.
        var n = 0L
        var nextOut = 0.0
        var prev = 0.0
        RandomAccessFile(out, "rw").use { file ->
            file.setLength(0)
            file.write(ByteArray(44))
            val chunk = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN)
            fun flush() {
                file.write(chunk.array(), 0, chunk.position())
                chunk.clear()
            }
            fun emit(value: Double) {
                if (chunk.remaining() < 2) flush()
                chunk.putShort(value.toInt().coerceIn(-32768, 32767).toShort())
                samples++
            }
            fun push(cur: Double) {
                if (n == 0L) {
                    emit(cur)
                    nextOut = step
                    prev = cur
                    n = 1
                    return
                }
                while (nextOut <= n) {
                    emit(prev + (cur - prev) * (nextOut - (n - 1)))
                    nextOut += step
                }
                prev = cur
                n++
            }

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0) {
                        val buffer = codec.getInputBuffer(index)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outIndex >= 0 -> {
                        val buffer = codec.getOutputBuffer(outIndex)!!
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val shorts = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        while (shorts.remaining() >= channels) {
                            var sum = 0
                            repeat(channels) { sum += shorts.get() }
                            push(sum.toDouble() / channels)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val changed = codec.outputFormat
                        rate = changed.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = changed.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        step = rate.toDouble() / TARGET_RATE
                    }
                }
            }
            flush()
            if (samples == 0L) throw IOException("No sound was found in this recording.")
            file.seek(0)
            file.write(header(samples * 2))
        }
    }

    private fun header(dataBytes: Long): ByteArray {
        val b = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt((36 + dataBytes).toInt()).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(TARGET_RATE).putInt(TARGET_RATE * 2).putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(dataBytes.toInt())
        return b.array()
    }
}
