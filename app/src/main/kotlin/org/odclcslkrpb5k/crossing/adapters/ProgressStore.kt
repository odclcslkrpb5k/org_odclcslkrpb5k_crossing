package org.odclcslkrpb5k.crossing.adapters

import android.util.AtomicFile
import android.util.Log
import androidx.core.util.tryWrite
import java.io.File
import java.io.IOException
import org.odclcslkrpb5k.crossing.core.Outcome
import org.odclcslkrpb5k.crossing.core.Progress
import org.odclcslkrpb5k.crossing.core.ProgressCodec

/**
 * Her progress on disk (ER-3: the only code that touches the file), in `progress/`, which Auto
 * Backup carries. Written atomically: a write finishes or leaves the old file. Missing or
 * unreadable progress starts from the beginning: losing a few stars beats a game that will not
 * open.
 */
class ProgressStore(filesDir: File) {
    private val file = AtomicFile(File(File(filesDir, "progress"), "progress.bin"))

    /** Saved progress, or a fresh start when there is none or it does not read back (logged). */
    fun load(): Progress {
        if (!file.baseFile.exists()) return Progress()
        val bytes =
            try {
                file.readFully()
            } catch (e: IOException) {
                Log.w(TAG, "progress unreadable: ${e.javaClass.simpleName}")
                return Progress()
            }
        return when (val p = ProgressCodec.decode(bytes)) {
            is Outcome.Ok -> p.value
            is Outcome.Err -> Progress().also { Log.w(TAG, "progress does not decode: ${p.error}") }
        }
    }

    fun save(progress: Progress): Outcome<Unit, String> =
        try {
            val dir = file.baseFile.parentFile
            if (dir != null && !dir.isDirectory && !dir.mkdirs()) throw IOException("mkdirs")
            file.tryWrite { it.write(ProgressCodec.encode(progress)) }
            Outcome.Ok(Unit)
        } catch (e: IOException) {
            Log.w(TAG, "progress not saved: ${e.javaClass.simpleName}")
            Outcome.Err(e.javaClass.simpleName)
        }

    private companion object {
        const val TAG = "ProgressStore"
    }
}
