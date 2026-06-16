package com.tzh.sme.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncStatusTracker @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Emits true if there are pending writes to the server (offline data waiting to sync).
     * Note: This checks a specific reference. For a global check, we can listen to a common collection.
     */
    fun isSyncing(userId: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                // hasPendingWrites is true if the document has local changes not yet synced
                trySend(snapshot?.metadata?.hasPendingWrites() ?: false)
            }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()
}
