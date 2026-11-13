package dev.fnc314.tools.jobtracker.core.data.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public actual object PlatformFileSaver {
    public actual suspend fun saveText(content: String, filename: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            // JS DOM interop for Blob/URL download is complex; fall back to failure for now
            Result.failure(UnsupportedOperationException("File save not yet supported on JS"))
        }
}
