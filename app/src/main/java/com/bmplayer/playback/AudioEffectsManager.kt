package com.bmplayer.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer

class AudioEffectsManager(audioSessionId: Int) {
    private val equalizer = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
    private val bassBoost = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
    private val virtualizer = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()
    private val loudnessEnhancer = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()
    private val reverb = runCatching { PresetReverb(0, audioSessionId) }.getOrNull()

    val bandCount: Int get() = equalizer?.numberOfBands?.toInt() ?: 0
    val bandRange: ShortArray get() = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
    val presets: ShortArray get() = equalizer?.numberOfPresets?.let { count -> ShortArray(count.toInt()) { it.toShort() } } ?: shortArrayOf()

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        loudnessEnhancer?.enabled = enabled
        reverb?.enabled = enabled
    }

    fun setBandLevel(band: Short, level: Short) { equalizer?.setBandLevel(band, level) }
    fun setBassStrength(strength: Short) { bassBoost?.setStrength(strength.coerceIn(0, 1000)) }
    fun setVirtualizerStrength(strength: Short) { virtualizer?.setStrength(strength.coerceIn(0, 1000)) }
    fun setLoudnessGain(gainMb: Int) { loudnessEnhancer?.setTargetGain(gainMb) }
    fun setReverbPreset(preset: Short) { reverb?.preset = preset }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        loudnessEnhancer?.release()
        reverb?.release()
    }
}
