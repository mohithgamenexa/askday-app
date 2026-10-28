package app.askday.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

/** Holds everything in memory, saves it to a file on the phone, and tells the screens when it changes. */
object Store {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var file: File

    private val _db = MutableStateFlow(DB())
    val db: StateFlow<DB> = _db
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    /** Called after every change, so notifications can be rescheduled. */
    var onChange: ((DB) -> Unit)? = null

    fun init(context: Context) {
        if (::file.isInitialized) return
        file = File(context.filesDir, "askday.json")
        io.launch {
            val loaded = runCatching { json.decodeFromString<DB>(file.readText()) }.getOrNull()
            if (loaded != null) _db.value = loaded
            _ready.value = true
        }
    }

    /**
     * Changes the data. The block edits a copy; return false to discard it when nothing changed.
     * An [AskdayError] thrown inside leaves everything as it was.
     */
    @Synchronized
    fun update(block: (DB) -> Boolean?): Boolean {
        val draft = json.decodeFromString<DB>(json.encodeToString(DB.serializer(), _db.value))
        if (block(draft) == false) return false
        _db.value = draft
        val text = json.encodeToString(DB.serializer(), draft)
        io.launch { runCatching { file.writeText(text) } }
        onChange?.invoke(draft)
        return true
    }

    /** Logging out of a guest account erases everything on this phone. */
    fun wipe() {
        _db.value = DB()
        io.launch { runCatching { file.delete() } }
        onChange?.invoke(_db.value)
    }
}

/** Runs an engine action and turns problems into a sentence for the screen (null means it worked). */
fun act(block: (DB) -> Unit): String? = try {
    Store.update { block(it); true }
    null
} catch (e: AskdayError) {
    e.message
} catch (e: Exception) {
    app.askday.services.Services.recordError(e)
    "Something went wrong. Try again."
}
