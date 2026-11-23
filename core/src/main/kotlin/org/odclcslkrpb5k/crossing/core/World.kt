package org.odclcslkrpb5k.crossing.core

import kotlin.math.roundToInt

/** A hop the child asks for: a tap is up, a swipe is the other three. */
enum class Move {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

/** Where the character is: [x] is the centre in cells (col + 0.5 on land; anywhere on a log). */
data class Player(val x: Float, val row: Int) {
    /** The column the character stands in. */
    val col: Int
        get() = (x - HALF).roundToInt().coerceIn(0, Board.COLS - 1)

    companion object {
        const val HALF: Float = 0.5f

        fun at(col: Int, row: Int): Player = Player(col + HALF, row)
    }
}

/**
 * The game, as a value: each [step] is one fixed tick ([DT] seconds), so the same level and the
 * same moves always play out the same way. There is no game over and no timer. A bonk (a cart on
 * the road, or the stream with no log underfoot) sends the character back to the last grass row it
 * stood on, a little dizzy for [DIZZY_TICKS], and takes nothing away. Stars stay collected.
 */
data class World(
    val level: Level,
    val tick: Long = 0,
    val player: Player = Player.at(Board.START_COL, Board.START_ROW),
    val checkpoint: Int = Board.START_ROW,
    val stars: Int = 0,
    val taken: Set<Int> = emptySet(),
    val bonks: Int = 0,
    /** Hops taken that moved her (a hop clamped at an edge, or ignored while dizzy, is none). */
    val hops: Int = 0,
    val dizzyUntil: Long = 0,
) {
    val seconds: Double
        get() = tick * DT

    /** The character reached the goal row; the screen celebrates and moves on ([next]). */
    val finished: Boolean
        get() = player.row == 0

    val dizzy: Boolean
        get() = tick < dizzyUntil

    /** One tick: the hop asked for (ignored while dizzy or finished), then the world moves on. */
    fun step(move: Move?): World {
        if (finished) return copy(tick = tick + 1)
        val hopped = if (move != null && !dizzy) hop(move) else this
        return hopped.advance()
    }

    /** The next level, with every star kept. */
    fun next(seed: Long): World = World(Level.make(level.number + 1, seed), stars = stars)

    private fun hop(move: Move): World {
        val p = player
        val onLand = level.lanes[p.row].ground != Ground.STREAM
        // On land the character always stands at a cell's centre; on a log it may be anywhere.
        val x = if (onLand) p.col + Player.HALF else p.x
        val moved =
            when (move) {
                Move.UP -> Player(x, p.row - 1)
                Move.DOWN -> Player(x, (p.row + 1).coerceAtMost(Board.START_ROW))
                Move.LEFT -> Player((x - 1).coerceAtLeast(Player.HALF), p.row)
                Move.RIGHT -> Player((x + 1).coerceAtMost(Board.COLS - Player.HALF), p.row)
            }
        // Landing on land snaps to the nearest cell, so a hop off a log lands squarely.
        val landed =
            if (level.lanes[moved.row].ground == Ground.STREAM) moved
            else Player.at(moved.col, moved.row)
        if (landed == player) return this
        return copy(player = landed, hops = hops + 1).arrived()
    }

    /** Grass the character reaches is its new checkpoint, and its star, if any, is taken. */
    private fun arrived(): World {
        val row = player.row
        if (level.lanes[row].ground != Ground.GRASS) return this
        val star = level.stars[row]
        val gets = star != null && star == player.col && row !in taken
        return copy(
            checkpoint = row,
            stars = if (gets) stars + 1 else stars,
            taken = if (gets) taken + row else taken,
        )
    }

    /** Time moves on: a log carries the character; a cart or the water bonks it. */
    private fun advance(): World {
        val next = copy(tick = tick + 1)
        val lane = level.lanes[player.row]
        val t = next.seconds
        return when (lane.ground) {
            Ground.ROAD -> if (lane.covers(player.x, t)) next.bonk() else next
            Ground.STREAM -> {
                // Carried no further than a hop can go: at the edge she stays on screen until the
                // log slides out from under her.
                val carried =
                    (player.x + lane.speed * DT.toFloat()).coerceIn(
                        Player.HALF,
                        Board.COLS - Player.HALF,
                    )
                val riding = next.copy(player = player.copy(x = carried))
                if (lane.covers(carried, t)) riding else riding.bonk()
            }
            Ground.GRASS,
            Ground.GOAL -> next
        }
    }

    private fun bonk(): World =
        copy(
            player = Player.at(Board.START_COL, checkpoint),
            bonks = bonks + 1,
            dizzyUntil = tick + DIZZY_TICKS,
        )

    companion object {
        /** Ticks per second: the simulation's fixed step, independent of the screen's rate. */
        const val RATE: Int = 60
        const val DT: Double = 1.0 / RATE

        /**
         * After a bonk, hops are ignored this long, so a frantic tap does not bonk again at once.
         */
        const val DIZZY_TICKS: Long = 36
    }
}

/** What a tick did that the child should hear: one at most, the most important first. */
enum class Event {
    BONK,
    GOAL,
    STAR,
    HOP,
}

/**
 * The event between [before] and this world, one tick apart on the same level, or null. A hop is
 * one the world counted: riding a log moves her without one, and a sideways hop on a log is one. A
 * new level is no event (its start is not a hop back down the board).
 */
fun World.eventSince(before: World): Event? =
    when {
        level.number != before.level.number -> null
        bonks > before.bonks -> Event.BONK
        finished && !before.finished -> Event.GOAL
        stars > before.stars -> Event.STAR
        hops > before.hops -> Event.HOP
        else -> null
    }
