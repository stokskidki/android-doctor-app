# Changelog

## 1.2.0 (2026-04-22) — Non-tech users + Shizuku walkthrough

### For non-technical users (zero-setup tier)
- **Heavy Apps screen** — "Which apps used the most time in last 7 days." Uses `UsageStatsManager` (user-grantable Usage Access perm, NO Shizuku). Tap any app → deep-links to its Settings info page. Color tiers: red (>20h), orange (>10h), yellow (>5h), green. Positioned on Home: "no Shizuku needed."
- **Manual debloat fallback** — When Shizuku absent, Bloatware screen shows "Open in Settings →" button per app (deep-link to `ACTION_APPLICATION_DETAILS_SETTINGS`). User taps Disable in the system UI. Works for ANY user.

### Shizuku in-app walkthrough
- New **ShizukuSetupScreen** — 5 guided steps. Status banner at top (green/yellow/yellow). Each step has Settings deep-link button.
- **Copy-to-clipboard ADB command** — tap code box, clipboard populated, toast confirms. Removes friction for tech users who have a PC.
- **Wireless ADB alternative** — explicit instructions for Android 11+ users with no PC.
- **Recheck button** in step 5 — polls Shizuku binder.
- All "Shizuku not running" banners in Fix + Battery now navigate to this internal screen instead of opening an external URL.

### Country-specific bloatware
- New catalogs: `country-in.json` (Jio, Airtel, Vi, Flipkart, PhonePe, Paytm — 11 entries), `country-br.json` (Claro, Vivo, TIM — 5 entries), `country-id.json` (Telkomsel, Indosat, Tokopedia, Shopee, Gojek — 5 entries).
- Country detected via `TelephonyManager.networkCountryIso` with `Locale.country` fallback.
- Catalogs merged on top of OEM + generic catalogs.

### Under the hood
- New `UsageStatsParser` with `hasUsageAccess` perm check + `topHeavyApps(days, limit)`.
- New `HeavyAppsViewModel` + `HeavyAppsScreen`.
- New `ShizukuSetupViewModel` + `ShizukuSetupScreen`.
- `BloatwareCatalogResult` adds `country` field.
- `BloatwareCatalogLoader.detectCountry()` added.
- `ShizukuSetupLink` component takes optional `onOpen` callback for in-app navigation.
- `PACKAGE_USAGE_STATS` permission declared.

## 1.1.1 (2026-04-22) — Fixes + Crazy Methods

### Bug fixes
- **Share report card** now shows error toast on failure instead of silent no-op. Crashlytics records failure reason.
- **Fix screen** now detects Shizuku availability. Shows banner + setup link when absent. Fix buttons disabled without Shizuku so users don't tap and get silent no-ops.
- **Per-app battery copy** changed from "Requires Android 12+ or Shizuku" (misleading) to "Requires Shizuku (setup once via ADB)" with setup link. Real requirement made explicit.

### New features — "crazy but meaningful" methods
- **⚡ Panic Boost button** — one tap runs: kill-all cached processes + pm trim-caches max + drop kernel caches + zero animations. Reports counts in result card.
- **CPU Performance Boost** — toggle writes `performance` governor to all CPUs. Visible indicator. Reversible via Rollback.
- **Storage TRIM** — added to Moderate + Aggressive fix levels. `fstrim -v /data` recovers write speed on worn eMMC/UFS.
- **ART re-optimization** — Aggressive only. `cmd package compile -m speed -f` on top launched apps. Cuts cold-start time.

### Technical
- New FixRepository APIs: `fstrimData`, `killAllApps`, `dropKernelCaches`, `reoptimizeTopApps`, `setCpuGovernor`, `getCurrentCpuGovernor`, `panicBoost`.
- New use cases: `PanicBoostUseCase`, `CpuBoostUseCase`.
- Rollback extended to handle `cpu_governor` action type.
- New `ShizukuSetupLink` component surfaces shizuku.rikka.app guide.
- `ShellExecutor.isShizukuAvailable` now exposed to Fix ViewModel.

## 1.1.0 (2026-04-22) — Performance Intelligence

Single-release rollout turning Android Doctor from a one-shot diagnose tool into a habitual health app.

### New features
- **Per-app battery attribution** — See exactly which apps drained your battery (YouTube 18%, Chrome 12%). Native API 31+ or Shizuku dumpsys fallback for Android 8-11.
- **Longitudinal score tracking** — Every diagnose saves a snapshot. New Trends screen shows 7d / 30d / 90d score history as a line chart. Answers "is my phone getting worse?"
- **Weekly health notification** — Reminds you every 7 days. Smart variant rotation: trend alerts, issue counts, free-up-space prompts, or praise when stable.
- **Quick Settings tile** — One-tap glance at your score from any screen. Tap to open app.
- **OEM bloatware catalog** — Curated lists for Samsung, Xiaomi, OnePlus, OPPO, Realme, and generic bloat. Per-app RAM and battery impact. One-tap disable via Shizuku.
- **Shareable report card** — After diagnose, generate a 1080×1920 PNG report and share via any app.

### Improvements
- Room database exports schema and migrates v1→v2 preserving benchmark history (previously would have wiped data on upgrade).
- Firebase Analytics + Crashlytics wired for install attribution and crash monitoring.
- `versionName` in Home footer now reads from BuildConfig instead of a hardcoded string.
- Added `buildConfig = true` build feature for dynamic BuildConfig access.

### Technical
- WorkManager + Hilt worker integration for scheduled diagnostics.
- kotlinx.serialization JSON for bloatware catalog (no extra 350KB YAML parser).
- TileService registered with BIND_QUICK_SETTINGS_TILE permission.

### Under the hood
- New permissions: POST_NOTIFICATIONS (API 33+), BATTERY_STATS (Shizuku-granted).
- New Room tables: `score_snapshots` (v2 schema).
- Firebase project: `android-doctor-17709`.

## 1.0.0 (2026-03-28)

Initial release. Diagnose (battery, storage, RAM, CPU, bloatware), Fix (safe/moderate/aggressive), Benchmark, Battery detail.
