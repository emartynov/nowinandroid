---
name: convert-to-testballoon
description: >
  Converts JUnit 4/5 and kotlin.test test files to testBalloon DSL format.
  Use this skill whenever the user asks to convert, migrate, or rewrite tests to testBalloon,
  mentions testBalloon in the context of existing tests, or asks how to "balloon-ify" a test file.
  Also trigger when the user pastes a JUnit/kotlin.test class and asks how it would look in testBalloon.
---

# Convert Tests to testBalloon

testBalloon is a Kotlin-first DSL test framework where tests are declared as top-level property
delegations (`val X by testSuite { }`) rather than annotated class methods. The framework handles
coroutines automatically — no `runTest` wrapper needed.

## Conversion workflow

1. Read the source test file(s) the user specified
2. Identify which patterns are present (see Pattern Reference below)
3. Write the converted file — same package, same assertions, restructured declarations
4. Call out any patterns that need human judgment (see Edge Cases)

**Bulk migration (directory or module):** Categorize files first by complexity (simple no-fixture,
fixture-only, fixture + custom rules, abstract base class). Then parallelize across complexity groups —
spawn subagents per group so independent files convert simultaneously. Abstract base class files must
be converted before subclasses (extract the top-level fixture function first).

If the user asks to convert a whole directory, scan and categorize first, then convert in parallel
batches, verify each batch compiles before moving to the next.

## Import changes

Remove all JUnit/kotlin.test imports that become redundant:
```
// REMOVE
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineDispatcher

// ADD
import de.infix.testBalloon.framework.testSuite
// Add only what the converted code actually uses:
// import de.infix.testBalloon.framework.testFixture   (if fixtures used)
// import de.infix.testBalloon.framework.TestConfig     (if config used)
```

Keep all assertion imports unchanged (`kotlin.test.*`, `org.junit.Assert.*`, Kotest, Truth, etc.).

---

## Pattern Reference

### 1. Basic test class

**JUnit:**
```kotlin
class FooTest {
    @Test
    fun some_thing_works() {
        assertEquals(4, 2 + 2)
    }
}
```

**testBalloon:**
```kotlin
val FooTest by testSuite {
    test("some thing works") {
        assertEquals(4, 2 + 2)
    }
}
```

Rules:
- Class name becomes the property name (keep it identical)
- Method name becomes the test string label — convert `snake_case` and `camelCase` to readable
  space-separated words; backtick names stay as-is (strip backticks, keep the string)
- Remove `@Test` annotations

### 2. Per-test setup/teardown (`@Before` / `@After`)

**JUnit:**
```kotlin
class FooTest {
    private lateinit var subject: Foo

    @Before
    fun setUp() {
        subject = Foo()
    }

    @After
    fun tearDown() {
        subject.close()
    }

    @Test
    fun it_works() { subject.doSomething() }
}
```

**testBalloon:**
```kotlin
val FooTest by testSuite {
    testFixture {
        Foo()
    } closeWith {
        close()
    } asParameterForEach { subject ->
        test("it works") { subject.doSomething() }
    }
}
```

- `@Before` body → `testFixture { ... }` initialization block
- `@After` body → `closeWith { ... }` (use `this` = the fixture value)
- Tests that used the field now receive it as a lambda parameter
- If there is no teardown, omit `closeWith`

### 3. Shared setup/teardown (`@BeforeClass` / `@AfterClass`)

Use `asParameterForAll` instead of `asParameterForEach` — one instance shared across all tests.

**JUnit:**
```kotlin
companion object {
    @BeforeClass @JvmStatic fun setUp() { /* ... */ }
    @AfterClass @JvmStatic fun tearDown() { /* ... */ }
}
```

**testBalloon:**
```kotlin
testFixture { /* create */ } closeWith { /* cleanup */ } asParameterForAll { shared ->
    // tests here share the same instance
}
```

### 4. Multiple fixture fields (use context objects)

When `@Before` initializes several fields, use an object expression with `asContextForEach`.
Fields in an `object { }` expression are initialized in declaration order — later fields can
reference earlier ones, so no outer locals are needed:

**JUnit:**
```kotlin
private lateinit var prefs: Prefs
private lateinit var repo: Repo
private lateinit var db: Database

@Before fun setUp() {
    prefs = Prefs()
    db = Database()
    repo = Repo(prefs, db)
}
```

