package org.odclcslkrpb5k.crossing.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * How far she has got: the level to play next, every star collected so far, and the character she
 * chose (its place in [Heroes.ALL]; whether it is unlocked is the game's to check).
 */
data class Progress(val level: Int = 1, val stars: Int = 0, val hero: Int = 0) {
    init {
        require(level in 1..MAX_LEVEL) { "level" }
        require(stars in 0..MAX_STARS) { "stars" }
        require(hero in 0..MAX_HERO) { "hero" }
    }

    companion object {
        const val MAX_LEVEL: Int = 9_999
        const val MAX_STARS: Int = 999_999
        const val MAX_HERO: Int = 255
    }
}

/** Why saved progress could not be read. */
enum class ProgressError {
    WRONG_SIZE,
    NOT_PROGRESS,
    UNKNOWN_VERSION,
    OUT_OF_RANGE,
}

/**
 * The saved form of [Progress], little-endian:
 *
 *     magic "CRSS" | version u16 | level u16 | stars i32 | hero u8   (hero: version 2 on)
 *
 * The reader also accepts version 1, which had no hero (the foal). A file of the wrong size for its
 * version, another magic or version, or values out of range, is an error value, never an exception
 * (BD-1): the game then starts from the beginning rather than crash.
 */
object ProgressCodec {
    const val VERSION: Int = 2
    private const val MAGIC = 0x53535243 // "CRSS" read little-endian
    private const val HEADER = 4 + 2
    private const val V1_SIZE = HEADER + 2 + 4
    private const val SIZE = V1_SIZE + 1
    private const val U16 = 0xFFFF
    private const val U8 = 0xFF

    fun encode(p: Progress): ByteArray =
        ByteBuffer.allocate(SIZE)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(MAGIC)
            .putShort(VERSION.toShort())
            .putShort(p.level.toShort())
            .putInt(p.stars)
            .put(p.hero.toByte())
            .array()

    fun decode(bytes: ByteArray): Outcome<Progress, ProgressError> {
        if (bytes.size < HEADER) return Outcome.Err(ProgressError.WRONG_SIZE)
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (b.int != MAGIC) return Outcome.Err(ProgressError.NOT_PROGRESS)
        val version = b.short.toInt() and U16
        val size =
            when (version) {
                1 -> V1_SIZE
                VERSION -> SIZE
                else -> return Outcome.Err(ProgressError.UNKNOWN_VERSION)
            }
        if (bytes.size != size) return Outcome.Err(ProgressError.WRONG_SIZE)
        val level = b.short.toInt() and U16
        val stars = b.int
        val hero = if (version >= VERSION) b.get().toInt() and U8 else 0
        val inRange = level in 1..Progress.MAX_LEVEL && stars in 0..Progress.MAX_STARS
        return if (inRange) Outcome.Ok(Progress(level, stars, hero))
        else Outcome.Err(ProgressError.OUT_OF_RANGE)
    }
}
