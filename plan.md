# FocusLock — Android NFC Commitment App

## 0. Your Role

You are implementing a personal Android application called **FocusLock**.

Act as a senior Android engineer. Optimize for:

- reliability
- deterministic behavior
- simple architecture
- testability
- resistance to accidental or impulsive circumvention
- safe failure modes
- minimal permissions
- no cloud dependency
- no unnecessary libraries

This is initially a personal/sideloaded application targeting a Samsung Galaxy S22 Ultra and modern Android.

Do not build the entire project as one giant pass. Implement it in the phases described below, and keep the application compiling and testable after every phase.

Do not introduce root, Shizuku, private Samsung APIs, hidden Android APIs, or exploit-based behavior.

---

# 1. Product Concept

FocusLock is a physical commitment device built around NFC tags.

The basic interaction is:

```text
Tap NFC tag
      ↓
Resolve tag → Focus Profile
      ↓
Validate activation rules
      ↓
Create immutable Focus Session
      ↓
Block configured distracting apps
      ↓
Session cannot normally be cancelled early
      ↓
Timer expires
      ↓
Apps automatically become available again
```

Example:

```text
Tag: "Deep Work"

Allowed activation:
18:00–23:59

Duration:
8 hours

Blocked:
Instagram
X/Twitter
Reddit
TikTok
YouTube
Chrome

Tap at 21:13

→ locked until 05:13
```

The central philosophy is:

> It should be extremely easy for present-me to make a commitment and deliberately inconvenient for future-me to break it.

However, the application must not be capable of accidentally making the phone unusable.

---

# 2. Non-Goals

For v1, do NOT implement:

- cloud accounts
- synchronization
- social features
- AI
- website filtering
- DNS filtering
- VPN filtering
- desktop synchronization
- location triggers
- Bluetooth triggers
- parental-control features
- analytics
- remote control
- root features
- arbitrary scripting
- Google Play publishing work

Keep the system local and deterministic.

---

# 3. Core Architectural Principle

The focus/session logic must **not know how applications are blocked**.

Define an abstraction similar to:

```kotlin
interface EnforcementBackend {
    suspendPackages(packages: Set<String>): EnforcementResult

    resumePackages(packages: Set<String>): EnforcementResult

    suspendability(packageName: String): Suspendability

    fun isAvailable(): Boolean

    fun backendType(): EnforcementBackendType
}
```

Implement two backends:

```text
AccessibilityEnforcementBackend
DeviceOwnerEnforcementBackend
```

Everything else in the application talks only to:

```text
EnforcementBackend
```

This is critical.

We should be able to build almost the entire product using the Accessibility backend and later switch to Device Owner without rewriting the rule engine, database, NFC system, session logic or UI.

---

# 4. Enforcement Levels

## Level 1 — Soft Lock

Use an Android `AccessibilityService`.

Purpose:

- develop and test FocusLock without factory-resetting the physical phone
- provide useful blocking behavior immediately
- validate the UX and rule system

The service observes application/window changes.

When the foreground package belongs to the current session's blocked set:

```text
Blocked app becomes foreground
        ↓
AccessibilityService receives event
        ↓
SessionManager asks:
"Is this package currently blocked?"
        ↓
YES
        ↓
performGlobalAction(GLOBAL_ACTION_HOME)
        ↓
User returns to launcher
```

Do not inspect view contents.

We only need:

```text
event.packageName
event.eventType
```

Keep the Accessibility scope as narrow as possible.

Possible relevant events:

```text
TYPE_WINDOW_STATE_CHANGED
TYPE_WINDOWS_CHANGED
```

The service should do essentially:

```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    val packageName = event.packageName?.toString() ?: return

    if (sessionManager.isPackageBlocked(packageName)) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        blockNotifier.notifyBlocked(packageName)
    }
}
```

Do not build complicated overlays for v1.

A notification such as:

```text
🔒 Instagram is locked
Deep Work • 4h 17m remaining
```

is sufficient.

### Known weakness

Accessibility can be disabled by the user.

Therefore:

```text
Soft Lock != security boundary
```

Do not waste large amounts of engineering effort pretending otherwise.

---

# 5. Level 2 — Hard Lock

Implement a Device Owner backend using:

