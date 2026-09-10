# FocusLock

FocusLock is a personal Android commitment device built around NFC tags.

Tap a physical NFC tag to begin an immutable Focus Session that blocks a
configured set of distracting apps until a timer expires. The goal is to make
it extremely easy for *present-you* to make a commitment and deliberately
inconvenient for *future-you* to break it — without ever being able to make the
phone unusable.

## How it works

```text
Tap NFC tag
      ↓
Resolve tag → Focus Profile
      ↓
Validate activation rules (time windows)
      ↓
Create immutable Focus Session (snapshot of rules)
      ↓
Block configured distracting apps
      ↓
Timer expires
      ↓
Apps automatically become available again
```

## Enforcement levels

| Level | Backend | Mechanism | Notes |
|-------|---------|-----------|-------|
| 1 — Soft Lock | `AccessibilityEnforcementBackend` | Detects the foreground package and sends the user HOME | Not a security boundary; user can disable the service |
| 2 — Hard Lock | `DeviceOwnerEnforcementBackend` | `DevicePolicyManager.setPackagesSuspended(...)` | OS-level suspension; survives FocusLock process death |

Both backends implement the same `EnforcementBackend` interface, so the entire
domain, session, persistence and UI layers are backend-agnostic.

## Requirements

* JDK 17
* Android SDK (compile/target SDK 34, min SDK 26)
* Gradle (via wrapper)

## Building

```bash
./gradlew assembleDebug
```

## Testing

```bash
./gradlew testDebugUnitTest
```

## Project status

| Phase | Status |
|-------|--------|
| 0 — Skeleton | Done |
| 1 — Domain model, RuleEngine, TimeAuthority, session expiry | Done |
| 2 — Persistence (Room) | Done |
| 3 — Profile UI | Done |
| 4 — NFC | Done |
| 5 — Session system | Done |
| 6 — Accessibility backend | Done |
| 7 — Resilience | Done |
| 8 — Device Owner backend | Done |
| 9 — Fortress Mode | Done |
| 10 — Recovery & chaos testing | Done |

## Design principles

* The domain/session logic never knows how apps are blocked.
* A timer/countdown is never authoritative; timestamps are.
* Same-boot expiry uses the monotonic `elapsedRealtime` clock.
* Profiles are templates; sessions are commitments (immutable snapshots).
* Reconciliation is idempotent: the database is the source of truth, and the
  enforcement backend is reconciled *toward* desired state.
* No network, no cloud, no analytics. All data stays local.

## Docs

* [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — design overview
* [docs/DEVICE_OWNER_SETUP.md](docs/DEVICE_OWNER_SETUP.md) — hard-lock provisioning
* [docs/CHAOS_TESTING.md](docs/CHAOS_TESTING.md) — acceptance + failure-injection matrix

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the detailed design.
