package org.odclcslkrpb5k.crossing.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SpritesTest :
    StringSpec({
        "every sprite is a rectangle of known colors, with something to see" {
            for (s in Sprites.ALL) {
                check(s.height > 0 && s.width > 0) { "${s.name} is empty" }
                check(s.rows.all { it.length == s.width }) {
                    "${s.name} has rows of different widths"
                }
                val unknown = s.rows.joinToString("").toSet() - Sprite.PALETTE.keys
                check(unknown.isEmpty()) { "${s.name} uses $unknown" }
                check(s.pixels().any { it != 0 }) { "${s.name} is all clear" }
            }
        }

        "pixels follow the grid row by row" {
            val s = Sprite("t", listOf("K.", ".W"))
            s.pixels().toList() shouldBe
                listOf(Sprite.PALETTE.getValue('K'), 0, 0, Sprite.PALETTE.getValue('W'))
        }
    })
