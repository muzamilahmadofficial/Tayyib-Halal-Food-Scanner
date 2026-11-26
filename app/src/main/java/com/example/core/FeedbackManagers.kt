package com.example.core

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R
import com.example.domain.HalalVerdict

class SoundFeedbackManager(context: Context) {

    private val soundPool: SoundPool
    private val scanConfirmId: Int
    private val halalId: Int
    private val haramId: Int
    private val doubtfulId: Int
    private val unknownId: Int

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        scanConfirmId = safeLoad(context, R.raw.sound_scan_confirm)
        halalId = safeLoad(context, R.raw.sound_halal)
        haramId = safeLoad(context, R.raw.sound_haram)
        doubtfulId = safeLoad(context, R.raw.sound_doubtful)
        unknownId = safeLoad(context, R.raw.sound_unknown)
    }

    private fun safeLoad(context: Context, resId: Int): Int = runCatching {
        soundPool.load(context, resId, 1)
    }.getOrDefault(0)

    fun playScanConfirm(enabled: Boolean) {
        if (!enabled || scanConfirmId == 0) return
        soundPool.play(scanConfirmId, 0.75f, 0.75f, 1, 0, 1.0f)
    }

    /**
     * Plays a distinct, non-intrusive audio cue strictly matched to the verified classification.
     * Never plays the Halal sound for Unknown or Doubtful results.
     */
    fun playVerdict(verdict: HalalVerdict, enabled: Boolean) {
        if (!enabled) return
        val soundId = when (verdict) {
            HalalVerdict.HALAL -> halalId
            HalalVerdict.HARAM -> haramId
            HalalVerdict.DOUBTFUL -> doubtfulId
            HalalVerdict.UNKNOWN -> unknownId
        }
        if (soundId != 0) {
            soundPool.play(soundId, 0.85f, 0.85f, 1, 0, 1.0f)
        }
    }

    fun release() {
        soundPool.release()
    }
}

class HapticFeedbackManager(private val context: Context) {

    private fun getVibrator(): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    fun vibrateScanConfirm(enabled: Boolean) {
        if (!enabled) return
        vibratePattern(longArrayOf(0, 35))
    }

    fun vibrateForVerdict(verdict: HalalVerdict, enabled: Boolean) {
        if (!enabled) return
        val pattern = when (verdict) {
            HalalVerdict.HALAL -> longArrayOf(0, 50)
            HalalVerdict.HARAM -> longArrayOf(0, 90, 70, 110)
            HalalVerdict.DOUBTFUL -> longArrayOf(0, 55, 65, 55)
            HalalVerdict.UNKNOWN -> longArrayOf(0, 30)
        }
        vibratePattern(pattern)
    }

    private fun vibratePattern(timings: LongArray) {
        val vibrator = getVibrator() ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        }
    }
}

open class NetworkMonitor(private val context: Context) {

    open fun isOnline(): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)
}
