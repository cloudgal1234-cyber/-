package com.taskalarm.app.alarm

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Generates the built-in alarm tones as small WAV files on first use,
 * so the app ships without binary audio assets.
 */
object ToneSynth {
    private const val RATE = 44100

    fun ensure(dir: File, kind: String): File {
        val toneDir = File(dir, "tones").apply { mkdirs() }
        val file = File(toneDir, "$kind-v1.wav")
        if (file.exists() && file.length() > 44) return file
        val samples = when (kind) {
            "siren" -> siren()
            "beep" -> beep()
            else -> chime()
        }
        val tmp = File(toneDir, "$kind.tmp")
        writeWav(tmp, samples)
        tmp.renameTo(file)
        return file
    }

    /** Rising/falling siren, 2 s loop. */
    private fun siren(): FloatArray {
        val n = RATE * 2
        val out = FloatArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / n               // 0..1
            val tri = 1 - abs(2 * t - 1)           // 0..1..0
            val freq = 650 + 650 * tri
            phase += 2 * PI * freq / RATE
            out[i] = (0.75 * sin(phase) + 0.25 * sin(2 * phase)).toFloat()
        }
        return out
    }

    /** Classic digital alarm: four quick beeps, then a pause. */
    private fun beep(): FloatArray {
        val n = (RATE * 1.2).toInt()
        val out = FloatArray(n)
        val beepLen = (RATE * 0.09).toInt()
        val gap = (RATE * 0.07).toInt()
        for (b in 0 until 4) {
            val start = b * (beepLen + gap)
            for (j in 0 until beepLen) {
                val env = edge(j, beepLen)
                val s = sin(2 * PI * 2000 * j / RATE)
                out[start + j] = (env * (if (s >= 0) 0.8 else -0.8)).toFloat() // square wave: piercing
            }
        }
        return out
    }

    /** Ascending arpeggio C5–E5–G5–C6. */
    private fun chime(): FloatArray {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.5)
        val noteLen = (RATE * 0.28).toInt()
        val n = noteLen * notes.size + (RATE * 0.4).toInt()
        val out = FloatArray(n)
        notes.forEachIndexed { idx, f ->
            val start = idx * noteLen
            for (j in 0 until noteLen * 2) {
                val k = start + j
                if (k >= n) break
                val decay = exp(-3.0 * j / noteLen)
                val v = edge(j, noteLen * 2) * decay * (0.7 * sin(2 * PI * f * j / RATE) + 0.2 * sin(4 * PI * f * j / RATE))
                out[k] = (out[k] + v).toFloat().coerceIn(-1f, 1f)
            }
        }
        return out
    }

    /** Short fade in/out to avoid clicks. */
    private fun edge(j: Int, len: Int): Double {
        val ramp = min(400, len / 4)
        return when {
            j < ramp -> j.toDouble() / ramp
            j > len - ramp -> (len - j).toDouble() / ramp
            else -> 1.0
        }
    }

    private fun writeWav(file: File, samples: FloatArray) {
        val dataSize = samples.size * 2
        val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()); buf.putInt(36 + dataSize); buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()); buf.putInt(16); buf.putShort(1); buf.putShort(1)
        buf.putInt(RATE); buf.putInt(RATE * 2); buf.putShort(2); buf.putShort(16)
        buf.put("data".toByteArray()); buf.putInt(dataSize)
        for (s in samples) buf.putShort((s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
        FileOutputStream(file).use { it.write(buf.array()) }
    }
}
