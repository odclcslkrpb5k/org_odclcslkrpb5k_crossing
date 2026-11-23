package org.odclcslkrpb5k.crossing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.withClip
import kotlin.math.sin
import org.odclcslkrpb5k.crossing.core.Board
import org.odclcslkrpb5k.crossing.core.Ground
import org.odclcslkrpb5k.crossing.core.Sprite
import org.odclcslkrpb5k.crossing.core.Sprites
import org.odclcslkrpb5k.crossing.core.World

/**
 * Draws a [World] onto the screen: the level's background, its stars, carts and logs, and the foal
 * at [shownX], [shownRow] (the screen eases her between cells; the world only knows cells). Pixel
 * art is scaled without smoothing, so it stays crisp. Main thread or the render thread only.
 */
class Painter {
    private val sprites: Map<Sprite, Bitmap> = Sprites.ALL.associateWith(::bitmap)
    private val crisp = Paint().apply { isFilterBitmap = false }
    private val text =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(SHADOW, 0f, 0f, Color.BLACK)
        }
    private val mirror = Matrix()
    private val rect = RectF()
    private var background: Bitmap? = null

    /** Seconds past the world's last tick, during [draw]: movers are drawn there. */
    private var between = 0.0

    /** The canvas being drawn on, only during [draw]. */
    private var target: Canvas? = null
    private var backgroundOf = 0
    private var cell = 1f
    private var left = 0f
    private var top = 0f

    /** Fits the board into a [width] by [height] screen, centred, in whole square cells. */
    fun layout(width: Int, height: Int) {
        cell = minOf(width / Board.COLS, height / Board.ROWS).coerceAtLeast(1).toFloat()
        left = (width - cell * Board.COLS) / 2
        top = (height - cell * Board.ROWS) / 2
        background = null
        text.textSize = cell * TEXT
    }

    fun draw(canvas: Canvas, world: World, frame: Frame) {
        this.between = frame.between
        target = canvas
        canvas.drawColor(Color.BLACK)
        val bg =
            background?.takeIf { backgroundOf == world.level.number }
                ?: Background.draw(world.level, cell.toInt()).also {
                    background = it
                    backgroundOf = world.level.number
                }
        canvas.drawBitmap(bg, left, top, crisp)
        canvas.withClip(left, top, left + cell * Board.COLS, top + cell * Board.ROWS) {
            playfield(world, frame)
        }
        put(Sprites.STAR, INSET, INSET, STAR_BADGE)
        canvas.drawText(
            "${world.stars}",
            left + cell * (STAR_BADGE + INSET),
            top + cell * TEXT_BASE,
            text,
        )
        target = null
    }

    /** Stars, movers, and the foal (dizzy stars round her head after a bonk), inside the board. */
    private fun playfield(world: World, frame: Frame) {
        val shownX = frame.shownX
        val shownRow = frame.shownRow
        val bob = sin(world.seconds * BOB_SPEED).toFloat() * cell * BOB
        for ((row, col) in world.level.stars) {
            if (row !in world.taken)
                put(Sprites.STAR, col + INSET, row + INSET + bob / cell, 1 - 2 * INSET)
        }
        movers(world)
        put(Sprites.FOAL, shownX - HALF, shownRow, 1f)
        if (world.dizzy) {
            val spin = world.seconds * SPIN
            for (i in 0 until DIZZY_STARS) {
                val a = spin + i * 2 * Math.PI / DIZZY_STARS
                val x = shownX + (kotlin.math.cos(a) * DIZZY_RADIUS).toFloat() - DIZZY_SIZE / 2
                val y = shownRow - DIZZY_SIZE / 2 + (sin(a) * DIZZY_RADIUS / 2).toFloat()
                put(Sprites.STAR, x, y, DIZZY_SIZE)
            }
        }
        if (frame.celebrate > 0f) burst(shownX, shownRow + HALF, frame.celebrate)
    }

    /** At the rainbow: a ring of stars flies out from her and off the edge of the board. */
    private fun burst(x: Float, y: Float, progress: Float) {
        val radius = progress * BURST_RADIUS
        for (i in 0 until BURST_STARS) {
            val a = i * 2 * Math.PI / BURST_STARS + progress
            val sx = x + (kotlin.math.cos(a) * radius).toFloat() - BURST_SIZE / 2
            val sy = y + (sin(a) * radius).toFloat() - BURST_SIZE / 2
            put(Sprites.STAR, sx, sy, BURST_SIZE)
        }
    }

    private fun movers(world: World) {
        world.level.lanes.forEachIndexed { row, lane ->
            val sprite =
                when (lane.ground) {
                    Ground.ROAD -> Sprites.CART
                    Ground.STREAM -> Sprites.LOG
                    Ground.GRASS,
                    Ground.GOAL -> null
                }
            if (sprite != null) {
                for (x in lane.movers(world.seconds + between)) {
                    put(sprite, x, row.toFloat(), lane.length, flip = lane.speed < 0)
                }
            }
        }
    }

    /** Draws [sprite] with its top-left at cell ([x], [y]), [cells] wide, scaled to its aspect. */
    private fun put(sprite: Sprite, x: Float, y: Float, cells: Float, flip: Boolean = false) {
        val canvas = target ?: return
        val bitmap = sprites.getValue(sprite)
        val w = cells * cell
        val h = w * sprite.height / sprite.width
        val px = left + x * cell
        val py = top + y * cell + (cells.coerceAtMost(1f) * cell - h).coerceAtLeast(0f) / 2
        rect.set(px, py, px + w, py + h)
        if (!flip) {
            canvas.drawBitmap(bitmap, null, rect, crisp)
            return
        }
        mirror.setScale(-w / bitmap.width, h / bitmap.height)
        mirror.postTranslate(px + w, py)
        canvas.drawBitmap(bitmap, mirror, crisp)
    }

    private fun bitmap(sprite: Sprite): Bitmap =
        Bitmap.createBitmap(sprite.pixels(), sprite.width, sprite.height, Bitmap.Config.ARGB_8888)

    private companion object {
        const val HALF = 0.5f
        const val INSET = 0.15f
        const val BOB = 0.05f
        const val BOB_SPEED = 3.0
        const val SPIN = 4.0
        const val DIZZY_STARS = 3
        const val DIZZY_RADIUS = 0.45
        const val DIZZY_SIZE = 0.3f
        const val STAR_BADGE = 0.7f
        const val TEXT = 0.6f
        const val TEXT_BASE = 0.75f
        const val SHADOW = 6f
        const val BURST_STARS = 10
        const val BURST_RADIUS = 4f
        const val BURST_SIZE = 0.6f
    }
}

/** Where the screen shows things this frame, beyond the world itself. */
class Frame(
    /** The foal's shown centre, in cells, eased between cells. */
    val shownX: Float,
    val shownRow: Float,
    /** Seconds past the world's last tick: movers are drawn there. */
    val between: Double,
    /** How far through the celebration at the goal, 0 (none) to 1. */
    val celebrate: Float,
)
