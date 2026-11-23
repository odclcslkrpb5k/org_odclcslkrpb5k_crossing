package org.odclcslkrpb5k.crossing.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

private val arbProgress =
    Arb.bind(Arb.int(1, Progress.MAX_LEVEL), Arb.int(0, Progress.MAX_STARS)) { l, s ->
        Progress(l, s)
    }

class ProgressCodecTest :
    StringSpec({
        "progress decodes to the progress that was encoded" {
            checkAll(arbProgress) { p ->
                ProgressCodec.decode(ProgressCodec.encode(p)) shouldBe Outcome.Ok(p)
            }
        }

        "any bytes decode to progress or an error, never an exception" {
            checkAll(Arb.byteArray(Arb.int(0, 20), Arb.byte())) { bytes ->
                val result = ProgressCodec.decode(bytes)
                if (result is Outcome.Ok)
                    ProgressCodec.encode(result.value).toList() shouldBe bytes.toList()
            }
        }

        "a cut-short or lengthened file is refused" {
            checkAll(arbProgress, Arb.int(0, 11)) { p, keep ->
                val bytes = ProgressCodec.encode(p)
                ProgressCodec.decode(bytes.copyOf(keep)) shouldBe
                    Outcome.Err(ProgressError.WRONG_SIZE)
                ProgressCodec.decode(bytes + 0) shouldBe Outcome.Err(ProgressError.WRONG_SIZE)
            }
        }

        "another file, another version or a level of 0 is refused" {
            val bytes = ProgressCodec.encode(Progress(3, 7))
            ProgressCodec.decode(bytes.copyOf().also { it[0] = 0 }) shouldBe
                Outcome.Err(ProgressError.NOT_PROGRESS)
            ProgressCodec.decode(bytes.copyOf().also { it[4] = 2 }) shouldBe
                Outcome.Err(ProgressError.UNKNOWN_VERSION)
            ProgressCodec.decode(bytes.copyOf().also { it[6] = 0 }) shouldBe
                Outcome.Err(ProgressError.OUT_OF_RANGE)
            ProgressCodec.decode(bytes.copyOf().also { it[11] = -1 })
                .shouldBeInstanceOf<Outcome.Err<ProgressError>>()
        }
    })
