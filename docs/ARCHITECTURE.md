# FocusLock Architecture

## Overview

FocusLock is a local, deterministic Android app. It turns a physical NFC tag
into an activation device for an *immutable* Focus Session that blocks a set of
distracting apps for a fixed duration.

The single most important architectural rule is:

> **The focus/session logic must not know how applications are blocked.**

Everything above the `EnforcementBackend` abstraction is pure, backend-agnostic
Kotlin. We can build the entire product on the Accessibility backend and later
switch to Device Owner without rewriting the rule engine, database, NFC system,
session logic, or UI.

## Data flow

```text
NFC tag
   ↓
NfcIntentParser          (extract token from NDEF)
   ↓
TagResolver              (token -> TagBinding -> FocusProfile)
   ↓
RuleEngine               (can this profile activate right now?)
   ↓
SafetyPolicy             (is the block list safe?)
   ↓
SessionManager           (create immutable snapshot, drive state machine)
   ↓
SessionRepository        (Room: durable source of truth)
   ↓
EnforcementBackend       (abstract; does the actual blocking)
   ↓
Android platform
```

## Backend abstraction

```text
                  ┌─ AccessibilityEnforcementBackend (soft)
SessionManager ───┤
                  └─ DeviceOwnerEnforcementBackend  (hard)
```

```kotlin
interface EnforcementBackend {
    suspend fun suspendPackages(packages: Set<String>): EnforcementResult
    suspend fun resumePackages(packages: Set<String>): EnforcementResult
    fun suspendability(packageName: String): Suspendability
    fun isAvailable(): Boolean
    fun backendType(): EnforcementBackendType
}
```

## Domain model

### Timekeeping

* `TimeAuthority` is the single clock abstraction. All business logic injects
  it; nothing calls the system clock directly.
* Same-boot expiry uses `SystemClock.elapsedRealtime()` (monotonic). Changing
  the wall clock forward cannot shorten an active session.
* `startedAtWallClock` / `expiresAtWallClock` are retained for UI, diagnostics,
  and reboot recovery.
* After a reboot, `SessionClock` detects the elapsed-clock reset and falls back
  to the wall-clock expiry snapshot.

### Profiles vs Sessions

* `FocusProfile` is a template (duration, blocked packages, activation windows,
  enforcement mode, fortress flag).
* `FocusSession` is a commitment. It snapshots every relevant field at
  activation time and is immutable afterwards. Editing or deleting the source
  profile never affects an active session.

### Session state machine

```text
IDLE
  → ACTIVATING
       → ACTIVE
       → FAILED
ACTIVE
  → EXPIRING
       → COMPLETED
```

There is deliberately no transition out of `ACTIVE` other than `EXPIRING`:
sessions cannot be cancelled early. `COMPLETED` and `FAILED` are terminal.

A session is never persisted as `ACTIVE` until enforcement has been verified.

## Reconciliation model

The database is the source of truth. The enforcement backend is reconciled
*toward* desired state, Kubernetes-style:

```text
desired state:  Instagram = suspended
actual state:   Instagram = not suspended
reconcile()
  → suspend Instagram
```

`SessionReconciler.reconcile()` is idempotent: running it five times produces
the same result as running it once. Triggers that call reconciliation include
app launch, boot, alarms, workers, NFC scans, accessibility events, and package
events.

## Package structure

```text
app/
├── data/
│   ├── db/
│   ├── dao/
│   ├── entity/
│   ├── repository/
│   └── datastore/
├── domain/
│   ├── model/
│   ├── rules/
│   ├── session/
│   ├── safety/
│   └── time/
├── enforcement/
│   ├── EnforcementBackend.kt
│   ├── accessibility/
│   └── deviceowner/
├── nfc/
├── scheduling/
├── receiver/
├── ui/
└── di/
```

## Persistence

* **Room** for structured state: profiles, blocked-package selections,
  activation windows, tag bindings, sessions, session snapshots, event log.
* **DataStore (Preferences)** only for simple app preferences (onboarding
  complete, selected backend, confirmation preference, diagnostics).

## Safety

`SafetyPolicy` maintains an immutable list of packages FocusLock refuses to
block (FocusLock itself, launcher, System UI, Settings, dialer, permission
controller, package installer, current IME, Google Play Services, etc.). Actual
package names are discovered at runtime where possible. A session never starts
when the requested block list is unsafe.

## Privacy

No `INTERNET` permission. No analytics. No cloud. The Accessibility service
only reads `event.packageName` / `event.eventType` and never inspects view
content or hierarchy.

## Threat model

See the product plan (`plan.md`) for the full threat model (T0–T10). The
important structural decisions that follow from it are:

* Session expiry uses the monotonic clock (defeats T3 wall-clock forwarding).
* Sessions survive reboot via `SessionReconciler` + `BootReceiver` (defeats T2).
* Sessions snapshot their rules (defeats T5/T6 profile edit/delete).
* A second tag can never shorten an existing session (defeats T7).
* Device Owner suspension does not depend on FocusLock staying alive
  (defeats T1/K).
* Package-change events trigger reconciliation so an app update is never an
  accidental unlock (defeats T8).
