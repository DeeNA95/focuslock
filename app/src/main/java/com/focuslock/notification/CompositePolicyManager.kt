package com.focuslock.notification

import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.session.DndPolicy
import com.focuslock.domain.session.TemporaryPolicyManager
import com.focuslock.enforcement.deviceowner.FortressPolicyManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Combines the two kinds of temporary session policy — Do Not Disturb (works
 * for soft and hard sessions) and Fortress Mode (hard-only device policies) —
 * behind the single [TemporaryPolicyManager] the session layer already uses.
 */
@Singleton
class CompositePolicyManager @Inject constructor(
    private val fortress: FortressPolicyManager,
    private val dnd: DndPolicy,
) : TemporaryPolicyManager {

    override suspend fun apply(session: FocusSession) {
        dnd.apply(session)
        fortress.apply(session)
    }

    override suspend fun restore(session: FocusSession) {
        dnd.restore(session)
        fortress.restore(session)
    }

    override suspend fun restoreAll() {
        dnd.restoreAll()
        fortress.restoreAll()
    }
}
