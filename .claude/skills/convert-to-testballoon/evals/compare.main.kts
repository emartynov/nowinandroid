#!/usr/bin/env kotlin

/**
 * Side-by-side comparison of with_skill vs without_skill (or old_skill) conversion results.
 * Reads grading JSON files and prints a delta table.
 *
 * Usage:
 *   kotlinc -script compare.main.kts [-- --results-dir <path>]
 */

import java.io.File

val args = args.toList()
val projectDir = File(__FILE__.parent).parentFile.parentFile.parentFile  // evals/../../.. = project root
val resultsDir = args.indexOf("--results-dir").let { i ->
    if (i >= 0 && i + 1 < args.size) File(args[i + 1]) else
        File(projectDir, ".claude/skills/convert-to-testballoon-workspace/iteration-1")
}

data class AssertionRow(val eval: String, val assertion: String, val passed: Boolean)

fun loadGrading(file: File): List<AssertionRow> {
    if (!file.exists()) return emptyList()
    val rows = mutableListOf<AssertionRow>()
    val pattern = Regex(""""eval"\s*:\s*"([^"]+)"\s*,\s*"assertion"\s*:\s*"([^"]+)"\s*,\s*"passed"\s*:\s*(true|false)""")
    pattern.findAll(file.readText()).forEach { m ->
        rows += AssertionRow(m.groupValues[1], m.groupValues[2], m.groupValues[3] == "true")
    }
    return rows
}

val withSkill   = loadGrading(File(resultsDir, "grading_with_skill.json"))
val noSkill     = loadGrading(File(resultsDir, "grading_without_skill.json"))
    .ifEmpty { loadGrading(File(resultsDir, "grading_old_skill.json")) }

if (withSkill.isEmpty()) {
    println("No grading_with_skill.json found in $resultsDir — run grade.main.kts first.")
    System.exit(1)
}

println("=== Comparison: with_skill vs baseline ===")
println()

val allKeys = (withSkill + noSkill).map { it.eval to it.assertion }.distinct().sortedWith(compareBy({ it.first }, { it.second }))

fun List<AssertionRow>.lookup(eval: String, assertion: String) =
    firstOrNull { it.eval == eval && it.assertion == assertion }?.passed

val colWidth = 52
val header = "%-${colWidth}s  WITH   BASE   DELTA".format("ASSERTION")
println(header)
println("-".repeat(header.length))

var wins = 0; var losses = 0; var ties = 0

for ((eval, assertion) in allKeys) {
    val w = withSkill.lookup(eval, assertion)
    val b = noSkill.lookup(eval, assertion)

    val wMark = when (w) { true -> "  ✓  "; false -> "  ✗  "; null -> "  -  " }
    val bMark = when (b) { true -> "  ✓  "; false -> "  ✗  "; null -> "  -  " }
    val delta = when {
        w == true  && b != true  -> { wins++;   "  ▲  " }
        w != true  && b == true  -> { losses++; "  ▼  " }
        else                     -> { ties++;   "     " }
    }
    val label = "[$eval] $assertion"
    println("%-${colWidth}s $wMark $bMark $delta".format(label.take(colWidth)))
}

println()
val withRate  = withSkill.count { it.passed }.toDouble() / withSkill.size * 100
val baseRate  = noSkill.count   { it.passed }.toDouble() / noSkill.size  * 100
println("Pass rate  :  with_skill=${"%.0f".format(withRate)}%  baseline=${"%.0f".format(baseRate)}%")
println("Regressions: $losses  Improvements: $wins  Unchanged: $ties")