```text
DevicePolicyManager
```

The important primitive is:

```kotlin
setPackagesSuspended(...)
```

During session activation:

```kotlin
devicePolicyManager.setPackagesSuspended(
    adminComponent,
    packages.toTypedArray(),
    true
)
```

During expiration:

```kotlin
devicePolicyManager.setPackagesSuspended(
    adminComponent,
    packages.toTypedArray(),
    false
)
```

Suspended apps should be genuinely unusable at the OS level rather than merely intercepted after launch.

Some launchers may visually gray suspended applications. Treat that as launcher-dependent presentation.

**The important requirement is enforcement, not appearance.**

Capture and record any packages Android refuses to suspend.

Never silently claim success.

---

# 6. Hard-Mode Safety Rules

Maintain an immutable internal list of packages/categories which FocusLock must refuse to block.

At minimum protect:

```text
FocusLock itself
current launcher
System UI
Settings initially
default dialer
permission controller
package installer
package uninstaller
device policy components
keyboard / active IME
Google Play Services
core Samsung framework packages
emergency calling infrastructure
```

Discover actual package names at runtime wherever possible rather than hardcoding Samsung package names.

Build:

```kotlin
SafetyPolicy
```

with:

```kotlin
fun validateBlockList(
    requested: Set<String>
): SafetyValidationResult
```

A session must not begin if its configuration would put the phone into an unsafe state.

Do not suspend Settings in v1.

---

# 7. Fortress Mode

After normal Hard Lock is proven reliable, implement an optional stricter mode called:

```text
Fortress Mode
```

Fortress Mode may additionally do the following while a Focus Session is active:

1. Prevent manual date/time modification.
2. Require automatic network time where supported.
3. Prevent FocusLock itself from being casually uninstalled.
4. Reapply package suspension after relevant package/system events.

Use supported DevicePolicyManager/UserManager policy APIs only.

Example conceptual behavior:

```text
Session starts
    ↓
suspend distracting packages
    ↓
DISALLOW_CONFIG_DATE_TIME
    ↓
automatic time enabled
    ↓
FocusLock uninstall blocked
```

Then at expiration:

```text
unsuspend packages
    ↓
restore temporary restrictions
    ↓
restore FocusLock uninstall behavior
```

IMPORTANT:

Every temporary policy change must be tracked so FocusLock knows exactly what it changed and can restore the previous state correctly.

Do not blindly overwrite global settings.

---

# 8. Threat Model

Think explicitly about the following ways future-me might attempt to cheat.

## T0 — Open blocked application

Must fail.

## T1 — Kill FocusLock

Soft mode:

Possibly defeats enforcement.

Accept this limitation.

Hard mode:

Suspended-package state must not depend on FocusLock continuously running.

## T2 — Reboot phone

Active session must survive reboot.

## T3 — Change system time forward

During the same boot, duration calculations should use monotonic elapsed time.

Hard/Fortress mode should additionally prevent manual date/time modification.

## T4 — Change timezone

Changing timezone must not shorten a duration-based active session.

## T5 — Edit the Focus Profile

Editing a profile during an active session must **not affect that session**.

A session snapshots its rules when created.

## T6 — Delete the Focus Profile

Deleting the originating profile must not terminate the active session.

## T7 — Scan another NFC tag

A second tag must never shorten the current lock.

For v1:

```text
if session active:
    reject new session activation
```

Later we may permit extensions.

Never permit:

```text
newExpiry < existingExpiry
```

## T8 — Uninstall blocked application and reinstall it

Hard mode should reconcile relevant package events.

## T9 — Disable Accessibility

Soft mode can be defeated this way.

Document rather than pretending otherwise.

## T10 — Factory reset / advanced ADB intervention

Outside the normal threat model.

Someone willing to factory-reset their phone to open Twitter has decisively won the argument with themselves.

---

# 9. Timekeeping Model

Do not implement the session using a decrementing timer.

Never make a countdown itself authoritative.

Store timestamps.

A session should contain approximately:

```kotlin
data class FocusSession(
    val id: UUID,

    val profileId: UUID?,

    val profileNameSnapshot: String,

    val startedAtWallClock: Instant,

    val startedAtElapsedRealtimeMs: Long,

    val expiresAtWallClock: Instant,

    val duration: Duration,

    val blockedPackagesSnapshot: Set<String>,

    val enforcementMode: EnforcementMode,

    val status: SessionStatus
)
```

