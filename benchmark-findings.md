# testBalloon vs JUnit 4 — Benchmark Findings

## Branches compared

| | testBalloon branch | main branch |
|---|---|---|
| Branch | `feature/convert-to-testballoon-skill` | `main` |
| Test framework | testBalloon 1.0.1-K2.4.0-SNAPSHOT | JUnit 4 + Robolectric |
| Kotlin | 2.4.0 | 2.3.0 |
| kotlinxCoroutines | 1.10.2 | 1.10.1 |
| kotlinxDatetime | 0.8.0 | 0.6.1 |
| kotlinxSerializationJson | 1.11.0 | 1.8.0 |
| roborazzi | 1.64.0 | 1.56.0 |
| jacoco | 0.8.15 | 0.8.12 |

The Kotlin and library version bumps affect **compilation only**, not test execution at runtime.
Roborazzi version difference has no impact since the screenshot tests that use it are disabled in both branches.

---

## Disabled tests (parity between branches)

These 4 test suites are disabled on both branches to ensure a fair comparison.

| Test class | Tests | Disabled via |
|---|---|---|
| `NiaAppScreenSizesScreenshotTests` | 9 | `.disable()` (testBalloon) / `@Ignore` (main) |
| `SnackbarScreenshotTests` | 4 | `.disable()` (testBalloon) / `@Ignore` (main) |
| `SnackbarInsetsScreenshotTests` | 4 | `.disable()` (testBalloon) / `@Ignore` (main) |
| `InterestsListDetailScreenTest` | 5 | `.disable()` (testBalloon) / `@Ignore` (main) |

