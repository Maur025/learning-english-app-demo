# AGENTS.md

Offline-first Android English-learning app (Kotlin + Jetpack Compose + Material 3). `README.md` is the full product/technical spec (~90 numbered sections, ~3,400 lines) and the authority on architecture, scope, and roadmap — consult it before any non-trivial design decision.

## Current state

- Phases 0–7 done: scaffold + navigation skeleton, pure domain model, Room/DataStore persistence, the content system (versioned JSON packs, parser, validator, idempotent importer, 56 bundled expressions), the SM-2 scheduler, the exercise engine (generator + answer evaluator), the learning engine (session planner, state progressor, `LearningEngine`) and the first usable screens (onboarding, Home and the practice placeholder).
- The practice screen is a stub: exercises, rating and session summary arrive in phase 8.
- Work through the phased roadmap (README §73), one small vertical slice at a time (README §84–85), keeping the build green after each step.
- Open decisions to validate against this toolchain before adopting: DI approach (README §47), module split (README §15). Room landed on 2.8.5 (README §10). Record all versions in `gradle/libs.versions.toml`.

## Learning engine

- `domain/engine/DailySessionPlanner` is pure: expressions + states + preferences + `now` in, `SessionPlan` out. It picks candidates in priority order (overdue, weak, production recall, new), caps new content when reviews pile up, and fills the daily budget by skipping steps that do not fit instead of truncating the tail. It validates each step with `ExerciseGenerator` because a type the content cannot support is not a valid step.
- `domain/engine/LearningStateProgressor` is applied by `ReviewRecorder` on the state the scheduler already programmed, so `reviewCount` is counted and the interval is untouched. Scores use an exponential moving average because the model stores no per-axis counter; only the axis of the exercise moves, so recognition can never dilute production.
- `ReviewRecorder` is the single write path for learning state. `DefaultLearningEngine` only loads, connects and persists.
- `learning_sessions.currentPosition` is the index of the next unanswered exercise, so an interrupted session resumes exactly where it stopped. Additive migration `V1_TO_V2`; never add a destructive one.
- `now`/timestamps are always explicit parameters — no injected `Clock`.

## Screens and ViewModels

- Every screen is stateless (`ui/<feature>/XScreen` takes a state object plus callbacks) and every state object comes from one ViewModel (`XUiState`, `XEffect`). Routes in `AppNavHost` only observe the ViewModel, paint the state and execute the effects — no domain decision lives in the nav layer.
- `ui/AppViewModelFactory` builds ViewModels with `viewModelFactory { initializer { … } }` and resolves `AppContainer` through `CreationExtras.appContainer` (`di/CreationExtrasExt.kt`). It lives in `ui`, not `di`, so the object graph never references screens. Screens never receive the container as a parameter.
- One-shot navigation uses `Channel<Effect>(BUFFERED).receiveAsFlow()`, never a field in the state: an effect must not be replayed on rotation.
- Double-tap guards are raised **synchronously** before launching the coroutine (`isStartingSession`/`isSaving`); reading them inside the coroutine would let a second tap through in the same frame and create two sessions.
- `StartDestinationViewModel` resolves `onboardingCompleted` once per process, and the nav graph is not drawn until it answers, so a returning user never sees onboarding again.
- `HomeViewModel` takes `now: () -> Long` (default `System::currentTimeMillis`) and re-reads it on every subscription, so opening Home recomputes what is due. There is no timer.
- Home counters are SQL aggregates (`observeDueCount`, `observeNewCount`, `observeSkillAverages`, `observeInProgress`), not in-memory folds. The Room repositories return 0/empty for an empty pack selection because `IN ()` is invalid SQL. `SkillAverages` percentages are `null` until the first review: an average over nothing is not 0 %.
- Pack selection lives in `UserPreferences.selectedPackIds(installed)` (empty preference = all installed), shared by the planner and Home so the counters and the generated session always describe the same content.
- Content installs in the background at startup, so onboarding and Home show a loading state while no pack is installed yet.
- Deferred to phase 8: the "minutes studied today / goal" progress bar. `SessionStep.estimatedSeconds` is not persisted and `reviews.responseTimeMs` only exists once answers are registered, so any bar today would show invented numbers.
- In tests, build ViewModels **inside** the test body, never as class fields: `viewModelScope` captures `Dispatchers.Main` at construction and `MainDispatcherRule` injects the test dispatcher afterwards, so a ViewModel created in the test constructor stays bound to the real main thread and its coroutines never run.
- `StateFlow.first()` returns the current value without waiting; use `first { predicate }` in tests or you end up asserting the `initialValue`.

