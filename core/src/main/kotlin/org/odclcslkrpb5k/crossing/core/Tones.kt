package org.odclcslkrpb5k.crossing.core

import kotlin.math.PI
import kotlin.math.sin

/** A game sound, as 16-bit mono samples at [Tones.RATE]. */
class Tone(val name: String, val samples: ShortArray)

/**
 * The game's sounds, made from sine waves when the game starts: no recordings, so they are
 * original, tiny, and the same on every device. Each note fades in and out over a few milliseconds,
 * so none clicks, and stays well below full scale, so none is harsh.
 */
object Tones {
    const val RATE: Int = 22_050

    /** Loudest sample, as a share of full scale. */
    const val PEAK: Double = 0.35

    /** A short rising blip. */
    val HOP: Tone = Tone("hop", notes(listOf(Note(660.0, 0.04), Note(880.0, 0.05))))

    /** A soft falling wobble: a bump, not a punishment. */
    val BONK: Tone =
        Tone("bonk", notes(listOf(Note(392.0, 0.08), Note(330.0, 0.08), Note(262.0, 0.14))))

    /** Two bright notes. */
    val STAR: Tone = Tone("star", notes(listOf(Note(1047.0, 0.07), Note(1319.0, 0.12))))

    /** A little rising tune at the rainbow. */
    val GOAL: Tone =
        Tone(
            "goal",
            notes(
                listOf(Note(523.0, 0.12), Note(659.0, 0.12), Note(784.0, 0.12), Note(1047.0, 0.3))
            ),
        )

    val ALL: List<Tone> = listOf(HOP, BONK, STAR, GOAL)

    private class Note(val hertz: Double, val seconds: Double)

    private fun notes(notes: List<Note>): ShortArray {
        val out = ShortArray(notes.sumOf { (it.seconds * RATE).toInt() })
        var at = 0
        for (note in notes) {
            val n = (note.seconds * RATE).toInt()
            val fade = minOf((FADE_SECONDS * RATE).toInt(), n / 2)
            for (i in 0 until n) {
                val envelope = minOf(1.0, i.toDouble() / fade, (n - 1 - i).toDouble() / fade)
                val wave = sin(2 * PI * note.hertz * i / RATE)
                out[at + i] = (wave * envelope * PEAK * Short.MAX_VALUE).toInt().toShort()
            }
            at += n
        }
        return out
    }

    private const val FADE_SECONDS = 0.006
}
