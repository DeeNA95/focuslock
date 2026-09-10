# Chaos & Acceptance Testing

This document maps the acceptance tests from the plan to what is covered by
automated unit tests versus what requires a device/emulator.

## Automated (JVM) — always run

```bash
./gradlew testDebugUnitTest
```

| Concern | Coverage |
|---------|----------|
| Midnight-crossing activation windows, exact boundaries, day-of-week | `ActivationWindowTest`, `RuleEngineTest` |
| Wall-clock-forward does not shorten a session (same boot) | `SessionClockTest` |
| Reboot falls back to wall-clock expiry | `SessionClockTest` |
| Timezone change does not shorten a session | `SessionClockTest` |
| Session state machine (no early cancel) | `SessionStateMachineTest` |
| Profile edit/delete does not mutate an active session | `SessionManagerTest` |
| Second activation while active is rejected | `SessionManagerTest` |
| Unsafe block list rejected | `SafetyPolicyTest`, `SessionManagerTest`, `HardModePreflightTest` |
| Partial enforcement failure rolls back, never ACTIVE | `SessionManagerTest` |
| Reconciliation idempotence | `SessionReconcilerTest` |
| Expiry restores packages + temporary policies | `SessionReconcilerTest` |
| Interrupted activation cleanup | `SessionReconcilerTest` |
| Recovery key hashing / verification | `RecoveryKeyHasherTest` |
| Emergency release (invalid key / valid key / no session) | `EmergencyReleaseTest` |
| NFC URI parse / resolve | `NfcUriTest`, `TagResolverTest` |
| Room persistence (profiles, sessions, tags, events) | Robolectric DAO tests |

## Manual / device — see plan §40–§41

The following acceptance tests require a device or emulator and cannot be
verified by JVM tests. Run them in order:

1. `A` Tap valid tag inside window → session starts, apps blocked.
2. `B` Tap valid tag outside window → nothing locked, reason shown.
3. `C` Allowed app still works during session.
4. `G` Reboot halfway → session recovered, enforcement resumes.
5. `H` Session expires → every suspended package restored.
6. `K` Kill FocusLock during Device Owner session → apps stay blocked.
7. `L` Recovery procedure works.

### Soft-lock device test matrix (Phase 6)

- 1-minute and 5-minute Accessibility sessions.
- Reboot during a session.
- NFC activation from a closed app.
- Screen-off / Doze behavior.
- One UI launcher behavior.
- Battery optimization behavior (whitelist FocusLock).

### Hard-lock / Device Owner test matrix (Phase 8, emulator first)

- Provision a fresh emulator (see `docs/DEVICE_OWNER_SETUP.md`).
- Suspension + restoration round-trip.
- Reboot with active hard session.
- Fortress Mode: date/time locked, automatic time forced, FocusLock
  uninstall blocked, all restored on expiry.
- Update/replace a blocked package mid-session (reconciliation re-applies).

## Failure injection checklist

- Kill process during a session (soft + hard).
- Reboot repeatedly.
- Change timezone mid-session.
- Deny exact-alarm permission (falls back to inexact alarm).
- Disable NFC.
- Update a blocked app.
- Remove a blocked app.
- Force backend exceptions (already covered by unit tests).
- Force database failure.
- Session expires while the phone is asleep.
- Session expires during reboot.

Each scenario must end in a safe, deterministic state: apps restored or still
blocked according to the session's authoritative timestamp, with the event log
recording what happened.
