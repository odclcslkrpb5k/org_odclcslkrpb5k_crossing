package org.odclcslkrpb5k.crossing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import org.odclcslkrpb5k.crossing.core.Heroes
import org.odclcslkrpb5k.crossing.core.Sprite

/**
 * The character button (top right: who she is playing as) and the picker it opens: every hero, the
 * locked ones as dark shapes with the stars they need. A tap on an unlocked hero chooses it
 * ([chose]); a tap on a locked one does nothing. The button wears a gold ring when a hero has
 * unlocked since she last opened the picker.
 */
class HeroPicker(
    private val context: Context,
    private val opened: () -> Unit,
    private val chose: (Int) -> Unit,
    dismiss: () -> Unit,
) {
    private var hero = 0
    private var stars = 0
    private var fresh = false

    val button: ImageButton =
        ImageButton(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "characters"
            val pad = dp(PAD)
            setPadding(pad, pad, pad, pad)
            setOnClickListener { open() }
        }

    /**
     * Behind the panel, over the whole game: it takes every tap outside the panel (none reaches the
     * game underneath) and closes the picker, so a tap anywhere gets her back to playing.
     */
    val scrim: View =
        View(context).apply {
            setBackgroundColor(SCRIM)
            visibility = View.GONE
            contentDescription = "back to the game"
            setOnClickListener { dismiss.invoke() }
        }

    val panel: GridLayout =
        GridLayout(context).apply {
            columnCount = COLUMNS
            visibility = View.GONE
            isClickable = true
            val pad = dp(PAD * 2)
            setPadding(pad, pad, pad, pad)
            background = round(Color.WHITE, EDGE, dp(CORNER).toFloat())
        }

    val isOpen: Boolean
        get() = panel.isVisible

    /** Shows [hero] on the button; [unlockedNow] rings it gold until the picker is opened. */
    fun show(hero: Int, stars: Int, unlockedNow: Boolean = false) {
        this.hero = hero
        this.stars = stars
        if (unlockedNow) fresh = true
        button.setImageDrawable(crisp(Heroes.ALL[hero].sprite, dark = false))
        button.background =
            round(if (fresh) GOLD_FILL else Color.WHITE, if (fresh) GOLD else EDGE, -1f)
    }

    fun close() {
        panel.visibility = View.GONE
        scrim.visibility = View.GONE
    }

    private fun open() {
        fresh = false
        show(hero, stars)
        panel.removeAllViews()
        Heroes.ALL.forEachIndexed { i, h ->
            val unlocked = Heroes.unlocked(i, stars)
            val cell =
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    val pad = dp(PAD)
                    setPadding(pad, pad, pad, pad)
                    background =
                        if (i == hero) round(GOLD_FILL, GOLD, dp(CORNER).toFloat()) else null
                    contentDescription = if (unlocked) h.name else "${h.name}, ${h.needs} stars"
                    if (unlocked) setOnClickListener { chose(i) }
                }
            cell.addView(
                ImageView(context).apply { setImageDrawable(crisp(h.sprite, dark = !unlocked)) },
                LinearLayout.LayoutParams(dp(PORTRAIT), dp(PORTRAIT)),
            )
            cell.addView(
                TextView(context).apply {
                    text = if (unlocked) "" else "★ ${h.needs}"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
                    setTextColor(LOCKED_TEXT)
                    gravity = Gravity.CENTER
                }
            )
            panel.addView(cell)
        }
        scrim.visibility = View.VISIBLE
        panel.visibility = View.VISIBLE
        opened()
    }

    /** The sprite, scaled without smoothing; a locked hero is one dark shape. */
    private fun crisp(sprite: Sprite, dark: Boolean): BitmapDrawable {
        val pixels = sprite.pixels()
        if (dark) for (i in pixels.indices) if (pixels[i] != 0) pixels[i] = SHADOW
        val bitmap =
            Bitmap.createBitmap(pixels, sprite.width, sprite.height, Bitmap.Config.ARGB_8888)
        return bitmap.toDrawable(context.resources).apply { isFilterBitmap = false }
    }

    /** A filled shape with an edge: a circle when [corner] is negative. */
    private fun round(fill: Int, edge: Int, corner: Float) =
        GradientDrawable().apply {
            if (corner < 0) shape = GradientDrawable.OVAL else cornerRadius = corner
            setColor(fill)
            setStroke(dp(RING), edge)
        }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v.toFloat(),
                context.resources.displayMetrics,
            )
            .toInt()

    companion object {
        /** The character button's size, in dp. */
        const val BUTTON_DP: Int = 96
        private const val COLUMNS = 3
        private const val PAD = 8
        private const val PORTRAIT = 120
        private const val CORNER = 20
        private const val RING = 4
        private const val TEXT_SP = 22f
        private const val EDGE = 0xFFD9CCF5.toInt()
        private const val GOLD = 0xFFFFB300.toInt()
        private const val GOLD_FILL = 0xFFFFF3C4.toInt()
        private const val SHADOW = 0xFF616161.toInt()
        private const val SCRIM = 0x66000000
        private const val LOCKED_TEXT = 0xFF5D4037.toInt()
    }
}
