package org.odclcslkrpb5k.crossing.adapters

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import org.odclcslkrpb5k.crossing.core.Tone
import org.odclcslkrpb5k.crossing.core.Tones

/**
 * Plays the game's [Tones], each from its own static track loaded once, so playing one is immediate
 * and allocates nothing. A sound that cannot be set up is logged and stays silent: the game never
 * stops for its sounds. Main thread.
 */
class Speaker {
    private val tracks: Map<Tone, AudioTrack> =
        Tones.ALL.mapNotNull { t -> load(t)?.let { t to it } }.toMap()

    fun play(tone: Tone) {
        val track = tracks[tone] ?: return
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.playbackHeadPosition = 0
            track.play()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "${tone.name} not played: ${e.javaClass.simpleName}")
        }
    }

    fun release() {
        tracks.values.forEach { it.release() }
    }

    private fun load(tone: Tone): AudioTrack? =
        try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(Tones.RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(tone.samples.size * 2)
                .build()
                .let { track ->
                    val written = track.write(tone.samples, 0, tone.samples.size)
                    if (written == tone.samples.size) {
                        track
                    } else {
                        // A track that is not whole is let go at once, not kept half loaded.
                        track.release()
                        Log.w(
                            TAG,
                            "${tone.name} not loaded: wrote $written of ${tone.samples.size}",
                        )
                        null
                    }
                }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "${tone.name} not loaded: ${e.javaClass.simpleName}")
            null
        } catch (e: UnsupportedOperationException) {
            Log.w(TAG, "${tone.name} not loaded: ${e.javaClass.simpleName}")
            null
        }

    private companion object {
        const val TAG = "Speaker"
    }
}
