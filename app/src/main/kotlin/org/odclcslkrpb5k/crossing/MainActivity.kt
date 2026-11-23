package org.odclcslkrpb5k.crossing

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.odclcslkrpb5k.crossing.adapters.ProgressStore
import org.odclcslkrpb5k.crossing.core.Level
import org.odclcslkrpb5k.crossing.core.Outcome
import org.odclcslkrpb5k.crossing.core.Progress
import org.odclcslkrpb5k.crossing.core.World

/**
 * The game, full screen. Her progress is read on the disk thread before the first level shows, and
 * saved there when a level is finished and whenever the game leaves the screen.
 */
class MainActivity : Activity() {
    private val view by lazy { GameView(this) }
    private val store by lazy { ProgressStore(filesDir) }
    private var loaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(view)
        view.onLevel = { save(it) }
        disk.execute {
            val progress = store.load()
            main.post {
                if (!isDestroyed) {
                    view.start(
                        World(Level.make(progress.level, GameView.SEED), stars = progress.stars)
                    )
                    loaded = true
                    if (hasWindowFocus()) view.resume()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (loaded) view.resume()
    }

    override fun onPause() {
        super.onPause()
        view.pause()
        view.world?.let { save(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        view.release()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        window.insetsController?.let {
            it.hide(WindowInsets.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (loaded) view.resume()
    }

    /**
     * Saves the level she is on and the stars she had when it began: a level left half done is
     * played again from its start, so its stars must not count twice.
     */
    private fun save(world: World) {
        val progress =
            Progress(
                world.level.number.coerceAtMost(Progress.MAX_LEVEL),
                (world.stars - world.taken.size).coerceIn(0, Progress.MAX_STARS),
            )
        disk.execute {
            val saved = store.save(progress)
            if (saved is Outcome.Err) Log.w(TAG, "progress not saved: ${saved.error}")
        }
    }

    private companion object {
        const val TAG = "MainActivity"
        /** The one thread that reads and writes progress, in the order asked. */
        val disk: ExecutorService = Executors.newSingleThreadExecutor()
        val main = Handler(Looper.getMainLooper())
    }
}
