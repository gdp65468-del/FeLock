# SelfLock — Agent Instructions

## Project Overview
SelfLock is a personal Android app (Pixel 8, Android 16/API 36) for controlling app and website usage. Built with Kotlin, Jetpack Compose, Material 3, Hilt DI, and Room.

## Build Commands
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew clean assembleDebug    # Clean build
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture
- **MVVM + Clean Architecture** with Hilt dependency injection
- **Data layer**: Room database (`AppDatabase`, version 2) with 4 entities: `WebsiteRule`, `AppRule`, `UsageLog`, `BlockEvent` + `AggregatedUsage` query POJO
- **Domain layer**: Use cases for block status checking, rule locking, installed apps
- **UI layer**: Jetpack Compose with 3 tabs (Websites, Apps, Statistics) + Settings as ModalBottomSheet + onboarding + lock screen + block overlay

## Key Mechanisms
- **App blocking**: Accessibility service detects foreground app → checks rules → launches `BlockOverlayActivity` as full-screen overlay. `MonitoringService` polls `UsageStatsManager` every 5s as backup enforcement.
- **Website blocking**: Accessibility service reads browser URL bars (Chrome, Firefox, Samsung Internet, Edge) → checks domain against `BlocklistManager` → launches overlay if blocked. No VPN (user runs Tailscale).
- **Usage tracking**: `MonitoringService` accumulates daily usage in Room DB. Website usage tracked when browser is foreground + URL matches a rule.
- **Self-control**: Active rules are read-only (locked). `IsRuleLockedUseCase` checks schedule windows and daily limit exhaustion.
- **Schedule alarms**: `ScheduleAlarmHelper` schedules `AlarmManager` alarms at block start/end times via `ScheduleReceiver`.
- **App Lock**: Optional master password gates app access. `MasterPasswordManager` stores hashed password in `EncryptedSharedPreferences`. `LockScreenActivity` shown on launch/resume when enabled. `AppLockState.isUnlocked` in-memory flag resets on `onStop()`. `FLAG_SECURE` applied when enabled.
- **Password-guarded rules**: Individual rules can have their own password (`isPasswordProtected`, `passwordHash` columns). Modifications require password entry via `PasswordEntryDialog`. Master password serves as universal override. Password hashing uses PBKDF2WithHmacSHA256 (600k iterations, 32-byte salt).
- **App icons**: Loaded via `PackageManager.getApplicationIcon()` and cached in `AppIconCache` (LruCache). Displayed in `AppRuleCard`, `AddAppRuleSheet`, and `BlockOverlayContent`.
- **Website favicons**: Loaded via Google favicon service (`google.com/s2/favicons`) using Coil `AsyncImage`. Displayed in `WebsiteRuleCard` and `BlockOverlayContent`. Falls back to globe icon on error.

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
├── data/local/      # Room entities, DAOs, database (v2), type converters, AggregatedUsage POJO
├── data/repository/ # Repositories wrapping DAOs
├── domain/model/    # Enums and data classes (BlockType, TargetType, RuleStatus)
├── domain/usecase/  # Business logic (CheckBlockStatus, IsRuleLocked, GetInstalledApps)
├── service/         # Android services (Monitoring, Accessibility, Boot/Schedule receivers)
├── vpn/             # DNS packet processing (legacy, not active — user uses Tailscale)
├── ui/
│   ├── navigation/  # NavGraph (3 tabs + settings ModalBottomSheet)
│   ├── theme/       # Material 3 theme
│   ├── components/  # Reusable composables (BlockOverlayContent, AppIcon, FaviconImage,
│   │                #   PasswordEntryDialog, PasswordProtectionSection, UsageBar,
│   │                #   SummaryCard, DateRangeSelector)
│   ├── screens/
│   │   ├── onboarding/  # OnboardingScreen (7-step permission wizard)
│   │   ├── website/     # WebsiteBlockScreen, WebsiteRuleCard, AddWebsiteRuleSheet
│   │   ├── app/         # AppBlockScreen, AppRuleCard, AddAppRuleSheet
│   │   ├── statistics/  # StatisticsScreen, StatisticsViewModel
│   │   ├── settings/    # SettingsSheet, SettingsViewModel, SetPasswordSheet, DisablePasswordDialog
│   │   └── lock/        # LockScreenActivity, LockScreen, LockScreenViewModel
│   └── util/        # Formatters, AppIconCache
└── util/            # Helpers (PermissionHelper, BrowserUrlParser, ScheduleHelper, etc.)
```

## Important Notes
- **No VPN** — website blocking is accessibility-based only (user runs Tailscale)
- **Crash handler** — `CrashHandlerActivity` runs in `:crash` process, shows full stack traces
- **minSdk 34, targetSdk 36** — can use modern Android APIs freely
- **Personal use app** — no privacy concerns, show everything on crash
- **DB version 2** — migration adds `isPasswordProtected` and `passwordHash` columns to both rule tables
- **Settings is a ModalBottomSheet** — accessible via gear icon in TopAppBar on all tabs, not a separate tab
- **Unused dependencies** — `security-crypto` is now used by `MasterPasswordManager`; `coil-compose` is used by `FaviconImage`
