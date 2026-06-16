package com.tzh.sme.domain.usecase.pos

import com.tzh.sme.data.repository.SyncStatusTracker
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSyncStatusUseCase @Inject constructor(
    private val syncStatusTracker: SyncStatusTracker
) {
    operator fun invoke(userId: String): Flow<Boolean> {
        return syncStatusTracker.isSyncing(userId)
    }
}
