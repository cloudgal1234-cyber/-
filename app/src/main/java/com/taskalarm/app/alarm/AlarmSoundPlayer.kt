package com.taskalarm.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.taskalarm.app.data.SoundIds

/** Loops a sound on the ALARM stream, falling back to the built-in beep if the chosen sound fails. */
class AlarmSoundPlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    val isPlaying: Boolean get() = player?.isPlaying == true

    fun start(soundId: String, playerVolume: Float = 1f) {
        stop()
        player = create(runCatching { SoundLibrary.resolve(context, soundId) }.getOrNull(), playerVolume)
            ?: create(SoundLibrary.resolve(context, SoundIds.BEEP), playerVolume)
    }

    private fun create(uri: Uri?, volume: Float): MediaPlayer? {
        if (uri == null) return null
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w("AlarmSoundPlayer", "Cannot play $uri", e)
            null
        }
    }

    fun stop() {
        player?.runCatching { stop(); release() }
        player = null
    }
}
