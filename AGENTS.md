# SelfLock — Agent Instructions

## Project Overview
SelfLock is a personal Android app (Pixel 8, Android 16/API 36) for controlling app usage and lockout periods. Built with Kotlin, Jetpack Compose, Material 3, Hilt DI, and Room.

## Build Commands
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew clean assembleDebug    # Clean build
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture
- **MVVM + Clean Architecture** with Hilt dependency injection
- **Data layer**: Room database (`AppDatabase`, version 4) with `LockoutRule`, `LockoutAllowedApp`, `LockoutSession`, `UsageLog`, and `BlockEvent` + `AggregatedUsage` query POJO
- **Domain layer**: Use cases for lockout enforcement, reward progress, contingency release, and installed apps
- **UI layer**: Jetpack Compose with 2 tabs (Apps, Statistics) + Settings as ModalBottomSheet + onboarding + lock screen + block overlay

## Key Mechanisms
- **App blocking**: Accessibility service detects the foreground app and enforces active allowlist lockouts. `MonitoringService` polls `UsageStatsManager` every 5s for backup enforcement and reward progress.
- **Usage tracking**: `MonitoringService` accumulates time in the selected progress app and grants configurable free-time rewards.
- **Self-control**: Active lockouts are read-only. Apps outside the allowlist remain blocked unless a reward or manually activated contingency window is active.
- **Schedule alarms**: `ScheduleAlarmHelper` schedules `AlarmManager` alarms at block start/end times via `ScheduleReceiver`.
- **App Lock**: Optional master password gates app access. `MasterPasswordManager` stores hashed password in `EncryptedSharedPreferences`. `LockScreenActivity` shown on launch/resume when enabled. `AppLockState.isUnlocked` in-memory flag resets on `onStop()`. `FLAG_SECURE` applied when enabled.
- **Password-guarded rules**: Individual rules can have their own password (`isPasswordProtected`, `passwordHash` columns). Modifications require password entry via `PasswordEntryDialog`. Master password serves as universal override. Password hashing uses PBKDF2WithHmacSHA256 (600k iterations, 32-byte salt).
- **App icons**: Loaded via `PackageManager.getApplicationIcon()` and cached in `AppIconCache` (LruCache). Displayed in `AppRuleCard`, `AddAppRuleSheet`, and `BlockOverlayContent`.

## Coding Standards
- **No comments** unless explicitly asked
- **Surgical changes** — touch only what the task requires
- **Simplicity first** — no speculative features, no unnecessary abstractions
- **Match existing style** — follow patterns in neighboring files
- **Kotlin idioms** — use `lateinit var` for Hilt injection, `suspend` for DB calls, `Flow` for reactive data

## Package Structure
```
com.selflock.app/
├── di/              # Hilt modules (AppModule, DatabaseModule)
├── security/        # MasterPasswordManager (EncryptedSharedPreferences + PBKDF2), AppLockState
├── data/local/      # Room entities, DAOs, database (v4), type converters, AggregatedUsage POJO
├── data/repository/ # Repositories wrapping DAOs
├── domain/model/    # Lockout decision and usage models
├── domain/usecase/  # Business logic (LockoutManager, GetInstalledApps)
├── service/         # Android services (Monitoring, Accessibility, Boot/Schedule receivers)
├── ui/
│   ├── navigation/  # NavGraph (2 tabs + settings ModalBottomSheet)
│   ├── theme/       # Material 3 theme
│   ├── components/  # Reusable composables (BlockOverlayContent, AppIcon,
│   │                #   PasswordEntryDialog, PasswordProtectionSection, UsageBar,
│   │                #   SummaryCard, DateRangeSelector)
│   ├── screens/
│   │   ├── onboarding/  # OnboardingScreen (7-step permission wizard)
│   │   ├── app/         # AppBlockScreen, AppRuleCard, AddAppRuleSheet
│   │   ├── statistics/  # StatisticsScreen, StatisticsViewModel
│   │   ├── settings/    # SettingsSheet, SettingsViewModel, SetPasswordSheet, DisablePasswordDialog
│   │   └── lock/        # LockScreenActivity, LockScreen, LockScreenViewModel
│   └── util/        # Formatters, AppIconCache
└── util/            # Helpers (PermissionHelper, ScheduleHelper, etc.)
```

## Important Notes
- **App-only focus** — no VPN, URL inspection, or website blocking
- **Crash handler** — `CrashHandlerActivity` runs in `:crash` process, shows full stack traces
- **minSdk 34, targetSdk 36** — can use modern Android APIs freely
- **Personal use app** — no privacy concerns, show everything on crash
- **DB version 4** — version 3 removes website data; version 4 replaces per-app block rules with allowlist lockouts and persisted reward sessions
- **Settings is a ModalBottomSheet** — accessible via gear icon in TopAppBar on all tabs, not a separate tab
- **Security dependency** — `security-crypto` is used by `MasterPasswordManager`