Use:

```kotlin
SystemClock.elapsedRealtime()
```

for duration measurement while the device remains within the same boot.

Conceptually:

```text
elapsed = elapsedRealtimeNow - session.startedElapsedRealtime
```

The session expires when:

```text
elapsed >= duration
```

This means changing:

```text
21:00 → 05:00
```

manually cannot instantly defeat an 8-hour session during the same boot.

Also store wall-clock expiry for:

- UI
- reboot recovery
- diagnostics
- scheduling

---

# 10. Reboot Handling

Register appropriate boot handling.

On boot:

```text
BOOT_COMPLETED
      ↓
SessionReconciler
      ↓
Load active session
      ↓
Determine whether it should still be active
      ↓
YES → ensure packages are blocked
NO  → ensure packages are restored
      ↓
schedule next expiry reconciliation
```

Never assume scheduled callbacks survived exactly as expected.

The database is the source of truth.

The enforcement backend must be considered something that is **reconciled toward desired state**.

Think Kubernetes-like:

```text
desired state:
Instagram = suspended

actual state:
Instagram = not suspended

reconcile()

→ suspend Instagram
```

This pattern should be used throughout the application.

---

# 11. Session State Machine

Implement an explicit state model.

For example:

```text
IDLE

ACTIVATING

ACTIVE

EXPIRING

COMPLETED

FAILED
```

Transitions:

```text
IDLE
  ↓ activate
ACTIVATING
  ↓ enforcement successful
ACTIVE
  ↓ deadline reached
EXPIRING
  ↓ apps restored
COMPLETED
```

Failure:

```text
ACTIVATING
  ↓ critical enforcement failure
FAILED
```

Do not persist a session as ACTIVE until we have verified sufficient enforcement was applied.

If one ordinary selected application cannot be blocked, activation should return a clear error rather than silently producing a partial lock unless explicitly configured otherwise.

---

# 12. Session Immutability

This is important.

Suppose:

```text
Deep Work profile

Instagram
Twitter
Reddit

8 hours
```

is activated.

The session should store:

```text
Instagram
Twitter
Reddit
8 hours
```

directly.

If the user then edits the profile and removes Instagram, the active session remains unchanged.

Profiles are templates.

Sessions are commitments.

---

# 13. Focus Profiles

Model:

```kotlin
data class FocusProfile(
    val id: UUID,
    val name: String,
    val duration: Duration,
    val blockedPackages: Set<String>,
    val activationRules: List<ActivationWindow>,
    val enforcementMode: EnforcementMode,
    val fortressModeEnabled: Boolean
)
```

Example:

```text
Deep Work

Duration:
8h

Apps:
Instagram
X
Reddit
TikTok
YouTube

Activation:
Mon–Sun
18:00–23:59

Mode:
Hard
```

---

# 14. Activation Windows

Support rules such as:

```text
Only allow this NFC tag to trigger between:

20:00 and 01:00
```

The logic must correctly support midnight crossing.

Example:

```kotlin
fun isWithinWindow(
    now: LocalTime,
    start: LocalTime,
    end: LocalTime
): Boolean {
    return if (start <= end) {
        now >= start && now < end
    } else {
        now >= start || now < end
    }
}
```

Also support selected days of week.

The activation rule affects **whether a session may start**.

Once started, its duration is independent of the activation window.

Example:

```text
Allowed activation:
20:00–23:00

Scan:
22:37

Duration:
8h

Expiry:
06:37
```

Correct.

Do not stop at 23:00.

---

# 15. Future Session Modes

Design the domain layer so that later we can support:

```text
DURATION
```

Example:

```text
8 hours from activation
```

and:

```text
UNTIL_LOCAL_TIME
```

Example:

```text
until 07:00 tomorrow
```

Only Duration needs to be implemented initially.

Do not bake `durationHours` throughout the codebase.

Use:

```kotlin
java.time.Duration
```

where appropriate.

---

# 16. NFC Architecture

NFC tags are **activation devices**.

They must never be early-unlock devices.

Use NDEF.

