package org.odclcslkrpb5k.crossing.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** How far she has got: the level to play next, and every star collected so far. */
data class Progress(val level: Int = 1, val stars: Int = 0) {
    init {
        require(level in 1..MAX_LEVEL) { "level" }
        require(stars in 0..MAX_STARS) { "stars" }
    }

    companion object {
        const val MAX_LEVEL: Int = 9_999
        const val MAX_STARS: Int = 999_999
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
 * The saved form of [Progress], little-endian: magic "CRSS" | version u16 | level u16 | stars i32.
 * A file of any other size, magic or version, or with values out of range, is an error value, never
 * an exception (BD-1): the game then starts from the beginning rather than crash.
 */
object ProgressCodec {
    const val VERSION: Int = 1
    private const val MAGIC = 0x53535243 // "CRSS" read little-endian
    private const val SIZE = 4 + 2 + 2 + 4
    private const val U16 = 0xFFFF

    fun encode(p: Progress): ByteArray =
        ByteBuffer.allocate(SIZE)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(MAGIC)
            .putShort(VERSION.toShort())
            .putShort(p.level.toShort())
            .putInt(p.stars)
            .array()

    fun decode(bytes: ByteArray): Outcome<Progress, ProgressError> {
        if (bytes.size != SIZE) return Outcome.Err(ProgressError.WRONG_SIZE)
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (b.int != MAGIC) return Outcome.Err(ProgressError.NOT_PROGRESS)
        if (b.short.toInt() and U16 != VERSION) return Outcome.Err(ProgressError.UNKNOWN_VERSION)
        val level = b.short.toInt() and U16
        val stars = b.int
        val inRange = level in 1..Progress.MAX_LEVEL && stars in 0..Progress.MAX_STARS
        return if (inRange) Outcome.Ok(Progress(level, stars))
        else Outcome.Err(ProgressError.OUT_OF_RANGE)
    }
}
