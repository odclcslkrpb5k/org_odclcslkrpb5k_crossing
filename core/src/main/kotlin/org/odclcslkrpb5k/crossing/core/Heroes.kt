package org.odclcslkrpb5k.crossing.core

/** A character she can play as, once she has [needs] stars. */
class Hero(val name: String, val sprite: Sprite, val needs: Int)

/**
 * Every character, in the order they unlock: the foal from the start, then one more every few
 * levels (two stars a level). Stars are never lost, so a character once unlocked stays unlocked. A
 * hero's place in [ALL] is its saved number, so only append.
 */
object Heroes {
    val ALL: List<Hero> =
        listOf(
            Hero("foal", Sprites.FOAL, 0),
            Hero("bunny", Sprites.BUNNY, 6),
            Hero("kitten", Sprites.KITTEN, 14),
            Hero("owl", Sprites.OWL, 24),
            Hero("dragon", Sprites.DRAGON, 36),
            Hero("pegasus", Sprites.PEGASUS, 50),
        )

    fun unlocked(hero: Int, stars: Int): Boolean = hero in ALL.indices && stars >= ALL[hero].needs

    /** The hero to play: the one chosen if she has unlocked it, else the foal. */
    fun playable(hero: Int, stars: Int): Int = if (unlocked(hero, stars)) hero else 0

    /** The heroes that unlocked as her stars went from [before] to [after]. */
    fun newlyUnlocked(before: Int, after: Int): List<Int> =
        ALL.indices.filter { ALL[it].needs in (before + 1)..after }
}