Do not depend solely on NFC hardware UID because tag UID behavior varies by tag type.

Write an application-specific random identifier into the NDEF payload.

Something like:

```text
focuslock://tag/8d476a81-....
```

Each physical tag maps to:

```text
TagBinding
```

Example:

```kotlin
data class TagBinding(
    val id: UUID,
    val token: String,
    val label: String,
    val profileId: UUID
)
```

Tag tokens should be random and sufficiently long.

The actual Focus Profile configuration lives in the app database.

The tag contains only an identifier.

Therefore changing:

```text
Deep Work:
6h → 8h
```

does not require rewriting the tag.

---

# 17. Pair Tag Flow

UI:

```text
Profile
  ↓
Pair NFC Tag
  ↓
"Hold an NFC tag against the phone"
  ↓
Generate random tag token
  ↓
Write NDEF record
  ↓
Read back
  ↓
Verify payload
  ↓
Save TagBinding
```

Do not report success until the written tag has been successfully read/verified.

Allow tag labels:

```text
Desk
Bedside
Gym
Office
```

---

# 18. NFC Activation Flow

When Android dispatches the tag:

```text
NFC intent received
       ↓
NfcIntentParser
       ↓
Validate URI/schema
       ↓
Extract tag token
       ↓
TagRepository.resolve(token)
       ↓
FocusProfile
       ↓
RuleEngine.canActivate(profile, now)
       ↓
SessionManager.activate(profile)
```

Possible results:

```text
ACTIVATED

OUTSIDE_ALLOWED_WINDOW

UNKNOWN_TAG

SESSION_ALREADY_ACTIVE

PROFILE_DISABLED

ENFORCEMENT_UNAVAILABLE

UNSAFE_CONFIGURATION
```

Every branch gets explicit UI.

Do not silently ignore failures.

---

# 19. Scan UX

For trusted FocusLock tags, the eventual preferred flow is:

```text
unlock phone
tap physical tag
*beep*
Deep Work activated
```

No seven-screen wizard.

During development, optionally show confirmation:

```text
Start Deep Work?

8 hours
6 apps blocked

[Commit]
```

Make confirmation configurable later.

The commitment action should clearly show:

```text
Duration
Expiry
Apps affected
Mode
```

before activation.

---

# 20. Active Session UX

Main screen during active session:

```text
┌─────────────────────────────┐
│        DEEP WORK            │
│                             │
│          05:42:17           │
│          remaining          │
│                             │
│ Unlocks 05:13               │
│                             │
│  6 apps unavailable         │
│                             │
│ 🔒 Hard Lock                │
└─────────────────────────────┘
```

No giant:

```text
CANCEL SESSION
```

button.

The UI should communicate:

```text
"You already made this decision."
```

without being obnoxious.

---

# 21. Emergency Recovery

There must be a recovery mechanism because a Device Owner application capable of suspending packages can cause serious inconvenience if implemented incorrectly.

However, recovery must not be equivalent to:

```text
Are you sure?

[Yes]
```

Implement a separate emergency-recovery system.

Suggested design:

During setup generate a long random recovery key.

Example conceptual flow:

```text
Emergency recovery key generated
        ↓
User is told to store it somewhere off-device
        ↓
Only a hash is stored locally
```

Early release requires entering the complete recovery key.

Do not display the recovery key again.

Do not provide password reset.

The point is:

```text
possible during genuine emergency
≠
convenient during boredom
```

For debug builds, also document an ADB recovery procedure.

Never ship a version of the Device Owner backend without a tested recovery route.

---

# 22. App Picker

Build an installed-application picker.

Show:

```text
icon
human-readable label
package name in secondary text
selection checkbox
```

Only ordinary launchable user applications should be prominent.

Allow search.

Example:

```text
Search apps...

☑ Instagram
  com.instagram.android

☑ Reddit
  com.reddit.frontpage

☐ Spotify
  com.spotify.music
```

Pass every selection through `SafetyPolicy`.

Clearly mark applications that cannot safely be blocked.

Do not request broader package visibility than actually required unless the implementation genuinely needs it.

---

# 23. Persistence

Use:

```text
Room
```

for structured state.

Suggested entities:

