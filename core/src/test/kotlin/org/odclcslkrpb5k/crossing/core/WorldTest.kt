package org.odclcslkrpb5k.crossing.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll

private val arbLevel = Arb.int(1, 40)
private val arbSeed = Arb.long()
private val arbMoves = Arb.list(Arb.enum<Move>().orNull(0.7), 0..2_000)

private fun play(world: World, moves: List<Move?>): List<World> =
    moves.runningFold(world) { w, m -> w.step(m) }

/**
 * A careful player with a child's limits: on any row it hops up only when playing the rest of the
 * way to the next safe row (grass or the goal), at a steady pace no faster than a hop every
 * [MIN_PACE_TICKS], can be seen in a copy of the future to keep a body [MARGIN] cells either side
 * of her centre clear of carts and wholly on a log. The world is a value, so the copy is exact; the
 * margin stands in for a child's imprecision, so a level that needs split-second timing fails.
 * Returns the world when finished, or null if the level was not crossed within [limit] ticks.
 */
private fun crossCarefully(start: World, limit: Long): World? {
    var w = start
    while (w.tick - start.tick < limit) {
        if (w.finished) return w
        // Plans are looked for every few ticks, as a child would wait and look, not every tick.
        val plan =
            if (w.tick % LOOK_EVERY == 0L) {
                (MIN_PACE_TICKS..MAX_PACE_TICKS).firstNotNullOfOrNull { pace -> safeRun(w, pace) }
            } else {
                null
            }
        w = plan ?: w.step(null)
    }
    return null
}

/** Hops up every [pace] ticks until on a safe row; the world there, or null if it bonked. */
private fun safeRun(from: World, pace: Int): World? {
    var w = from.step(Move.UP)
    var tick = 1
    while (w.bonks == from.bonks && w.comfortable() && !w.onSafeRow()) {
        w = w.step(if (tick % pace == 0) Move.UP else null)
        tick += 1
    }
    return w.takeIf { it.bonks == from.bonks && it.comfortable() }
}

/** Her whole body, not just its centre, is clear of carts on a road and on a log in a stream. */
private fun World.comfortable(): Boolean {
    val lane = level.lanes[player.row]
    val t = seconds
    val body = listOf(player.x - MARGIN, player.x, player.x + MARGIN)
    return when (lane.ground) {
        Ground.ROAD -> body.none { lane.covers(it, t) }
        Ground.STREAM -> body.all { lane.covers(it, t) }
        Ground.GRASS,
        Ground.GOAL -> true
    }
}

private fun World.onSafeRow(): Boolean =
    level.lanes[player.row].ground.let { it == Ground.GRASS || it == Ground.GOAL }

private const val LEVEL_SEED = 2026L
private const val MIN_PACE_TICKS = 12
private const val MAX_PACE_TICKS = 40
private const val MARGIN = 0.35f
private const val LOOK_EVERY = 6L
private const val FIVE_MINUTES = 5L * 60 * World.RATE

