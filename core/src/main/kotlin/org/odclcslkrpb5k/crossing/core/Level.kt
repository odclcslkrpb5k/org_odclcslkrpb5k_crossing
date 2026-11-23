package org.odclcslkrpb5k.crossing.core

import kotlin.math.floor
import kotlin.random.Random

/** The playfield: the tablet's landscape screen in square cells, row 0 at the top. */
object Board {
    const val COLS: Int = 16
    const val ROWS: Int = 10

    /** The row the character starts each level on, at the bottom. */
    const val START_ROW: Int = ROWS - 1

    /** The column the character starts on, and comes back to after a bonk. */
    const val START_COL: Int = COLS / 2
}

/** What a row is made of. Grass and the goal are safe; a road has carts; a stream has logs. */
enum class Ground {
    GRASS,
    ROAD,
    STREAM,
    GOAL,
}

/**
 * One row. Its movers (carts on a road, logs on a stream) are each [length] cells long and move
 * [speed] cells a second (negative: leftwards), wrapping around a loop of [loop] cells that is
 * longer than the board, so each one leaves the screen on one side before it comes back on the
 * other. A grass or goal row has none. Where a mover is depends only on the time, never on what
 * happened before: the same time always gives the same picture.
 */
data class Lane(
    val ground: Ground,
    val speed: Float = 0f,
    val length: Float = 0f,
    val starts: List<Float> = emptyList(),
    val loop: Float = Board.COLS.toFloat(),
) {
    init {
        require(loop >= Board.COLS) { "loop" }
        require(starts.isEmpty() || length > 0f) { "length" }
        require(ground == Ground.ROAD || ground == Ground.STREAM || starts.isEmpty()) { "movers" }
    }

    /** Each mover's left edge at [seconds], in [-length, loop - length). */
    fun movers(seconds: Double): List<Float> = starts.map { start ->
        val moved = start + speed * seconds
        (moved - loop * floor((moved + length) / loop)).toFloat()
    }

    /** Whether a mover covers the point [x] (in cells, a cell's centre is at col + 0.5). */
    fun covers(x: Float, seconds: Double): Boolean =
        movers(seconds).any { left -> x >= left && x < left + length }
}

/**
 * A level: its rows top to bottom and where its stars wait (row to column). Made from a number and
 * a seed only, so a level can be played again exactly, and a test can play every one.
 */
data class Level(val number: Int, val lanes: List<Lane>, val stars: Map<Int, Int>) {
    init {
        require(lanes.size == Board.ROWS) { "rows" }
        require(lanes.first().ground == Ground.GOAL) { "goal" }
        require(lanes[Board.START_ROW].ground == Ground.GRASS) { "start" }
        require(
            stars.all { (row, col) ->
                lanes[row].ground == Ground.GRASS && col in 0 until Board.COLS
            }
        ) {
            "stars"
        }
    }

    companion object {
        /** Rows top to bottom: goal, two streams, grass, two roads, grass, two roads, start. */
        private val LAYOUT =
            listOf(
                Ground.GOAL,
                Ground.STREAM,
                Ground.STREAM,
                Ground.GRASS,
                Ground.ROAD,
                Ground.ROAD,
                Ground.GRASS,
                Ground.ROAD,
                Ground.ROAD,
                Ground.GRASS,
            )

        /**
         * Level [number] (from 1). Speeds start gentle for a six-year-old and grow a little with
         * each level, up to a cap; neighbouring lanes run opposite ways. Carts leave gaps of at
         * least [MIN_ROAD_GAP] cells; logs are long and close together.
         */
        fun make(number: Int, seed: Long): Level {
            require(number >= 1) { "number" }
            val random = Random(seed * LEVEL_MIX + number)
            val pace = minOf(1f + (number - 1) * PACE_STEP, MAX_PACE)
            val lanes = LAYOUT.mapIndexed { row, ground ->
                val direction = if (row % 2 == 0) 1f else -1f
                when (ground) {
                    Ground.ROAD -> movers(ground, random, pace * direction, CART, CARTS)
                    Ground.STREAM -> movers(ground, random, pace * direction, LOG, LOGS)
                    Ground.GRASS,
                    Ground.GOAL -> Lane(ground)
                }
            }
            val stars =
                LAYOUT.indices
                    .filter { LAYOUT[it] == Ground.GRASS && it != Board.START_ROW }
                    .associateWith { random.nextInt(Board.COLS) }
            return Level(number, lanes, stars)
        }

        /** Evenly spaced movers, each nudged a little, all at one speed in the lane. */
        private fun movers(
            ground: Ground,
            random: Random,
            pace: Float,
            length: Float,
            count: Int,
        ): Lane {
            val speed = pace * (MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED))
            val spacing = LOOP / count
            val offset = random.nextFloat() * spacing
            // Each mover sits in its own slot of the loop, nudged by at most half the slack, so
            // neighbours are always at least spacing - length - slack / 2 apart.
            val slack = (spacing - length - MIN_ROAD_GAP).coerceAtLeast(0f)
            val starts = List(count) { i -> offset + i * spacing + random.nextFloat() * slack / 2 }
            return Lane(ground, speed, length, starts, LOOP)
        }

        private const val LEVEL_MIX = 1_000_003L
        private const val PACE_STEP = 0.1f
        private const val MAX_PACE = 1.8f
        private const val MIN_SPEED = 0.8f
        private const val MAX_SPEED = 1.5f
        private const val LOOP = 24f
        private const val CART = 2f
        private const val CARTS = 3
        private const val LOG = 4f
        private const val LOGS = 4

        /** The narrowest gap between carts, in cells: room to hop into, with time to spare. */
        const val MIN_ROAD_GAP: Float = 3f

        /** The shortest log, in cells: a hop lands on it without fine timing. */
        const val MIN_LOG: Float = 3f
    }
}