```text
FocusProfileEntity
ProfileBlockedPackageEntity
ActivationWindowEntity
TagBindingEntity
FocusSessionEntity
SessionBlockedPackageEntity
SessionEventEntity
```

Use DataStore only for simple application preferences such as:

```text
onboarding complete
default enforcement backend
confirmation preference
diagnostic preferences
```

Do not serialize the entire product state into one giant JSON DataStore value.

---

# 24. Repository Layer

Suggested repositories:

```text
ProfileRepository
SessionRepository
TagRepository
AppRepository
SettingsRepository
```

Domain services:

```text
RuleEngine
SessionManager
SessionReconciler
SafetyPolicy
TimeAuthority
TagResolver
```

Enforcement:

```text
EnforcementBackend
AccessibilityEnforcementBackend
DeviceOwnerEnforcementBackend
```

Platform infrastructure:

```text
NfcManager
SessionAlarmScheduler
BootReceiver
PackageChangeReceiver
TimeChangeReceiver
DeviceAdminReceiver
```

---

# 25. Suggested Project Structure

```text
app/
├── data/
│   ├── db/
│   ├── dao/
│   ├── entity/
│   ├── repository/
│   └── datastore/
│
├── domain/
│   ├── model/
│   ├── rules/
│   ├── session/
│   ├── safety/
│   └── time/
│
├── enforcement/
│   ├── EnforcementBackend.kt
│   ├── accessibility/
│   └── deviceowner/
│
├── nfc/
│   ├── NfcIntentParser.kt
│   ├── NfcTagWriter.kt
│   └── TagResolver.kt
│
├── scheduling/
│   ├── SessionAlarmScheduler.kt
│   └── SessionExpiryWorker.kt
│
├── receiver/
│   ├── BootReceiver.kt
│   ├── PackageChangeReceiver.kt
│   └── TimeChangeReceiver.kt
│
├── ui/
│   ├── home/
│   ├── profile/
│   ├── apppicker/
│   ├── tags/
│   ├── session/
│   ├── diagnostics/
│   └── onboarding/
│
└── di/
```

Use dependency injection, preferably Hilt unless there is a compelling reason not to.

---

# 26. UI Technology

Use:

```text
Kotlin
Jetpack Compose
Material 3
Coroutines
Flow
Room
Hilt
```

Avoid XML UI unless required by an Android platform component.

Use current stable Android libraries rather than experimental dependencies unless required.

Target the latest stable Android SDK available in the development environment.

A modern minimum SDK is acceptable because this is initially a personal application for a modern device.

---

# 27. Scheduler

The deadline stored in `FocusSession` is authoritative.

Scheduling exists to wake the application and reconcile state.

Use an abstraction:

```kotlin
interface SessionScheduler {
    fun scheduleExpiry(session: FocusSession)
    fun cancel(sessionId: UUID)
}
```

Use Android-supported scheduling mechanisms.

If exact alarm access is available and justified, it may be used for precise expiry.

Otherwise use resilient delayed/background work.

Regardless:

```text
scheduler callback ≠ source of truth
database session state = source of truth
```

When anything wakes FocusLock:

```text
app launch
boot
alarm
worker
NFC scan
accessibility event
package event
```

call reconciliation where appropriate.

---

# 28. Reconciliation Algorithm

Conceptually:

```kotlin
suspend fun reconcile() {
    val session = sessionRepository.getActiveSession()
        ?: return ensureNoUnexpectedFocusPolicies()

    if (timeAuthority.isExpired(session)) {
        enforcement.resumePackages(
            session.blockedPackagesSnapshot
        )

        restoreTemporaryPolicies(session)

        sessionRepository.markCompleted(session.id)

        scheduler.cancel(session.id)

        return
    }

    enforcement.suspendPackages(
        session.blockedPackagesSnapshot
    )

    ensureTemporaryPolicies(session)

    scheduler.scheduleExpiry(session)
}
```

Make this operation idempotent.

Running it five times should produce the same desired state as running it once.

---

# 29. Notifications

While a session is active, provide a persistent notification:

```text
🔒 Deep Work

5h 42m remaining
6 apps locked
```

Tapping opens the active-session screen.

Do not spam notifications every time the timer changes.

If the user attempts a blocked app in Accessibility mode:

