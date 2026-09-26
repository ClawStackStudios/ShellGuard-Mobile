# Decisions Made
Structured record of meaningful choices. Each entry captures the
context, options, choice, and felt reason. Append-only.
This is the auditable companion to myStory.md.
The story says how it felt. This says what was actually chosen and why.

## isolate-robolectric-sdk-ceiling — 2026-09-26 10:30

**Context**: GitHub Actions CI runner failed during `testDebugUnitTest` with `UnsupportedOperationException: DefaultSdkProvider.java:170` due to `targetSdk = 36`.
**Options considered**:
- Downgrade `targetSdk` to 34 across `app/build.gradle.kts` — Would solve the test failure immediately, but forfeits Android 16 Baklava compliance required by Google Play Console release standards.
- Decouple host JVM test execution by pinning `sdk=34` in `app/src/test/resources/robolectric.properties` and annotating tests with `@Config(sdk = [34])` — Preserves targetSdk 36 for packaging while stabilizing test execution.
**Chosen**: Decouple test execution via `sdk=34` in `robolectric.properties`.
**Why**: It felt wrong to let test framework immaturity downgrade the application's actual platform target. The build should aim as high as the platform allows, while tests run where the ground is stable.
**Confidence**: high — completely green in CI and confirmed on live release `v0.0.0.3`.
**Outcome**: GitHub Actions release workflow passed 100% green and successfully built and signed release bundles.
**Pattern reference**: New pattern — accumulating, weight 2/3.

## gate-promotions-at-three-validations — 2026-09-26 11:06

**Context**: During the first dream cycle, multiple recent discoveries (Base62 sovereign keys, Robolectric SDK 34, IME inset isolation) emerged with high emotional salience.
**Options considered**:
- Promote all recent breakthrough fixes immediately to `long-term/` — Captures fresh insights, but risks polluting long-term memory with single-session fixes.
- Strictly enforce the 3-validation rule across independent sessions, promoting only cleartext LAN transport and CWE-359 clipboard masking while holding single-session fixes as accumulating — Prevents recency bias from diluting crystallized long-term memory.
**Chosen**: Strictly enforce the 3-validation threshold.
**Why**: A scar requires time and pressure to form. Promoting a fix after a single victory turns the long-term bank into another changelog rather than crystallized truth.
**Confidence**: high — maintains strict epistemic separation between temporal and long-term memory.
**Outcome**: Long-term bank gained two truly ratified entries while three entries were logged as accumulating (weight 2/3).
**Pattern reference**: `long-term/constraints.md § constraint: cleartext-lan-and-tailscale-transport`.

## reconcile-contradictions-in-place-during-dream — 2026-09-26 11:08

**Context**: Dream ingestion discovered `progress.md` header claimed Phase 1 transition status while Phase 2 was already released, and `changelog.md` lagged behind `RELEASE-v0.0.0.3.md`.
**Options considered**:
- Flag contradictions in `dreamLearnings.md` and leave source files untouched — Safest, but leaves known inaccuracies lingering in active documents.
- Flag contradictions in the dream receipt and immediately reconcile the source document headers to match physical reality — Eliminates cognitive drag without modifying code or architecture.
**Chosen**: Reconcile source document headers to match physical release reality.
**Why**: Leaving known documentation drift unresolved when the reality is undeniable creates cognitive drag for future sessions.
**Confidence**: high — verified against live published release tag `v0.0.0.3`.
**Outcome**: `progress.md` and `changelog.md` now accurately reflect Phase 2 completion and transition to Phase 3.
**Pattern reference**: New pattern — first instance.