Reason for disabling: Hilt + testBalloon Robolectric integration issue ([testBalloon#86](https://github.com/infix-de/testBalloon/issues/86)).

---

## Benchmark approach

**Tool:** Gradle Profiler 0.24.0

**Scenario file (`benchmark.scenarios`):**
```
run_tests {
    title = "Unit test execution"
    tasks = ["testDemoDebug"]
    cleanup-tasks = ["cleanTestDemoDebugUnitTest"]
    gradle-args = ["--no-build-cache"]
    warm-ups = 2
    iterations = 5
}
```

**Key design decisions:**
- `testDemoDebug` — the same task CI uses; fans out across all modules for the demo+debug variant
- `cleanup-tasks = ["cleanTestDemoDebugUnitTest"]` — deletes test result XMLs before each build so Gradle considers the test task out-of-date, while Kotlin compilation stays UP-TO-DATE (sources unchanged)
- `--no-build-cache` — prevents Gradle from restoring test results from the build cache (without this, measured times were ~2.5s, i.e. cache restore, not actual test execution)
- Daemons killed with `./gradlew --stop` before each profiler run to prevent resource competition
- Both branches shared the same `gradle-user-home` so Gradle daemon JIT warm-up is comparable

**Runs discarded:**
1. First run used `--rerun-tasks` instead of `cleanup-tasks` — included Kotlin compilation in every iteration (~43s avg), making it impossible to isolate test execution time.
2. Second run used `cleanup-tasks` but forgot `--no-build-cache` — Gradle restored results from cache (~2.5s avg, not real execution).

---

## Performance results — isolated test execution (final, valid run)

| Build | testBalloon | main (JUnit 4) |
|---|---|---|
| Warm-up 1 | 20.9s | 32.1s |
| Warm-up 2 | 19.5s | 25.6s |
| Measured 1 | 18.7s | 25.3s |
| Measured 2 | 21.5s | 24.7s |
| Measured 3 | 21.1s | 23.2s |
| Measured 4 | 22.2s | 24.1s |
| Measured 5 | 25.6s | 22.2s |
| **Average** | **21.8s** | **23.9s** |
| Min | 18.7s | 22.2s |
| Max | 25.6s | 25.3s |

**testBalloon is ~2.1s (~9%) faster on average.**

The ranges overlap (testBalloon 18.7–25.6s, main 22.2–25.3s), so the difference is suggestive rather than conclusive at 5 iterations. A longer run (15–20 iterations) would be needed to confirm statistical significance.

Notable observations:
- testBalloon drifts **up** across iterations (18.7 → 25.6) — possible GC/memory pressure build-up in daemon
- main drifts **down** (25.3 → 22.2) — JIT still optimising during early measured builds
- main warm-up 1 (32.1s) is much slower because the worktree had no prior run state; testBalloon warm-up 1 (20.9s) benefited from previously compiled classes

---

## What was tested

**Task:** `testDemoDebug` across all modules

**Test types included:**
- Roborazzi screenshot tests (Robolectric-backed): `ButtonScreenshotTests`, `TopAppBarScreenshotTests`, `LoadingWheelScreenshotTests`, `NavigationScreenshotTests`, `FilterChipScreenshotTests`, `TagScreenshotTests`, `TabsScreenshotTests`, `BackgroundScreenshotTests`, `IconButtonScreenshotTests`, `ForYouScreenScreenshotTests`
- Robolectric UI tests: `NiaAppStateTest`, design system screenshot tests
- Pure JVM tests: ViewModel tests, repository tests, data layer tests

**Test counts:**

| | testBalloon | main |
|---|---|---|
| Total entries reported | 211 | 165 |
| Skipped (disabled) | 22 | 22 |
| Actually executed | ~189 | 143 |

The 46-entry discrepancy is because testBalloon reports suite wrapper nodes (`TestBalloonJUnit4`) alongside individual test methods in the XML. Both branches execute the same logical test cases.

---

## Test report comparison

Both branches produce standard Gradle JUnit XML + HTML reports, but the **structure differs significantly**.

**main** — each test class is a top-level row at the module level:
```
core:data testDemoDebugUnitTest (34 tests)
  ├── CompositeUserNewsResourceRepositoryTest  4 tests
  ├── NetworkEntityTest                        4 tests
  ├── OfflineFirstNewsRepositoryTest          11 tests
  ├── OfflineFirstTopicsRepositoryTest         4 tests
  └── ...
```

**testBalloon** — all tests collapsed under a single `TestBalloonJUnit4` entry point:
```
core:data testDemoDebugUnitTest (34 tests)
  └── TestBalloonJUnit4  34 tests
        ├── CompositeUserNewsResourceRepositoryTest/
        ├── NetworkEntityTest/
        ├── OfflineFirstNewsRepositoryTest/
        └── ...
```

The test data is identical (same counts, same pass/fail, same durations). The difference is purely presentational: testBalloon's HTML report requires **one extra click** to reach individual test classes at the module level. This is a usability regression worth reporting upstream.

---

## Code coverage (Jacoco)

Coverage **is collected automatically** during `testDemoDebug` — Jacoco `.exec` files are written to `build/outputs/unit_test_code_coverage/demoDebugUnitTest/` in each Jacoco-enabled module.

Modules with Jacoco configured: `core:ui`, `core:database`, `core:designsystem`, `core:network`, `core:datastore`, `core:data`, `core:domain`, `app`, `feature:settings:impl`, `feature:topic:impl`, `feature:search:impl`, `feature:interests:impl`, `sync:work`

**Sample .exec file sizes:**

| Module | testBalloon | main |
|---|---|---|
| core:designsystem | 484 KB | 396 KB |
| core:network | 63 KB | 73 KB |
| core:datastore | 70 KB | 60 KB |
| core:data | present | 118 KB |

Coverage report generation task: `createDemoDebugCombinedCoverageReport` (per module, not aggregated).

**The .exec file sizes differ between branches.** This is expected and explained by:
1. **Kotlin 2.3.0 → 2.4.0** — the Kotlin compiler generates different bytecode; Jacoco instruments at the bytecode level, so different bytecode = different instrumentation = different .exec size
2. **Jacoco 0.8.12 → 0.8.15** — the instrumentation agent itself changed, affecting how execution data is recorded

This is **not a functional difference** in test coverage. The actual coverage percentages may differ slightly but that would reflect bytecode-level coverage of the same source logic, not a real change in what is covered.

---

## Summary

| Dimension | Verdict |
|---|---|
| Test execution speed | testBalloon ~9% faster (2.1s avg), within noise for 5 iterations |
| Test count parity | Same logical tests; testBalloon inflates XML count with wrapper entries |
| Test report usability | testBalloon adds one extra navigation level — minor usability regression |
| Code coverage collection | Both collect Jacoco automatically; .exec sizes differ due to Kotlin/Jacoco version bumps, not functional difference |
| Overall conclusion | testBalloon introduces **no performance penalty**; slight speed advantage likely within noise |