```text
Instagram unavailable
Deep Work ends at 05:13
```

Rate-limit repeated block notifications.

---

# 30. Privacy

The application should require no network connection.

Prefer not to request:

```text
INTERNET
```

at all for v1.

All data stays local.

Accessibility handling must not log:

```text
screen text
typed text
passwords
view hierarchy
```

We only care which package became foreground.

---

# 31. Diagnostics

Build a useful diagnostics screen because Android device-management bugs are otherwise painful.

Display:

```text
NFC hardware:
Available

NFC:
Enabled

Accessibility service:
Enabled

Device Admin:
Active

Device Owner:
Yes / No

Selected backend:
Device Owner

Exact alarm capability:
Granted / Not granted

Active session:
Deep Work

Desired blocked packages:
6

Actually suspended:
6

Last reconciliation:
21:14:03

Last reconciliation result:
SUCCESS
```

Add a local event log.

Examples:

```text
21:13:42 NFC_TAG_DETECTED
21:13:42 PROFILE_RESOLVED Deep Work
21:13:43 SESSION_CREATED
21:13:43 PACKAGE_SUSPENDED com.instagram.android
21:13:43 PACKAGE_SUSPENDED com.reddit.frontpage
21:13:44 SESSION_ACTIVE
```

Never put sensitive user content in logs.

---

# 32. Device Owner Provisioning

Do NOT attempt to turn the physical daily phone into a Device Owner during early development.

Develop Device Owner support against a fresh Android emulator first.

Create:

```text
docs/DEVICE_OWNER_SETUP.md
```

Document:

1. How a fresh emulator/device must be prepared.
2. How to install the debug APK.
3. How to identify the `DeviceAdminReceiver`.
4. How to use the supported `adb shell dpm ...` development commands.
5. How to verify Device Owner status.
6. How to safely remove/reset it in a debug environment.
7. Which steps may require a fresh/unprovisioned device.
8. Strong warning before attempting this on a primary phone.

Never automatically run provisioning commands.

Never design the application under the assumption that becoming Device Owner is an ordinary runtime permission dialog.

---

# 33. Device Owner Preflight

Before entering Hard Lock:

```text
Is FocusLock Device Owner?
      ↓
Does profile contain unsafe packages?
      ↓
Can packages be suspended?
      ↓
Is recovery configured?
      ↓
Can expiry scheduling be established?
      ↓
PASS
      ↓
activate session
```

If preflight fails:

```text
DO NOT ACTIVATE A PARTIALLY TRUSTED HARD SESSION
```

Show exactly what failed.

---

# 34. Package Suspension Transactions

Think transactionally.

Activation:

```text
1. create pending session
2. validate packages
3. apply temporary policies
4. suspend packages
5. verify result
6. persist ACTIVE
7. schedule expiry
```

If step 4 fails unexpectedly:

```text
rollback packages already suspended
restore temporary policies
mark session FAILED
```

Do not leave the system half-configured.

Expiry:

```text
1. mark EXPIRING
2. unsuspend snapshot packages
3. verify
4. restore temporary policies
5. mark COMPLETED
```

If restoration fails:

keep enough session metadata to retry reconciliation.

---

# 35. Package Changes

Listen for relevant package lifecycle events.

If an active session contains:

```text
com.example.distraction
```

and that package is replaced/updated/reinstalled, reconciliation should verify that its blocked state still matches the current session.

Never let a package update become an accidental unlock mechanism.

---

# 36. Profile Editing UX

Screens:

```text
Profiles
  ↓
Deep Work
  ↓
Name
Duration
Apps
Activation window
Enforcement mode
Fortress mode
NFC tags
```

The app should make profiles pleasant to create because the physical tag workflow depends on them.

Potential examples:

```text
Deep Work — 8h
Sleep — until morning
Gym — 2h
Reading — 3h
```

Only duration mode needs implementation initially.

---

# 37. Onboarding

Suggested onboarding:

```text
Welcome
  ↓
Explain commitment concept
  ↓
Create first profile
  ↓
Choose distracting apps
  ↓
Configure Accessibility backend
  ↓
Pair NFC tag
  ↓
Run 1-minute test session
  ↓
Success
```

Do NOT make the first test an eight-hour lock.

