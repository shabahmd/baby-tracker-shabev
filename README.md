# Nestling

**The baby tracker that can't lose your data.**

No account. No cloud. No ads. No analytics SDK. Nestling does not declare the `INTERNET`
permission — and a unit test plus a CI gate fail the build if it ever appears.

## The one job

> Log a feeding, sleep, or diaper event in under 5 seconds, half-asleep, one-handed, at 3am.

Everything in the app is measured against that sentence. If a feature does not serve
tired-parent-at-night, it isn't here.

## What it does

| | |
| --- | --- |
| **One-tap logging** | Three big tonal buttons on the Today screen — Bottle, Sleep, Diaper. Bottle/Sleep start a live timer; tapping stop saves. Diaper saves instantly after the type. |
| **Today timeline** | Reverse-chronological list of today: time, event, amount, duration chip, "25 min ago" overline. |
| **Side + amount** | One segmented row (Left / Right / Both) plus an amount stepper, offered *after* the event is already saved. |
| **History + search** | Day-by-day pager and a plain text search across everything ever logged. |
| **CSV export** | The whole log to a file, handed to the system share sheet. For the pediatrician, and because the data is yours. |
| **1×1 widget** | The three buttons on the home screen. Bottle/Sleep toggle the timer without opening the app. |

Deliberately **not** here: accounts, cloud sync, growth charts, photos, reminders,
multi-baby, milestones, AI advice. Those are the things that turn a 5 second log into a
subscription funnel.

## Interaction budget (measured in taps, from Today)

| Event | Taps |
| --- | --- |
| Sleep | 2 — Sleep, then Stop |
| Bottle | 2 — Bottle, then Stop (reuses the last amount; the snackbar offers "Edit") |
| Diaper | 2 — Diaper, then Wet / Dirty / Both |

`TodayScreenTest` asserts each of these.

## Design

- **Material 3**, `Theme.Material3.DayNight.NoActionBar`, dynamic colour via both
  `DynamicColors.applyToActivitiesIfAvailable` (views/splash) and
  `dynamicLightColorScheme` / `dynamicDarkColorScheme` (Compose).
- **True-black night mode** — AMOLED black surfaces with dim warm accents instead of
  blue light, offered automatically once after 10pm. Contrast against `#000000`:
  body text 14.6:1, secondary text 8.6:1, accent 8.9:1 (target was 4.5:1).
- **Type**: the default M3 scale, no custom fonts. Event times in `titleLarge`, amounts
  in `headlineSmall` with `tnum` tabular figures so numerals never jitter.
- **Layout**: one top app bar, a bottom bar with exactly two destinations, a
  centre-docked FAB. Event buttons are 104dp tall against a 56dp minimum target.
- **Rhythm**: a single 16dp gutter, 8dp between rows, 24dp between sections. Rows are
  separated by whitespace and a soft container — never a divider per row.
- **Motion**: M3 emphasized easing on the sheet and the FAB morph only, capped at 260ms.
- **States**: a real empty state with an illustration, errors as snackbars, and
  **no pull-to-refresh** (local data is always fresh; CI fails if it is added).
- **Accessibility**: content descriptions on every icon control, and a test that renders
  the timeline at 1.3× font scale.

## Correctness

The failure mode this app exists to avoid is *losing a parent's log*.

- Every write is a Room `@Transaction`. Stopping a timer inserts the event and clears the
  running timer atomically — you can never end up with both or neither.
- The running timer lives in the database, written on start and on **every minute
  boundary** by a foreground service. Elapsed time is always derived from the stored
  start time, so rotation, app switching and process death all land on the same number.
- `NestlingDatabase` has **no** `fallbackToDestructiveMigration`. A broken migration must
  be loud, not silently erase a year of feeds.

## Tests → acceptance criteria

| Test | Criterion |
| --- | --- |
| `TimerRestoreTest` | Kill the process mid-timer → relaunch restores the running timer with the correct start time; ticks persist; stop is atomic. |
| `CsvExportTest` | The export contains **every** event ever logged (count and id match), including in the written file. |
| `NoInternetPermissionTest` | `INTERNET` (and every other network/tracking permission) is absent from the merged manifest; only three local permissions are declared. |
| `NavigationGraphTest` + `TodayScreenTest` | No login/paywall route exists in the nav graph, and no screen shows sign-in words. |
| `TodayScreenTest` | Two-tap logging for all three types, 56dp minimum targets, real empty state. |
| `DesignRhythmTest` | 16dp gutter, 8dp whitespace between rows, no per-row dividers, tabular numerals, legible at 1.3× font scale. |
| `NestlingViewModelTest` | Timer toggling, switching type mid-timer without losing the first event, amount memory, search, night-mode prompt rules. |
| `TimeFormatTest` | The strings a parent reads at 3am. |

CI additionally greps the source for networking, accounts and pull-to-refresh, and runs
`aapt2 dump permissions` over the built APKs.

## Build

```bash
./gradlew assembleDebug          # APK
./gradlew testDebugUnitTest      # the suite above
./gradlew lintDebug
```

- Kotlin, single module, Room, DataStore, Compose, Glance. No backend, no Firebase, no
  analytics.
- minSdk 26, targetSdk 36, JDK 21, AGP 9.3.2 / Gradle 9.7.1.

GitHub Actions (`.github/workflows/android.yml`) runs the whole thing on every push and
uploads the debug + release APKs.

## Play listing copy (the permissions *are* the marketing)

> Nestling asks for three permissions: notifications, and the two that let a feeding
> timer keep running while your phone sleeps. It cannot access the internet — the
> permission isn't in the app. Your baby's log never leaves your phone, and you can
> export all of it to a CSV file at any time, without an account.
