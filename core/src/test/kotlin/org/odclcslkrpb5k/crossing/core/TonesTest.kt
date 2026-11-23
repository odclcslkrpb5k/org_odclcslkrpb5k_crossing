package org.odclcslkrpb5k.crossing.core

import io.kotest.core.spec.style.StringSpec
import kotlin.math.abs

class TonesTest :
    StringSpec({
        "every sound is short, gentle, audible, and starts and ends at silence" {
            val peak = (Tones.PEAK * Short.MAX_VALUE).toInt() + 1
            for (t in Tones.ALL) {
                val s = t.samples
                check(s.size in Tones.RATE / 50..Tones.RATE) { "${t.name}: ${s.size} samples" }
                check(s.all { abs(it.toInt()) <= peak }) { "${t.name} is louder than its peak" }
                check(s.any { abs(it.toInt()) > peak / 2 }) { "${t.name} is nearly silent" }
                check(abs(s.first().toInt()) < peak / 20 && abs(s.last().toInt()) < peak / 20) {
                    "${t.name} would click"
                }
            }
        }

        "no sound jumps between samples, even where its notes meet" {
            val peak = Tones.PEAK * Short.MAX_VALUE
            for (t in Tones.ALL) {
                // A sine of at most 1.4 kHz at 22 kHz moves under 0.4 of its peak per sample.
                val worst = t.samples.toList().zipWithNext { a, b -> abs(a - b) }.max()
                check(worst < peak * 0.45) { "${t.name} jumps by $worst" }
            }
        }
    })