Provide a special onboarding/test session:

```text
60 seconds
```

so all enforcement and restoration paths can be tested safely.

---

# 38. Test Mode

Create a development/test profile capable of durations such as:

```text
30 seconds
1 minute
5 minutes
```

Production profile UI can later impose a larger minimum.

This will massively speed development.

---

# 39. Automated Testing

The domain layer must be heavily unit tested.

Inject clocks.

Do not call system time directly throughout business logic.

Use something like:

```kotlin
interface TimeAuthority {
    fun now(): Instant
    fun elapsedRealtimeMillis(): Long
}
```

Create:

```text
FakeTimeAuthority
```

Test:

### Activation windows

```text
normal daytime window
midnight-crossing window
exact start boundary
exact end boundary
different day-of-week
```

### Sessions

```text
new session expiration
elapsed time
wall-clock manipulation
profile modification
profile deletion
second activation while active
```

### Safety

```text
block normal app
reject launcher
reject FocusLock
reject required system package
```

### Reconciliation

```text
desired blocked + actual allowed
desired blocked + actual blocked
expired + actual blocked
no active session
partial backend failure
```

---

# 40. Instrumentation / Device Tests

Test on emulator:

```text
NFC intent parsing
Room persistence
boot recovery where possible
Accessibility service behavior
Device Owner provisioning
package suspension
package restoration
reboot with active session
```

Then physical-device test:

```text
1-minute Accessibility session
5-minute Accessibility session
reboot during session
NFC closed-app activation
screen-off / Doze behavior
One UI launcher behavior
battery optimization behavior
```

Only after those pass should Device Owner be considered for the physical S22 Ultra.

---

# 41. Critical Acceptance Tests

The project is not considered reliable until all of these pass.

### A

```text
Tap valid tag inside window.
Session starts.
Blocked apps become inaccessible.
```

### B

```text
Tap valid tag outside allowed window.
Nothing is locked.
Reason is displayed.
```

### C

```text
Open allowed app during session.
Works normally.
```

### D

```text
Edit source profile during session.
Existing session remains unchanged.
```

### E

```text
Delete source profile during session.
Existing session remains active.
```

### F

```text
Move wall clock eight hours forward during same boot.
Session does not expire early.
```

### G

```text
Reboot halfway through session.
Session is recovered and enforcement resumes.
```

### H

```text
Session expires.
Every package FocusLock suspended becomes available again.
```

### I

```text
One suspension fails during activation.
Session does not silently become ACTIVE.
```

### J

```text
Scan another tag during active session.
Existing session cannot be shortened.
```

### K

```text
FocusLock process dies during Device Owner session.
Blocked apps remain blocked.
```

### L

```text
Recovery procedure works in test environment.
```

---

# 42. Implementation Phases

## Phase 0 — Skeleton

Create:

```text
Compose project
navigation
Hilt
Room
domain modules/packages
tests
```

No blocking yet.

Deliverable:

App launches and tests pass.

---

## Phase 1 — Domain Model

Implement:

```text
FocusProfile
ActivationWindow
FocusSession
RuleEngine
TimeAuthority
SafetyPolicy interfaces
Session state machine
```

Use fake repositories initially.

Write comprehensive unit tests.

Deliverable:

The entire commitment/rule system works without Android platform code.

---

## Phase 2 — Persistence

Implement Room repositories.

Persist:

```text
profiles
app selections
activation windows
tags
sessions
session snapshots
event logs
```

Deliverable:

State survives process death.

---

## Phase 3 — Profile UI

Implement:

```text
profile list
create profile
duration
app picker
activation window
profile editing
```

Deliverable:

Complete profile can be configured from UI.

---

## Phase 4 — NFC

Implement:

```text
tag writing
tag verification
tag binding
NDEF parsing
closed-app NFC dispatch
profile resolution
```

Initially make scanning simply display:

```text
Deep Work tag detected.
```

Then connect to activation.

Deliverable:

Physical tag resolves reliably to a Focus Profile.

---

## Phase 5 — Session System

Implement:

```text
activation
active-session screen
immutable snapshots
countdown display
expiration
persistent notification
scheduler
reconciliation
```

Use a fake enforcement backend initially.

Deliverable:

