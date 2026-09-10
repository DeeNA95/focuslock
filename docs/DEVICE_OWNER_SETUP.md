# Device Owner Setup

> **WARNING — read this before doing anything.**
>
> Becoming a Device Owner gives FocusLock the ability to suspend arbitrary
> packages at the OS level. Done incorrectly this can make a phone very
> inconvenient to use. **Do not attempt Device Owner provisioning on your daily
> phone.** Develop and test against a fresh emulator (or a factory-reset spare
> device) first. Always verify a working recovery route before relying on hard
> lock on a real device.

## When this is needed

- **Soft Lock** (Accessibility) needs no Device Owner and is safe to use on your
  phone. It is not a security boundary.
- **Hard Lock** requires FocusLock to be the Device Owner. This is the only way
  to get genuine OS-level package suspension that survives FocusLock process
  death.

## How Device Owner works

FocusLock declares a `DeviceAdminReceiver`
(`com.focuslock.enforcement.deviceowner.FocusLockDeviceAdminReceiver`). When the
device is provisioned with that receiver as the Device Owner, FocusLock can call
`DevicePolicyManager.setPackagesSuspended(...)`.

Becoming Device Owner is **not** an ordinary runtime permission dialog. It must
be provisioned via ADB (development) or a provisioning flow, and only on an
unprovisioned device (no accounts, freshly reset).

## Preparing a fresh emulator

1. Create a new AVD (API 34 is fine).
2. Do **not** sign in to Google, or create a device with no account.
3. Boot it fully.

## Installing the debug APK

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Identifying the admin component

The admin component is:

```
com.focuslock/com.focuslock.enforcement.deviceowner.FocusLockDeviceAdminReceiver
```

## Provisioning Device Owner (development)

The device must be unprovisioned (factory-fresh, no accounts). Use:

```bash
adb shell dpm set-device-owner com.focuslock/.enforcement.deviceowner.FocusLockDeviceAdminReceiver
```

If you already have accounts on the device you will get an error like
`java.lang.IllegalStateException: Not allowed to set the device owner because
there are already some accounts on the device`. Factory-reset the device/emulator
and retry.

## Verifying Device Owner status

```bash
adb shell dpm get-device-owner
```

Should print `com.focuslock`. You can also check inside the app via the
Diagnostics screen ("Device Owner: Yes").

## Removing / resetting Device Owner

Only a Device Owner can remove itself:

```bash
adb shell dpm remove-active-admin com.focuslock/.enforcement.deviceowner.FocusLockDeviceAdminReceiver
```

Or simply wipe the emulator:

```bash
adb shell pm clear com.focuslock   # clears app data, not ownership
adb emu kill && avdmanager wipe    # full reset (recommended)
```

> A factory reset always clears Device Owner. This is the ultimate escape hatch.

## Order of operations for safe development

1. Build the app and verify Soft Lock works end-to-end.
2. On a **fresh emulator**, provision Device Owner.
3. Create a short hard-lock profile (1–5 minutes via test durations).
4. Verify suspension, reboot recovery, and expiry restoration.
5. Verify the recovery route (see below) works.
6. Only then consider a spare physical device, never your daily phone.

## Recovery

FocusLock's emergency recovery (a long random recovery key whose hash is stored
locally) is implemented in a later phase. **Never use hard lock on a device
without a tested recovery route.**

## Safety rules

FocusLock refuses to suspend critical packages (itself, the launcher, System UI,
Settings, the dialer, the active IME, permission controller, package installer,
and Google Play Services). The `SafetyPolicy` also discovers several of these at
runtime. A hard session will not start if its block list is unsafe.
