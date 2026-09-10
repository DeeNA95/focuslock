package com.focuslock.domain.session

import com.focuslock.domain.model.SessionStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionStateMachineTest {

    @Test
    fun `idle transitions to activating`() {
        assertThat(
            SessionStateMachine.canTransition(SessionStatus.IDLE, SessionStatus.ACTIVATING)
        ).isTrue()
    }

    @Test
    fun `activating transitions to active`() {
        assertThat(
            SessionStateMachine.canTransition(SessionStatus.ACTIVATING, SessionStatus.ACTIVE)
        ).isTrue()
    }

    @Test
    fun `activating transitions to failed`() {
        assertThat(
            SessionStateMachine.canTransition(SessionStatus.ACTIVATING, SessionStatus.FAILED)
        ).isTrue()
    }

    @Test
    fun `active transitions to expiring`() {
        assertThat(
            SessionStateMachine.canTransition(SessionStatus.ACTIVE, SessionStatus.EXPIRING)
        ).isTrue()
    }

    @Test
    fun `expiring transitions to completed`() {
        assertThat(
            SessionStateMachine.canTransition(SessionStatus.EXPIRING, SessionStatus.COMPLETED)
        ).isTrue()
    }

    @Test
    fun `active cannot be cancelled`() {
        assertThat(
            SessionStateMachine.canTransition(SessionStatus.ACTIVE, SessionStatus.COMPLETED)
        ).isFalse()
        assertThat(
            SessionStateMachine.canTransition(SessionStatus.ACTIVE, SessionStatus.IDLE)
        ).isFalse()
    }

    @Test
    fun `terminal states are terminal`() {
        assertThat(SessionStateMachine.isTerminal(SessionStatus.COMPLETED)).isTrue()
        assertThat(SessionStateMachine.isTerminal(SessionStatus.FAILED)).isTrue()
        assertThat(SessionStateMachine.isTerminal(SessionStatus.ACTIVE)).isFalse()
    }

    @Test
    fun `completed cannot transition anywhere`() {
        assertThat(
            SessionStateMachine.canTransition(SessionStatus.COMPLETED, SessionStatus.ACTIVE)
        ).isFalse()
    }

    @Test
    fun `expiring cannot go back to activating`() {
        assertThat(
            SessionStateMachine.canTransition(SessionStatus.EXPIRING, SessionStatus.ACTIVATING)
        ).isFalse()
    }
}