**testBalloon:**
```kotlin
testFixture {
    object {
        val prefs = Prefs()
        val db = Database()
        val repo = Repo(prefs, db)  // references earlier fields directly
    }
} asContextForEach {
    test("something") {
        repo.doThing()  // accessed via implicit this
    }
}
```

**Critical**: Do NOT create outer local variables that are then re-exposed as object fields with
different names. This two-tier aliasing is unnecessary and confusing:

```kotlin
// WRONG — outer local + aliased object field
testFixture {
    val niaPrefs = NiaPreferencesDataSource(...)  // outer local
    object {
        val prefs = niaPrefs  // alias — just use one name!
        val subject = Repo(niaPrefs)
    }
}

// RIGHT — construct directly in the object
testFixture {
    object {
        val prefs = NiaPreferencesDataSource(...)
        val subject = Repo(prefs)
    }
}
```

Outer locals are only justified when the same value is referenced in `closeWith { }` AND in the
object expression (since `closeWith` and the object are peers, not nested).

**Local helpers in `asContextForEach`:** You can define `suspend fun` helpers inside the
`asContextForEach { }` block — they have access to the fixture's fields via implicit `this`:

```kotlin
niaDbFixture() asContextForEach {
    suspend fun insertTopics() {
        topicDao.insertOrIgnoreTopics(listOf(testTopic("1", "compose")))
    }
    test("getTopics") {
        insertTopics()
        assertEquals(listOf("1"), topicDao.getTopicEntities().first().map { it.id })
    }
}
```

### 5. Coroutine tests (`runTest`)

testBalloon gives every test body a `TestScope` automatically. Just remove the `runTest { }` wrapper.

**JUnit:**
```kotlin
@Test
fun loads_data() = runTest {
    val result = subject.loadData()
    assertEquals(expected, result)
}
```

**testBalloon:**
```kotlin
test("loads data") {
    val result = subject.loadData()
    assertEquals(expected, result)
}
```

Also remove `StandardTestDispatcher` / `TestCoroutineDispatcher` fields and their injection into
the subject — testBalloon's coroutine context flows down automatically. If the subject requires an
explicit dispatcher, inject `testScope.coroutineContext` or `UnconfinedTestDispatcher()`.

### 6a. Project-owned rules (`MainDispatcherRule` and similar)

If the project has a custom JUnit `TestWatcher` or `TestRule` that just sets up/tears down state
(like `MainDispatcherRule` which calls `Dispatchers.setMain`), convert it to a `TestConfig` value
in the shared test module rather than wrapping it in `JUnit4RulesContext`:

**JUnit (old):**
```kotlin
@get:Rule val mainDispatcherRule = MainDispatcherRule()
```

**testBalloon (per-suite inline — wrong if used in multiple suites):**
```kotlin
val FooTest by testSuite(
    testConfig = TestConfig.aroundEachTest { action ->
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try { action() } finally { Dispatchers.resetMain() }
    }
) { ... }
```

**testBalloon (right — extract to shared val in your testing module):**
```kotlin
// In core/testing/src/main/.../util/MainDispatcherRule.kt (or a new file)
val mainDispatcherTestConfig = TestConfig.aroundEachTest { action ->
    val dispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(dispatcher)
    try { action() } finally { Dispatchers.resetMain() }
}

// At each call site:
val FooTest by testSuite(testConfig = mainDispatcherTestConfig) { ... }
```

Add imports: `de.infix.testBalloon.framework.TestConfig`,
`kotlinx.coroutines.test.UnconfinedTestDispatcher`, `kotlinx.coroutines.test.setMain`,
`kotlinx.coroutines.test.resetMain`.

### 6b. JUnit 4 Rules (`@get:Rule`)

**JUnit:**
```kotlin
@get:Rule val composeTestRule = createComposeRule()
```

**testBalloon:**
```kotlin
testFixture {
    object : JUnit4RulesContext() {
        val composeTestRule = rule(createComposeRule())
    }
} asContextForEach {
    test("click") {
        composeTestRule.setContent { ... }
    }
}
```

Add import: `import de.infix.testBalloon.framework.JUnit4RulesContext`

Note: Only use this for rules you can't change. Avoid creating new JUnit rules; use fixtures instead.

**Multiple rules with ordering** (e.g., Hilt must inject before Compose creates the activity):
```kotlin
testFixture {
    object : JUnit4RulesContext() {
        val hiltRule = rule(HiltAndroidRule(this), order = 0)
        val composeTestRule = rule(createAndroidComposeRule<HiltComponentActivity>(), order = 1)

        @Inject lateinit var repository: MyRepository
    }
} asContextForEach {
    hiltRule.inject()  // must be called before test code accesses @Inject fields
    test("...") { ... }
}
```