## Content packs

- Bundled packs live in `app/src/main/assets/content/` and are listed in `BundledContent.FILES`; `BundledContentInstaller` runs them at startup, off the main thread.
- JSON schema is version 1 (`ContentPackFile`): `pack` metadata + `expressions` with `patterns`, `examples` and `tags`. `difficulty` is `EASY`/`MEDIUM`/`HARD` and `level` is `A1`–`C2`.
- The schema is validated before importing: `ContentPackValidator` returns all issues with their JSON path instead of failing on the first one. `BundledContentTest` fails the build if a bundled pack is invalid or if the total expression count leaves the 50–100 range (README §3 Phase 3 deliverable).
- `RoomContentImporter` skips a pack whose installed `version` is greater or equal, and rewrites examples, patterns and tag links on every import, so re-importing converges instead of duplicating. Expression ids are namespaced as `<packId>/<localId>` because they are Room primary keys.
- Add new content by editing the JSON and bumping `pack.version`; never by inserting rows from code.

## Build and verify

- `./gradlew assembleDebug` — build (requires Android SDK via `local.properties`)
- `./gradlew test` — all JVM unit tests
- `./gradlew :app:testDebugUnitTest --tests "com.example.learning.english.learning.english.app.ExampleUnitTest"` — single test class
- `./gradlew :app:lintDebug` — Android Lint (no ktlint/detekt configured)
- `./gradlew :app:connectedDebugAndroidTest` — instrumented tests (require emulator/device)
- Always use the wrapper: Gradle 9.6.0, daemon JVM pinned to JDK 25 in `gradle/gradle-daemon-jvm.properties` (generated by `updateDaemonJvm` — don't hand-edit). Foojay resolver auto-provisions JDKs.
- Configuration cache is on; if a build-script change breaks it, rerun with `--no-configuration-cache` to see the underlying error.

## Toolchain gotchas (AGP 9.4.1)

- Only `com.android.application` + `org.jetbrains.kotlin.plugin.compose` + `org.jetbrains.kotlin.plugin.serialization` plugins are applied. AGP 9 ships built-in Kotlin (KGP 2.2.10) — adding `org.jetbrains.kotlin.android` breaks the build. KSP and the serialization plugin share the `kotlin` version ref: move them together or the build breaks.
- The build uses the new AGP 9 DSL (`compileSdk { version = release(37) }`, `optimization { enable = false }`). Don't rewrite it to legacy `compileSdk = 37` / `isMinifyEnabled` syntax.
- `settings.gradle.kts` sets `FAIL_ON_PROJECT_REPOS` — never add repositories in module build scripts; declare dependencies via the version catalog.

## Package naming mismatch

The actual namespace/applicationId/package is `com.example.learning.english.learning.english.app` (duplicated template artifact), but README §16 suggests `com.example.englishtrainer`. Place new code under the existing package; don't rename or create divergent packages — flag the mismatch instead.

## Hard constraints (from README — do not violate)

- **Offline-first**: no backend, auth, accounts, network, or online LLM dependency for any V1 feature. Full exclusion list in README §9.2.
- **Single activity**: `MainActivity` + Compose Navigation only; never one Activity per screen.
- **Pure domain layer**: no Room/Android annotations in domain models; map Room entity → domain model → UI state. No DAO access from composables.
- **Room is the source of truth** for learning data; DataStore only for small preferences; no custom cache layers. No destructive migrations once real learning data exists.
- **`Expression` (not `Word`) is the core entity**; recognition and production are tracked as separate scores; never mark an expression mastered from recognition-only success.
- **Seed import must be idempotent** — no duplicate expressions on app restart.
- Don't implement future phases (listening, speaking, AI) while the vocabulary loop is incomplete.
- Use domain naming from README §78 (`LearningState`, `ReviewScheduler`, `LearningEngine`, …); avoid `Utils`/`Manager`/`Helper`.
- Small conventional commits (`feat:`, `chore:`, … — README §79). When a decision is unclear, prefer the simpler implementation (README §86).
