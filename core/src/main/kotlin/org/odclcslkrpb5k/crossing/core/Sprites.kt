package org.odclcslkrpb5k.crossing.core

/**
 * A picture as rows of characters, one per pixel, each looked up in [PALETTE] ('.' is clear). The
 * game's art is these grids and nothing else: original by construction, tiny, and checked by a test
 * (every row the same width, every character a color).
 */
class Sprite(val name: String, val rows: List<String>) {
    val width: Int = rows.firstOrNull()?.length ?: 0
    val height: Int = rows.size

    /** ARGB per pixel, row by row. */
    fun pixels(): IntArray =
        IntArray(width * height) { i -> PALETTE.getValue(rows[i / width][i % width]) }

    companion object {
        /** The colors, by character: soft and bright, for a six-year-old. */
        val PALETTE: Map<Char, Int> =
            mapOf(
                '.' to 0x00000000,
                'K' to 0xFF4527A0.toInt(), // outline, deep purple
                'W' to 0xFFFFFFFF.toInt(),
                'E' to 0xFF3E2723.toInt(), // eye
                'P' to 0xFFF06292.toInt(), // pink
                'V' to 0xFF9575CD.toInt(), // violet
                'Y' to 0xFFFDD835.toInt(), // gold
                'O' to 0xFFF57F17.toInt(), // star edge
                'C' to 0xFFF8BBD0.toInt(), // cheek
                'R' to 0xFFE53935.toInt(), // cart red
                'r' to 0xFFEF9A9A.toInt(), // cart panel
                'G' to 0xFF8D6E63.toInt(), // wheel
                'B' to 0xFF8D6E63.toInt(), // log bark
                'b' to 0xFF6D4C41.toInt(), // bark grain
                'T' to 0xFFD7B98E.toInt(), // log end
            )
    }
}

/** Every picture in the game. */
object Sprites {
    /** The character: a unicorn foal, side on, facing right. */
    val FOAL =
        Sprite(
            "foal",
            listOf(
                "..........Y.....",
                ".........YY.....",
                "...PPV..KWWK....",
                "..PPVVKKWWWWK...",
                "..PVV.KWWWEWWK..",
                ".PPV..KWWWWWWWK.",
                ".PV...KWWWWWCWK.",
                ".P....KWWWKKKK..",
                ".....KWWWWK.....",
                "....KWWWWWWK....",
                "...KWWWWWWWWK...",
                "..PKWWWWWWWWK...",
                ".PPKWWKKKKWWK...",
                "..P.KWK..KWK....",
                "....KWK..KWK....",
                "....KKK..KKK....",
            ),
        )

    /** A hay cart, two cells long, rolling right (drawn mirrored to roll left). */
    val CART =
        Sprite(
            "cart",
            listOf(
                "....YYYYYYYYYYYYYYYY....",
                "..YYYYYYYYYYYYYYYYYYYY..",
                ".RRRRRRRRRRRRRRRRRRRRRR.",
                ".RrrrrrrrrrrrrrrrrrrrrR.",
                ".RrrrrrrrrrrrrrrrrrrrrR.",
                ".RRRRRRRRRRRRRRRRRRRRRR.",
                "...KKK............KKK...",
                "..KGGGK..........KGGGK..",
                "..KGKGK..........KGKGK..",
                "..KGGGK..........KGGGK..",
                "...KKK............KKK...",
                "........................",
            ),
        )

    /** A floating log, four cells long. */
    val LOG =
        Sprite(
            "log",
            listOf(
                "..BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB..",
                ".BBbBBBBBBbbBBBBBBBBBBBbBBBBBBBBBbbBBBBBBBBBBBT.",
                "TBBBBBBbBBBBBBBBBbBBBBBBBBBBbBBBBBBBBBBBbBBBBTTT",
                "TTBBBBBBBBBBBBbBBBBBBBBBbbBBBBBBBBBBBbBBBBBBBTbT",
                "TbBBBbbBBBBBBBBBBBBBbBBBBBBBBBBbBBBBBBBBBBBBBTTT",
                "TTBBBBBBBBBBbBBBBBBBBBBBBBBBBBBBBBBBBBbbBBBBBBT.",
                ".BBBBBBBBBBBBBBBbbBBBBBBBBBbBBBBBBBBBBBBBBBBBB..",
                "..BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB..",
            ),
        )

    /** A star to collect. */
    val STAR =
        Sprite(
            "star",
            listOf(
                ".....OO.....",
                ".....YY.....",
                "....OYYO....",
                "OOOOYYYYOOOO",
                ".OYYYYYYYYO.",
                "..OYYYYYYO..",
                "...OYYYYO...",
                "..OYYYYYYO..",
                "..OYYOOYYO..",
                ".OYYO..OYYO.",
                ".OYO....OYO.",
                ".OO......OO.",
            ),
        )

    val ALL: List<Sprite> = listOf(FOAL, CART, LOG, STAR)
}