Sessions work correctly without actual app blocking.

---

## Phase 6 — Accessibility Backend

Implement:

```text
AccessibilityService
foreground package detection
HOME interception
blocked-attempt notifications
```

Deliverable:

Useful real-world FocusLock v1.

Install it on the S22 Ultra.

Use it for several days.

Find UX problems before doing Device Owner work.

---

## Phase 7 — Resilience

Implement/test:

```text
boot reconciliation
time changes
timezone changes
package replacement
process death
Doze
battery optimization
scheduler recovery
```

Deliverable:

Soft version behaves predictably across ordinary Android lifecycle events.

---

## Phase 8 — Device Owner Backend

Do this on an emulator first.

Implement:

```text
DeviceAdminReceiver
ownership detection
setPackagesSuspended
package restoration
hard-mode preflight
transactional activation
transactional expiration
diagnostics
```

Deliverable:

Hard sessions work on managed emulator.

---

## Phase 9 — Fortress Mode

Implement supported anti-circumvention policies:

```text
date/time configuration restriction
automatic time policy
FocusLock uninstall protection
additional reconciliation
```

Every restriction must automatically restore when the session ends.

Deliverable:

Changing time, killing FocusLock, or casually entering Settings cannot trivially defeat a Hard/Fortress session.

---

## Phase 10 — Recovery & Chaos Testing

Test deliberately bad situations:

```text
kill process
reboot repeatedly
change timezone
deny exact alarms
disable NFC
update blocked app
remove blocked app
backend throws exception
database operation fails
session expires while phone asleep
session expires during reboot
```

Prove FocusLock always moves toward a safe deterministic state.

---

# 43. Engineering Constraints

Throughout implementation:

- Prefer explicit code over clever abstractions.
- Never use a timer as authoritative session state.
- Never let profile editing mutate an active commitment.
- Never let a new session shorten an existing one.
- Never let NFC unlock an active session.
- Never silently swallow enforcement failures.
- Never suspend unknown critical system packages.
- Never require the network.
- Never rely on a background process staying alive.
- Never assume an alarm fires exactly when requested.
- Never test first-time Device Owner behavior on the primary phone.
- Never ship Device Owner behavior without recovery.
- Never bypass Android security mechanisms.
- Keep enforcement implementations replaceable.
- Make reconciliation idempotent.
- Keep the database as durable truth.

---

# 44. Initial Product Definition

The first version I actually want to use should support exactly this:

```text
Create Profile:
"Deep Work"

Duration:
8 hours

Activation:
18:00–00:00

Blocked apps:
user-selected

Pair:
physical NFC tag on desk
```

Then:

```text
21:00

tap desk tag
      ↓
phone vibrates
      ↓
"Deep Work active until 05:00"
      ↓
selected applications unavailable
```

Opening FocusLock shows:

```text
DEEP WORK

07:59:53 remaining

Instagram
Reddit
YouTube
X
TikTok

Session began:
21:00

Ends:
05:00

Enforcement:
Hard
```

There is no ordinary early-unlock button.

At 05:00:

```text
session expires
apps restored
temporary policies restored
session marked COMPLETED
```

That is the core product.

Everything else is secondary.

---

# 45. First Task

Before writing significant Android platform code:

1. Create the project skeleton.
2. Write `README.md`.
3. Write `docs/ARCHITECTURE.md`.
4. Define the domain models and interfaces.
5. Implement `RuleEngine`.
6. Implement `TimeAuthority`.
7. Implement session-expiry logic.
8. Write unit tests covering midnight windows, duration expiry and wall-clock manipulation.
9. Run the test suite.
10. Only then continue to persistence/UI.

In `ARCHITECTURE.md`, explicitly document this flow:

```text
NFC
 ↓
TagResolver
 ↓
RuleEngine
 ↓
SessionManager
 ↓
SessionRepository
 ↓
EnforcementBackend
 ↓
Android
```

and:

```text
                  ┌─ Accessibility backend
SessionManager ───┤
                  └─ Device Owner backend
```

Do not prematurely implement Device Owner functionality before the core domain model is correct.

When a phase is finished, run tests/builds, fix failures, summarize what changed, and continue to the next phase unless a genuinely blocking platform limitation is discovered.
