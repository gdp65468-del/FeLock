# SelfLock

SelfLock is an open-source Android lockout app for controlling when and how applications can be used.

## Lockouts

A lockout defines a schedule and a list of applications that remain available. While it is active, every other non-essential application is blocked by a full-screen overlay.

Each lockout can configure:

- days and start/end times;
- multiple allowed applications;
- one allowed progress application;
- accumulated minutes required in the progress application;
- the free-time reward granted after reaching the goal;
- the maximum number of rewards per lockout session;
- an alternative release that becomes available after a configured wait;
- the duration of the alternative release;
- whether Android Settings is blocked;
- optional password protection.

Completing the progress goal temporarily releases all applications. When the reward ends, the lockout resumes automatically. Progress is accumulated only while the selected progress application is in the foreground.

If the goal is not completed, the overlay displays a **Release** button after the contingency wait. Nothing is released automatically: the free-time window begins only after the user presses the button. Existing progress is preserved.

## Enforcement

The Accessibility Service reacts immediately when the foreground application changes. A foreground monitoring service polls Usage Stats every second as backup enforcement and records progress. Lockout sessions, earned rewards, and contingency use are persisted in Room so process recreation does not reset them.

Each lockout can use one or more progress applications. Time accumulated in any selected progress application counts toward the same reward.

When **Block Settings** is enabled, Android Settings remains blocked for the full scheduled lockout, including earned free-time windows.

## Protection modes

- **Standard protection** works on any supported Android device and blocks applications and Android Settings while the lockout is active.
- **Full protection** uses Android lock task mode when SelfLock is configured as the device owner. This prevents leaving the allowed set of apps to reach Settings during a lockout. Device-owner setup requires a factory-reset device and provisioning during initial setup.

For Xiaomi devices, set SelfLock battery usage to **No restrictions** and keep Accessibility, Usage Access, and battery-optimization exemption enabled so the foreground monitor is not interrupted.

SelfLock does not inspect browser URLs, block website domains, or create a VPN connection.

## Tech stack

- Kotlin and Java 17
- Jetpack Compose and Material 3
- MVVM with Hilt
- Room
- Coroutines
- Accessibility Service
- Usage Stats and foreground service
- AlarmManager
- AndroidX Security Crypto and PBKDF2

**Requirements:** Android 14 or newer (API 34), target API 36, and JDK 17.

## Build

```bash
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For a clean build:

```bash
./gradlew clean assembleDebug
```

Install it on a connected device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## First-run permissions

1. Accessibility for immediate foreground-app enforcement
2. Usage access for progress measurement and backup enforcement
3. Display over other apps for the block experience
4. Notifications for foreground monitoring
5. Exact alarms for schedule boundaries
6. Battery optimization exemption for reliable monitoring

A master password and recovery account are optional.

## Privacy

Rules, sessions, usage, and password data stay on the device. SelfLock has no browser inspection, VPN traffic, analytics, advertising SDK, account requirement, or cloud synchronization.

## Limitations

SelfLock cannot fully prevent Safe Mode, ADB removal, or factory reset. Reliable enforcement depends on Accessibility and Usage Access remaining enabled.

## License

This project is licensed under the [MIT License](LICENSE).
