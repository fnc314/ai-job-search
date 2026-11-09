package dev.fnc314.tools.jobtracker.cli.db

import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.cloud.FirestoreClient
import dev.fnc314.tools.jobtracker.core.data.platform.RemotePlatform

public object FirestoreProvider {
    public val firestore: Firestore by lazy {
        RemotePlatform.initialize()
        val app = FirebaseApp.getInstance()
        FirestoreClient.getFirestore(app)
    }

    public fun isWritesAllowed(): Boolean {
        return System.getenv("TRACKER_ALLOW_WRITES") == "1"
    }

    public fun assertWritesAllowed(action: String) {
        if (!isWritesAllowed()) {
            throw IllegalStateException("TRACKER_ALLOW_WRITES=1 is required to perform $action.")
        }
    }
}
