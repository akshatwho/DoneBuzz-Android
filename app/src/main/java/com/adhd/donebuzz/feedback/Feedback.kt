package com.adhd.donebuzz.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The reward for finishing something: the hop-and-coin chime plus a haptic pattern timed to it.
 * The chime respects silent and vibrate modes.
 */
class Feedback(context: Context) {
    private val appContext = context.applicationContext
    private val audio = appContext.getSystemService(AudioManager::class.java)
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        appContext.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Vibrator::class.java)
    }
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val chime: Int = run {
        val file = File(appContext.cacheDir, "hop_and_coin.wav")
        if (!file.exists()) file.writeBytes(Synth.hopAndCoin())
        pool.load(file.path, 1)
    }

    fun taskCompleted() {
        if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) pool.play(chime, 1f, 1f, 1, 0, 1f)
        vibrate(Haptics.taskCompleted(vibrator))
    }

    /** The last open item of the day: a drum-roll into one big hit. */
    fun dayCleared() = vibrate(Haptics.dayCleared(vibrator), delayMillis = 500)

    fun tick() = vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))

    private fun vibrate(effect: VibrationEffect, delayMillis: Long = 0) {
        if (!vibrator.hasVibrator()) return
        if (delayMillis == 0L) vibrator.vibrate(effect)
        else android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ vibrator.vibrate(effect) }, delayMillis)
    }
}

/** Haptic patterns built from the phone's own primitives where it has them, so they feel crisp, not buzzy. */
object Haptics {
    private fun supports(v: Vibrator, vararg ids: Int) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && v.areAllPrimitivesSupported(*ids)

    /** A thump as the cat crouches, a rise as it jumps, the strongest tap with the coin, then two soft echoes. */
    fun taskCompleted(v: Vibrator): VibrationEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            supports(v, VibrationEffect.Composition.PRIMITIVE_THUD, VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_CLICK, VibrationEffect.Composition.PRIMITIVE_TICK)
        ) {
            return VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.6f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.7f, 30)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, 60)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 90)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.3f, 90)
                .compose()
        }
        return VibrationEffect.createWaveform(
            longArrayOf(0, 25, 60, 20, 60, 40, 80, 15, 80, 10),
            intArrayOf(0, 140, 0, 110, 0, 255, 0, 90, 0, 60),
            -1,
        )
    }

    fun dayCleared(v: Vibrator): VibrationEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            supports(v, VibrationEffect.Composition.PRIMITIVE_LOW_TICK, VibrationEffect.Composition.PRIMITIVE_THUD,
                VibrationEffect.Composition.PRIMITIVE_SPIN)
        ) {
            val c = VibrationEffect.startComposition()
            listOf(0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f).forEach { c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, it, 45) }
            return c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1f, 60)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SPIN, 0.6f, 40)
                .compose()
        }
        return VibrationEffect.createWaveform(
            longArrayOf(0, 15, 45, 15, 45, 15, 45, 20, 60, 80, 60, 120),
            intArrayOf(0, 60, 0, 90, 0, 120, 0, 160, 0, 255, 0, 80),
            -1,
        )
    }
}

/** A quick upward "boing" for the hop, then a bright G-C-E-G coin arpeggio, as 16-bit mono WAV bytes. */
object Synth {
    private const val RATE = 44_100

    fun hopAndCoin(): ByteArray {
        val duration = 0.85
        val count = (RATE * duration).toInt()
        val mix = DoubleArray(count)

        // 1. The hop: a 0.16 s sweep from 300 Hz up to 820 Hz.
        val hopLength = 0.16
        var phase = 0.0
        for (i in 0 until (RATE * hopLength).toInt()) {
            val t = i.toDouble() / RATE
            val frequency = 300 * (820.0 / 300.0).pow(t / hopLength)
            phase += 2 * PI * frequency / RATE
            val envelope = min(t / 0.004, 1.0) * (1 - t / hopLength)
            mix[i] += 0.55 * envelope * softSquare(phase)
        }

        // 2. The coin: four quick notes as the cat reaches the top; the last one rings on.
        val notes = listOf(Triple(783.99, 0.20, 18.0), Triple(1046.50, 0.26, 18.0), Triple(1318.51, 0.32, 18.0), Triple(1567.98, 0.38, 6.0))
        for ((frequency, start, decay) in notes) {
            val first = (RATE * start).toInt()
            for (i in first until count) {
                val local = (i - first).toDouble() / RATE
                val envelope = min(local / 0.003, 1.0) * exp(-local * decay)
                if (local > 0.05 && envelope < 0.0005) break
                mix[i] += 0.5 * envelope * softSquare(2 * PI * frequency * local)
            }
        }

        val pcm = ByteBuffer.allocate(44 + count * 2).order(ByteOrder.LITTLE_ENDIAN)
        pcm.put("RIFF".toByteArray()).putInt(36 + count * 2).put("WAVE".toByteArray())
        pcm.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(RATE).putInt(RATE * 2).putShort(2).putShort(16)
        pcm.put("data".toByteArray()).putInt(count * 2)
        for (value in mix) pcm.putShort(((value * 0.6).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort())
        return pcm.array()
    }

    /** A square wave from its first three odd harmonics: chiptune-flavoured but not harsh. */
    private fun softSquare(phase: Double) = sin(phase) + sin(3 * phase) / 3 + sin(5 * phase) / 5
}