class WorldTest :
    StringSpec({
        "every level can be crossed without a single bonk by a careful player" {
            checkAll(60, arbLevel, arbSeed) { number, seed ->
                val crossed = crossCarefully(World(Level.make(number, seed)), FIVE_MINUTES)
                checkNotNull(crossed) { "level $number, seed $seed was not crossed" }
                crossed.bonks shouldBe 0
            }
        }

        "the same level and the same moves always play out the same way" {
            checkAll(50, arbLevel, arbSeed, arbMoves) { n, seed, moves ->
                play(World(Level.make(n, seed)), moves) shouldBe
                    play(World(Level.make(n, seed)), moves)
            }
        }

        "the character never leaves the board, whatever is tapped" {
            checkAll(50, arbLevel, arbSeed, arbMoves) { n, seed, moves ->
                for (w in play(World(Level.make(n, seed)), moves)) {
                    check(w.player.x in 0f..Board.COLS.toFloat()) { "x ${w.player.x}" }
                    check(w.player.row in 0..Board.START_ROW) { "row ${w.player.row}" }
                }
            }
        }

        "a bonk takes nothing: stars stay, and the character is back on grass it stood on" {
            checkAll(50, arbLevel, arbSeed, arbMoves) { n, seed, moves ->
                play(World(Level.make(n, seed)), moves).zipWithNext().forEach { (before, after) ->
                    check(after.stars >= before.stars)
                    if (after.bonks > before.bonks) {
                        after.stars shouldBe before.stars
                        after.player.row shouldBe before.checkpoint
                        after.level.lanes[after.player.row].ground shouldBe Ground.GRASS
                        after.dizzy shouldBe true
                    }
                }
            }
        }

        "each star is counted once, and only on its own cell" {
            checkAll(50, arbLevel, arbSeed, arbMoves) { n, seed, moves ->
                val worlds = play(World(Level.make(n, seed)), moves)
                val last = worlds.last()
                last.stars shouldBe last.taken.size
                check(last.taken.all { it in last.level.stars }) { "a star on a row without one" }
                worlds.zipWithNext().forEach { (before, after) ->
                    if (after.stars > before.stars) {
                        after.player.col shouldBe after.level.stars.getValue(after.player.row)
                    }
                }
            }
        }

        "while dizzy, hops are ignored" {
            checkAll(50, arbLevel, arbSeed, Arb.enum<Move>()) { n, seed, m ->
                val dizzy = World(Level.make(n, seed), dizzyUntil = 10)
                dizzy.step(m).player shouldBe dizzy.player
            }
        }

        "carts on a road always leave a gap to stand in" {
            checkAll(arbLevel, arbSeed, Arb.double(0.0, 600.0, includeNaNs = false)) { n, seed, t ->
                for (lane in Level.make(n, seed).lanes.filter { it.ground == Ground.ROAD }) {
                    val lefts = lane.movers(t).sorted()
                    val gaps =
                        lefts.zipWithNext { a, b -> b - a - lane.length } +
                            (lefts.first() + lane.loop - lefts.last() - lane.length)
                    check(gaps.all { it >= Level.MIN_ROAD_GAP - 1e-3f }) { "gaps $gaps" }
                }
            }
        }

        "logs are long enough to land on without fine timing" {
            checkAll(arbLevel, arbSeed) { n, seed ->
                for (lane in Level.make(n, seed).lanes.filter { it.ground == Ground.STREAM }) {
                    check(lane.length >= Level.MIN_LOG) { "a log of ${lane.length} cells" }
                    check(lane.starts.isNotEmpty()) { "a stream with no logs" }
                }
            }
        }

        "movers come round again after one loop" {
            checkAll(arbLevel, arbSeed, Arb.double(0.0, 600.0, includeNaNs = false)) { n, seed, t ->
                for (lane in Level.make(n, seed).lanes.filter { it.starts.isNotEmpty() }) {
                    val period = lane.loop / kotlin.math.abs(lane.speed).toDouble()
                    lane.movers(t).zip(lane.movers(t + period)).forEach { (a, b) ->
                        check(
                            kotlin.math.abs(a - b) < 1e-2f ||
                                kotlin.math.abs(kotlin.math.abs(a - b) - lane.loop) < 1e-2f
                        )
                    }
                    lane.movers(t).forEach {
                        check(it >= -lane.length && it < lane.loop - lane.length + 1e-3f)
                    }
                }
            }
        }

        "a star collected stays collected when a cart bonks her straight after" {
            val level = Level.make(1, LEVEL_SEED)
            val (row, col) = level.stars.entries.first { it.key == 6 }.toPair()
            // From the road just below the star's row, hop onto the star, then onto the road
            // above and wait there for a cart.
            var w = World(level, player = Player.at(col, row + 1)).step(Move.UP)
            w.stars shouldBe 1
            w.checkpoint shouldBe row
            w = w.step(Move.UP)
            var waited = 0
            while (w.bonks == 0 && waited < 60 * World.RATE) {
                w = w.step(null)
                waited += 1
            }
            w.bonks shouldBe 1
            w.stars shouldBe 1
            w.taken shouldBe setOf(row)
            w.player.row shouldBe row
        }

        "a finished level waits, and the next keeps every star" {
            val done = World(Level.make(1, 7), player = Player.at(3, 0), stars = 2)
            done.finished shouldBe true
            done.step(Move.DOWN).player shouldBe done.player
            val next = done.next(7)
            next.level.number shouldBe 2
            next.stars shouldBe 2
            next.player shouldBe Player.at(Board.START_COL, Board.START_ROW)
        }

        "a level that is not a playfield is refused" {
            shouldThrow<IllegalArgumentException> { Level.make(0, 1) }
            shouldThrow<IllegalArgumentException> {
                Level(1, List(Board.ROWS) { Lane(Ground.GRASS) }, emptyMap())
            }
            shouldThrow<IllegalArgumentException> {
                Lane(Ground.GRASS, starts = listOf(1f), length = 1f)
            }
        }
    })
