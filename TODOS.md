# TODOs — Android Doctor

Tracked deferred work. Each item has context so a 3-months-later-you can pick it up.

## Deferred from v1.1.0 review (2026-04-22)

### P2 — Persistent notification mode (iStat-style)
**What:** Optional ongoing notif showing RAM %, CPU temp, live score.
**Why:** Power users want always-visible phone health without opening the app.
**Pros:** Stickiness multiplier, differentiator vs competitors.
**Cons:** Battery drain if polling too often. User opt-in only. Notif channel + foreground service complexity.
**Context:** Consider v1.2.0 after we see retention data from Weekly Notif feature. If Weekly Notif opens <30%, persistent notif may be the fix. Implementation: `Service` with `startForeground()`, refresh every 60s via `Handler`.
**Effort:** M (~45 min CC).
**Depends on:** v1.1.0 ships + retention data.

### P2 — Fix history UI (snapshots exist, no surface)
**What:** UI screen surfacing existing fix rollback snapshots stored as JSON.
**Why:** Trust-builder. "I disabled X 3 days ago — can I undo?"
**Pros:** Near-zero new code. Data already persisted by FixUseCase. Just needs ViewModel + Compose screen.
**Cons:** Snapshot format may need migration if FixUseCase evolves.
**Context:** Check `app/src/main/java/com/androidoctor/data/repository/` for snapshot JSON serializer. Build `FixHistoryScreen` modeled on `BenchmarkScreen`.
**Effort:** S (~20 min CC).
**Depends on:** nothing.

### P2 — Storage forecaster
**What:** "At current growth rate, you'll run out in 23 days. Top space eaters: WhatsApp (4.2GB), Photos (8.1GB)."
**Why:** Actionable storage insight. Current Storage screen only shows % full.
**Pros:** High user impact, linear regression on ScoreSnapshot.storageFreeBytes solves it.
**Cons:** Requires >7 data points to be accurate → new-user empty state.
**Context:** Once longitudinal tracking ships (PR 2 of v1.1.0), add forecaster as a card on Storage detail screen. Use `SimpleRegression` or roll your own least-squares over last 14 snapshots.
**Effort:** S (~25 min CC).
**Depends on:** v1.1.0 PR 2 (Longitudinal tracking).

### P2 — Thermal throttle alert
**What:** Background watcher. If CPU throttled >10 min → notif "Your phone is overheating. Tap to diagnose."
**Why:** Catches performance-killing state users don't notice.
**Pros:** Differentiator. BatteryManager BATTERY_PROPERTY_TEMPERATURE available without Shizuku.
**Cons:** Background watcher = battery cost. Needs careful WorkManager sizing.
**Context:** Hook into WorkManager 15-min periodic tasks. Track temp trend. Fire notif on sustained >42°C.
**Effort:** M (~40 min CC).
**Depends on:** v1.1.0 PR 2 (WorkManager infra).

### P2 — Privacy audit screen
**What:** Scanner showing which apps have dangerous perms they don't need ("Weather app has mic access — revoke?").
**Why:** Expands positioning from "speed" to "control + privacy".
**Pros:** Unique angle. Uses PackageManager.getPermissionInfo — no Shizuku needed.
**Cons:** Per-app perm analysis is noisy. Need curated rule set for "weird combos". Approaches Play Protect territory.
**Context:** Start with 10-15 curated rules (weather+mic, calculator+location, flashlight+contacts). Grow organically.
**Effort:** M (~60 min CC).
**Depends on:** nothing.

### P3 — Remote Config for feature flags
**What:** Firebase Remote Config integration for gradual feature rollout.
**Why:** Lets us ship features gated + enable per-% of users.
**Pros:** Free (part of Firebase suite already integrated). Rollback without Play release.
**Cons:** Adds network dependency. v1.1.0 features are small enough to not need.
**Context:** Integrate when first >1000 users, before shipping any feature that could controversially break.
**Effort:** S (~20 min CC).
**Depends on:** >1000 installs.

### P3 — Widget (home screen)
**What:** Resizable widget showing current score + last diagnose time.
**Why:** Highest-visibility Android surface. Always-on reminder.
**Pros:** Marketing screenshot gold. Low implementation cost.
**Cons:** Widget refresh is restricted post-Android 12 (Doze). Must handle stale data UI.
**Context:** Use Glance (Jetpack Compose widgets). Target 2x2 and 4x1 sizes.
**Effort:** M (~45 min CC).
**Depends on:** v1.1.0 (score data available).

### P3 — Speed Test Mode
**What:** Dedicated perceived-perf test: cold app launch, web scroll frames, camera open latency.
**Why:** Benchmark-style flex + shareable score.
**Pros:** Viral potential via shareable result card.
**Cons:** Harder than storage/memory benchmarks — needs UI automation via Accessibility API.
**Context:** Use `AccessibilityService` to launch common apps (Chrome, Camera, Gallery) and measure first-frame time. Compare to cohort baseline.
**Effort:** L (~90 min CC).
**Depends on:** v1.1.0 shipped.

### P3 — Safe Mode Boost (gaming pre-flight)
**What:** One-button pre-game optimization: kill background apps, disable animations, lock CPU governor (Shizuku-only).
**Why:** Gamers are highest-LTV segment.
**Pros:** Direct, measurable performance gain users feel.
**Cons:** Requires Shizuku. Governor tweaks dangerous if wrong values.
**Context:** Whitelist known-safe governor values per Snapdragon/MediaTek family. Run for N minutes then auto-revert.
**Effort:** M (~60 min CC).
**Depends on:** Shizuku docs improvement (users need clear setup flow first).

### P3 — Wear OS companion
**What:** Score glanceable on watch face.
**Why:** Multiplies surface area.
**Pros:** Nobody else does this.
**Cons:** Wear OS dev is niche. Tiny user overlap today.
**Context:** Only pursue if install base crosses 10k AND Wear OS data shows >5% of users wear one.
**Effort:** L (~2h CC).
**Depends on:** product-market fit signal first.

---

**Process:** When picking up a TODO, move it to the top of its priority section with `[IN PROGRESS]` tag. When done, remove from file and add line to CHANGELOG.
