package org.odclcslkrpb5k.crossing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import kotlin.random.Random
import org.odclcslkrpb5k.crossing.core.Board
import org.odclcslkrpb5k.crossing.core.Ground
import org.odclcslkrpb5k.crossing.core.Level

/**
 * A level's rows drawn once into a bitmap the size of the board, [cell] pixels a cell, so each
 * frame only copies it: grass with tufts, roads with dashes, streams with ripples, and the goal as
 * a rainbow band. The decoration comes from the level number, so a level always looks the same.
 */
object Background {
    fun draw(level: Level, cell: Int): Bitmap {
        val bitmap = createBitmap(Board.COLS * cell, Board.ROWS * cell)
        val rows =
            Rows(
                Canvas(bitmap),
                Paint(),
                Random(level.number),
                cell.toFloat(),
                bitmap.width.toFloat(),
            )
        level.lanes.forEachIndexed { row, lane ->
            val top = row * cell.toFloat()
            when (lane.ground) {
                Ground.GOAL -> rows.goal(top)
                Ground.GRASS -> rows.grass(top)
                Ground.ROAD -> rows.road(top)
                Ground.STREAM -> rows.stream(top)
            }
        }
        return bitmap
    }

    private class Rows(
        val canvas: Canvas,
        val paint: Paint,
        val random: Random,
        val cell: Float,
        val width: Float,
    ) {
        fun goal(top: Float) {
            val band = cell / RAINBOW.size
            RAINBOW.forEachIndexed { i, color ->
                paint.color = color
                canvas.drawRect(0f, top + i * band, width, top + (i + 1) * band, paint)
            }
        }

        fun grass(top: Float) {
            fill(top, GRASS)
            paint.color = TUFT
            val size = cell / TUFT_SIZE
            repeat(Board.COLS * TUFTS_PER_CELL) {
                val x = random.nextFloat() * width
                val y = top + random.nextFloat() * (cell - size)
                canvas.drawRect(x, y, x + size, y + size, paint)
            }
        }

        fun road(top: Float) {
            fill(top, ROAD)
            paint.color = DASH
            val y = top + cell / 2f
            for (col in 0 until Board.COLS) {
                val x = col * cell + cell / DASH_INSET
                canvas.drawRect(
                    x,
                    y - cell / DASH_THICK,
                    x + cell / 2f,
                    y + cell / DASH_THICK,
                    paint,
                )
            }
        }

        fun stream(top: Float) {
            fill(top, WATER)
            paint.color = RIPPLE
            repeat(Board.COLS * RIPPLES_PER_CELL) {
                val x = random.nextFloat() * width
                val y = top + random.nextFloat() * cell
                canvas.drawRect(x, y, x + cell / RIPPLE_LONG, y + cell / RIPPLE_THICK, paint)
            }
        }

        private fun fill(top: Float, color: Int) {
            paint.color = color
            canvas.drawRect(0f, top, width, top + cell, paint)
        }
    }

    private val RAINBOW =
        listOf(
            0xFFE53935.toInt(),
            0xFFFF8F00.toInt(),
            0xFFFDD835.toInt(),
            0xFF43A047.toInt(),
            0xFF1E88E5.toInt(),
            0xFF7A4BE0.toInt(),
        )
    private const val GRASS = 0xFF8BC34A.toInt()
    private const val TUFT = 0xFF689F38.toInt()
    private const val ROAD = 0xFFBCAAA4.toInt()
    private const val DASH = 0xFFFFFFFF.toInt()
    private const val WATER = 0xFF4FC3F7.toInt()
    private const val RIPPLE = 0xFFB3E5FC.toInt()
    private const val TUFTS_PER_CELL = 3
    private const val TUFT_SIZE = 12f
    private const val DASH_INSET = 4f
    private const val DASH_THICK = 24f
    private const val RIPPLES_PER_CELL = 2
    private const val RIPPLE_LONG = 3f
    private const val RIPPLE_THICK = 20f
}
