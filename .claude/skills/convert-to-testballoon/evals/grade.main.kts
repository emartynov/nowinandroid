#!/usr/bin/env kotlin

/**
 * Grades converted testBalloon output files.
 * Checks each converted file against a set of pattern assertions and prints a report.
 *
 * Usage:
 *   kotlinc -script grade.main.kts [-- --results-dir <path>] [--run <with_skill|without_skill>]
 */

import java.io.File

val args = args.toList()
val projectDir = File(__FILE__.parent).parentFile.parentFile.parentFile  // evals/../../.. = project root
val resultsDir = args.indexOf("--results-dir").let { i ->
    if (i >= 0 && i + 1 < args.size) File(args[i + 1]) else
        File(projectDir, ".claude/skills/convert-to-testballoon-workspace/iteration-1")
}
val runLabel = args.indexOf("--run").let { i ->
    if (i >= 0 && i + 1 < args.size) args[i + 1] else "with_skill"
}

data class Assertion(val name: String, val check: (String) -> Boolean)

/** Assertions that apply to every converted file. */
val universalAssertions = listOf(
    Assertion("uses testSuite DSL")          { "by testSuite {" in it },
    Assertion("no @Test annotations")        { "@Test" !in it },
    Assertion("no @Before annotations")      { "@Before" !in it },
    Assertion("no @After annotations")       { "@After" !in it },
    Assertion("no runTest wrapper")          { "runTest" !in it },
    Assertion("no junit.Test import")        { "import org.junit.Test" !in it },
    Assertion("no junit.Before import")      { "import org.junit.Before" !in it },
    Assertion("has at least one test()")     { Regex("""test\s*\(""").containsMatchIn(it) },
)

/** Per-eval extra assertions keyed by input file name prefix. */
val perEvalAssertions: Map<String, List<Assertion>> = mapOf(
    "01_coroutine" to listOf(
        Assertion("uses testFixture")                { "testFixture {" in it },
        Assertion("uses asParameterForEach")         { "asParameterForEach" in it },
        Assertion("no StandardTestDispatcher field") { "StandardTestDispatcher()" !in it },
        Assertion("no lateinit vars")                { "lateinit var" !in it },
    ),
    "02_compose" to listOf(
        Assertion("uses JUnit4RulesContext")         { "JUnit4RulesContext" in it },
        Assertion("rule() wraps composeTestRule")    { "rule(createComposeRule())" in it },
        Assertion("no @RunWith annotation")          { "@RunWith" !in it },
        Assertion("no @get:Rule")                    { "@get:Rule" !in it },
    ),
    "03_param" to listOf(
        Assertion("no @RunWith(Parameterized)")      { "Parameterized" !in it },
        Assertion("uses loop or forEach")            { "forEach" in it || "for (" in it },
        Assertion("no companion object")             { "companion object" !in it },
        Assertion("no @Parameterized.Parameters")    { "@Parameterized.Parameters" !in it },
    ),
)

data class EvalResult(
    val evalName: String,
    val assertions: List<Pair<Assertion, Boolean>>,
    val outputFile: File?,
)

println("=== testBalloon Conversion Grader ===")
println("Results dir : $resultsDir")
println("Run         : $runLabel")
println()

val evalDirs = resultsDir.listFiles { f -> f.isDirectory }?.sortedBy { it.name } ?: emptyList()
val results = mutableListOf<EvalResult>()

for (evalDir in evalDirs) {
    val runDir = File(evalDir, runLabel)
    val outputFile = runDir.listFiles { f -> f.extension == "kt" }?.firstOrNull()

    if (outputFile == null) {
        println("⚠ ${evalDir.name}/$runLabel — no output file found, skipping")
        println()
        continue
    }

    val content = outputFile.readText()
    val extra = perEvalAssertions.entries
        .firstOrNull { (prefix, _) -> evalDir.name.startsWith(prefix) }
        ?.value ?: emptyList()

    val allAssertions = universalAssertions + extra
    val checks = allAssertions.map { it to it.check(content) }
    results += EvalResult(evalDir.name, checks, outputFile)

    val passCount = checks.count { it.second }
    val total = checks.size
    val icon = if (passCount == total) "✅" else "❌"
    println("$icon ${evalDir.name}  ($passCount/$total)")

    for ((assertion, passed) in checks) {
        val mark = if (passed) "  ✓" else "  ✗"
        println("$mark ${assertion.name}")
    }
    println()
}

// Summary
val totalChecks = results.sumOf { it.assertions.size }
val totalPassed = results.sumOf { r -> r.assertions.count { it.second } }
val allPassed = totalPassed == totalChecks

println("=== Summary: $totalPassed / $totalChecks assertions passed ===")

// Write machine-readable results
val jsonLines = results.flatMap { r ->
    r.assertions.map { (assertion, passed) ->
        """  {"eval": "${r.evalName}", "assertion": "${assertion.name}", "passed": $passed}"""
    }
}
val jsonOutput = "[\n${jsonLines.joinToString(",\n")}\n]"
File(resultsDir, "grading_${runLabel}.json").writeText(jsonOutput)
println("Grading results written to grading_${runLabel}.json")

if (!allPassed) System.exit(1)
