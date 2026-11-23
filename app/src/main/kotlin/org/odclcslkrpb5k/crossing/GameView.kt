package org.odclcslkrpb5k.crossing

import android.content.Context
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.math.hypot
import org.odclcslkrpb5k.crossing.core.Ground
import org.odclcslkrpb5k.crossing.core.Move
import org.odclcslkrpb5k.crossing.core.World

/**
 * The playfield. Each display frame (Choreographer) runs as many fixed world ticks as the time
 * since the last frame holds, then draws once, so the game runs at the same speed at 60 or 90 Hz. A
 * tap hops forward; a swipe hops that way. A finished level is celebrated for a moment, then the
 * next one starts and [onLevel] hears of it (to save her progress).
 */
class GameView(context: Context) :
    SurfaceView(context), SurfaceHolder.Callback, Choreographer.FrameCallback {
    private val painter = Painter()
    private val moves = ArrayDeque<Move>()
    private val slop = context.resources.displayMetrics.density * SLOP_DP
    private var surface = false
    private var running = false
    private var lastNanos = 0L
    private var owed = 0L
    private var downX = 0f
    private var downY = 0f
    private var shownX = 0f
    private var shownRow = 0f
    private var bonksShown = 0
    private var celebrated = 0L
    private var multi = false
    private var levelShown = 0

    private var current: World? = null

    /** The world being played, as of the last frame. */
    val world: World?
        get() = current

    /** Starts playing [world], with the foal shown where it stands. */
    fun start(world: World) {
        current = world
        snap(world)
    }

    var onLevel: (World) -> Unit = {}

    init {
        holder.addCallback(this)
    }

    fun resume() {
        running = true
        lastNanos = 0L
        if (surface) schedule()
    }

    /** Asks for the next frame, once: resume and a new surface both ask, and must not double. */
    private fun schedule() {
        Choreographer.getInstance().removeFrameCallback(this)
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun pause() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surface = true
        if (running) schedule()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        painter.layout(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surface = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        val w = current
        if (!running || !surface || w == null) return
        val elapsed = if (lastNanos == 0L) 0L else frameTimeNanos - lastNanos
        lastNanos = frameTimeNanos
        // A long stall (the app was in the background) is not played back as a burst of ticks.
        owed = (owed + elapsed).coerceAtMost(MAX_OWED_NANOS)
        var next: World = w
        while (owed >= TICK_NANOS) {
            owed -= TICK_NANOS
            next = tick(next)
        }
        current = next
        ease(next, elapsed)
        val canvas = holder.lockHardwareCanvas()
        if (canvas != null) {
            painter.draw(canvas, next, shownX, shownRow, between())
            holder.unlockCanvasAndPost(canvas)
        }
        schedule()
    }

    /** How far, in seconds, the screen is past the last tick: movers are drawn there, smoothly. */
    private fun between(): Double = owed / NANOS_PER_SECOND

    private fun tick(w: World): World {
        if (!w.finished) return w.step(moves.removeFirstOrNull())
        if (celebrated == 0L) celebrated = w.tick
        if (w.tick - celebrated < CELEBRATE_TICKS) return w.step(null)
        celebrated = 0L
        moves.clear()
        return w.next(SEED).also(onLevel)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                multi = false
            }
            // A second finger makes the touch no hop at all: a palm or a pinch is not a swipe.
            MotionEvent.ACTION_POINTER_DOWN -> multi = true
            MotionEvent.ACTION_UP ->
                if (!multi) {
                    performClick()
                    val dx = event.x - downX
                    val dy = event.y - downY
                    val move =
                        when {
                            hypot(dx, dy) < slop -> Move.UP
                            abs(dx) > abs(dy) -> if (dx > 0) Move.RIGHT else Move.LEFT
                            else -> if (dy > 0) Move.DOWN else Move.UP
                        }
                    // A queue of two: a quick double tap hops twice; mashing does not pile up hops.
                    if (moves.size < MAX_QUEUED) moves.addLast(move)
                }
            else -> Unit
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    /**
     * The foal eases towards her cell on land, rides a log exactly, and jumps back after a bonk.
     */
    private fun ease(w: World, elapsedNanos: Long) {
        if (w.bonks != bonksShown || w.tick == 0L || w.level.number != levelShown) {
            snap(w)
            return
        }
        val k = (elapsedNanos / NANOS_PER_SECOND * EASE).toFloat().coerceIn(0f, 1f)
        val onStream = w.level.lanes[w.player.row].ground == Ground.STREAM
        shownRow += (w.player.row - shownRow) * k
        val lane = w.level.lanes[w.player.row]
        shownX =
            if (onStream && abs(shownRow - w.player.row) < HALF) {
                w.player.x + lane.speed * between().toFloat()
            } else {
                shownX + (w.player.x - shownX) * k
            }
    }

    private fun snap(w: World) {
        levelShown = w.level.number
        shownX = w.player.x
        shownRow = w.player.row.toFloat()
        bonksShown = w.bonks
    }

    companion object {
        /** Every level is made from this seed, so level n is always the same level. */
        const val SEED: Long = 2026L
        private const val NANOS_PER_SECOND = 1e9
        private const val TICK_NANOS = (NANOS_PER_SECOND / World.RATE).toLong()
        private const val MAX_OWED_NANOS = TICK_NANOS * 6
        private const val CELEBRATE_TICKS = World.RATE * 2L
        private const val SLOP_DP = 24f
        private const val MAX_QUEUED = 2
        private const val EASE = 18.0
        private const val HALF = 0.5f
    }
}
