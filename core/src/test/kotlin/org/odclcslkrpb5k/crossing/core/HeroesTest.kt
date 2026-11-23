package org.odclcslkrpb5k.crossing.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

class HeroesTest :
    StringSpec({
        "the foal is free, and each later hero needs more stars than the one before" {
            Heroes.ALL.first().needs shouldBe 0
            Heroes.ALL.zipWithNext().forEach { (a, b) -> check(b.needs > a.needs) { "${b.name}" } }
            check(Heroes.ALL.size <= Progress.MAX_HERO + 1)
            check(Heroes.ALL.all { it.sprite in Sprites.ALL }) { "a hero without a checked sprite" }
        }

        "heroes keep the numbers progress is saved with" {
            Heroes.ALL.map { it.name } shouldBe
                listOf("foal", "bunny", "kitten", "owl", "dragon", "pegasus")
        }

        "a hero once unlocked stays unlocked as stars grow" {
            checkAll(Arb.int(0, 200), Arb.int(0, 200), Arb.int(0, Heroes.ALL.size - 1)) { s, more, h
                ->
                if (Heroes.unlocked(h, s)) check(Heroes.unlocked(h, s + more))
            }
        }

        "a locked or unknown choice plays the foal; an unlocked one plays itself" {
            checkAll(Arb.int(0, 200), Arb.int(-3, 300)) { stars, h ->
                val p = Heroes.playable(h, stars)
                if (Heroes.unlocked(h, stars)) p shouldBe h else p shouldBe 0
            }
        }

        "each unlock is announced once, as the stars pass it" {
            checkAll(Arb.int(0, 100), Arb.int(0, 100)) { a, b ->
                val (lo, hi) = minOf(a, b) to maxOf(a, b)
                val all = (lo until hi).flatMap { Heroes.newlyUnlocked(it, it + 1) }
                all shouldBe Heroes.newlyUnlocked(lo, hi)
                all.distinct() shouldBe all
            }
        }
    })
