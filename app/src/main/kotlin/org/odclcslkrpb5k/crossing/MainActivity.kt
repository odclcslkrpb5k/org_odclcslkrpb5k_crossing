package org.odclcslkrpb5k.crossing

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.odclcslkrpb5k.crossing.adapters.ProgressStore
import org.odclcslkrpb5k.crossing.core.Heroes
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

    /** The character she chose (its place in Heroes.ALL), saved with her progress. */
    private var hero = 0

    /** Stars banked when the last level began, to tell which heroes a finished level unlocked. */
    private var previousBanked = 0
    private val picker by lazy {
        HeroPicker(
            this,
            opened = { pickerOpened() },
            chose = { choose(it) },
            dismiss = { closePicker() },
        )
    }
    private val backFromPicker = OnBackInvokedCallback { closePicker() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            FrameLayout(this).apply {
                addView(view)
                val size = dp(HeroPicker.BUTTON_DP)
                addView(
                    picker.button,
                    FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.END).apply {
                        setMargins(dp(MARGIN), dp(MARGIN), dp(MARGIN), dp(MARGIN))
                    },
                )
                addView(picker.scrim, FrameLayout.LayoutParams(MATCH, MATCH))
                addView(picker.panel, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
            }
        )
        // Nothing to choose until her progress is read.
        picker.button.isEnabled = false
        view.onLevel = { next ->
            save(next)
            val banked = next.stars
            val unlocked = Heroes.newlyUnlocked(previousBanked, banked).isNotEmpty()
            picker.show(hero, banked, unlockedNow = unlocked)
            previousBanked = banked
        }
        disk.execute {
            val progress = store.load()
            main.post {
                if (!isDestroyed) {
                    hero = Heroes.playable(progress.hero, progress.stars)
                    view.hero = Heroes.ALL[hero].sprite
                    previousBanked = progress.stars
                    picker.show(hero, progress.stars)
                    picker.button.isEnabled = true
                    view.start(
                        World(Level.make(progress.level, GameView.SEED), stars = progress.stars)
                    )
                    loaded = true
                    if (hasWindowFocus() && !picker.isOpen) view.resume()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (loaded && !picker.isOpen) view.resume()
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
        if (loaded && !picker.isOpen) view.resume()
    }

    /** She chose [chosen]: it plays from now, and is saved. */
    private fun choose(chosen: Int) {
        hero = chosen
        view.hero = Heroes.ALL[chosen].sprite
        closePicker()
        view.world?.let { save(it) }
    }

    /** The game waits while she chooses; Back closes the picker. */
    private fun pickerOpened() {
        view.pause()
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            backFromPicker,
        )
    }

    private fun closePicker() {
        onBackInvokedDispatcher.unregisterOnBackInvokedCallback(backFromPicker)
        picker.close()
        val w = view.world
        if (w != null) picker.show(hero, w.stars - w.taken.size)
        if (loaded && hasWindowFocus()) view.resume()
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
                hero,
            )
        disk.execute {
            val saved = store.save(progress)
            if (saved is Outcome.Err) Log.w(TAG, "progress not saved: ${saved.error}")
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "MainActivity"
        const val MARGIN = 12
        const val WRAP = FrameLayout.LayoutParams.WRAP_CONTENT
        const val MATCH = FrameLayout.LayoutParams.MATCH_PARENT
        /** The one thread that reads and writes progress, in the order asked. */
        val disk: ExecutorService = Executors.newSingleThreadExecutor()
        val main = Handler(Looper.getMainLooper())
    }
}