### 6c. Test isolation — mutable state MUST be in `testFixture`

**Critical pitfall:** `testSuite { val x = Mutable() }` creates `x` ONCE during suite registration
and shares it across all tests. Only `testFixture { }` guarantees per-test recreation.

```kotlin
// WRONG — all tests share the same mutable repository
val FooTest by testSuite {
    val repo = TestRepository()  // shared! mutations in test 1 bleed into test 2
    test("a") { repo.emit(1) }
    test("b") { assertEquals(emptyList(), repo.data) }  // may fail
}

// RIGHT — fresh instance per test
val FooTest by testSuite {
    testFixture {
        object { val repo = TestRepository() }
    } asContextForEach {
        test("a") { repo.emit(1) }
        test("b") { assertEquals(emptyList(), repo.data) }  // always passes
    }
}
```

`val`s inside `asContextForEach { }` that are OUTSIDE `test { }` bodies are also created once
at registration time. Immutable data (a `listOf(...)`, a constant) is fine there. Mutable objects
must go in `testFixture { }`.

### 7. Parameterized tests

Also applies to **many near-identical tests** (e.g., same helper, different screen sizes). Replace
them with a `for` loop in the registration block — each iteration registers one `test()` call:

```kotlin
for ((width, height, name) in listOf(
    Triple(400.dp, 400.dp, "compact"),
    Triple(900.dp, 600.dp, "expanded"),
)) {
    test(name) { testScreenshot(width, height, name) }
}
```

**JUnit 4:**
```kotlin
@RunWith(Parameterized::class)
class FooTest(val input: String, val expected: Int) {
    companion object {
        @Parameterized.Parameters @JvmStatic
        fun data() = listOf(arrayOf("a", 1), arrayOf("bb", 2))
    }
    @Test fun length() = assertEquals(expected, input.length)
}
```

**testBalloon:**
```kotlin
val FooTest by testSuite {
    listOf("a" to 1, "bb" to 2).forEach { (input, expected) ->
        test("length of '$input' is $expected") {
            assertEquals(expected, input.length)
        }
    }
}
```

Parameters become plain Kotlin variables in the registration phase (blue code).
Use them to generate `test(...)` calls in a loop.

### 8. Robolectric (`@RunWith(RobolectricTestRunner::class)`)

Remove the annotation — testBalloon's Robolectric integration handles this via its Gradle plugin.
Ensure the module has `de.infix.testBalloon:testBalloon-integration-robolectric` on the classpath.

### 9. Nested test organization

Use nested `testSuite(name) { }` calls to mirror any logical grouping the original had:

```kotlin
val FooTest by testSuite {
    testSuite("when empty") {
        test("returns null") { ... }
    }
    testSuite("when populated") {
        test("returns item") { ... }
    }
}
```

### 10. Private helper functions

Helper methods on the test class move to top-level functions (or companion functions in the
testSuite block if they only make sense there):

```kotlin
val FooTest by testSuite {
    fun makeSubject() = Foo(config = testConfig)  // local helper in registration scope

    test("works") {
        val s = makeSubject()
        // ...
    }
}
```

---

## Edge Cases — flag these for the user

- **`@Rule` with custom rules you own**: prefer rewriting as a testBalloon fixture instead
- **`@ExtendWith` JUnit 5 extensions**: convert to `TestConfig.aroundEachTest { }` decorator
- **Abstract test base classes**: testBalloon doesn't need inheritance — extract shared fixtures
  into top-level functions and call them from each suite
- **`@Ignore` / `@Disabled`**: no direct equivalent; comment out the test or use a `if (false)`
  guard and leave a TODO
- **`@Order`**: not needed — testBalloon runs in source order by default

---

## Kotlin script helper

If the user wants a quick mechanical first-pass on a file, offer to generate a `.kts` script
that handles the purely textual transforms (removing annotations, rewriting the class header,
stripping `runTest` wrappers). The script won't handle fixtures or parameterized tests perfectly,
but it reduces the manual work before you do the semantic conversion.

Example invocation:
```
kotlinc-jvm -script convert_junit.main.kts -- path/to/FooTest.kt
```

Offer to write `convert_junit.main.kts` to the project root or a `scripts/` directory
alongside the tests if the user finds it useful.
