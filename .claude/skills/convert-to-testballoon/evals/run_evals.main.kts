#!/usr/bin/env kotlin

/**
 * Runs the convert-to-testballoon skill against each input file,
 * saves outputs to a results directory, then invokes the grader.
 *
 * Usage:
 *   kotlinc -script run_evals.main.kts [-- --results-dir <path>] [--baseline]
 *
 * --results-dir  where to write outputs (default: <project>/.claude/skills/convert-to-testballoon-workspace/iteration-1)
 * --baseline     run WITHOUT the skill (for comparison)
 */

import java.io.File
import java.util.concurrent.TimeUnit

val args = args.toList()
val skillDir = File(__FILE__.parent).parentFile  // evals/../ = skill root
val projectDir = skillDir.parentFile.parentFile  // skills/../.. = project root

val resultsDir = args.indexOf("--results-dir").let { i ->
    if (i >= 0 && i + 1 < args.size) File(args[i + 1]) else
        File(projectDir, ".claude/skills/convert-to-testballoon-workspace/iteration-1")
}
val baseline = "--baseline" in args
val skillPath = File(skillDir, "SKILL.md").absolutePath

val inputsDir = File(__FILE__.parent, "inputs")
val inputs = inputsDir.listFiles { f -> f.extension == "kt" }?.sortedBy { it.name } ?: emptyList()

println("=== testBalloon Eval Runner ===")
println("Mode    : ${if (baseline) "baseline (no skill)" else "with skill"}")
println("Results : $resultsDir")
println("Inputs  : ${inputs.size} files")
println()

var passed = 0
var failed = 0

for (input in inputs) {
    val evalName = input.nameWithoutExtension
    val runLabel = if (baseline) "without_skill" else "with_skill"
    val outputDir = File(resultsDir, "$evalName/$runLabel").also { it.mkdirs() }

    println("▶ $evalName")

    val prompt = buildString {
        appendLine("Convert the following JUnit/Kotlin test to testBalloon DSL format.")
        appendLine("Output ONLY the converted Kotlin source file — no explanation, no markdown fences.")
        appendLine()
        appendLine("Source file: ${input.name}")
        appendLine()
        appendLine(input.readText())
    }

    val cmd = buildList {
        add("claude")
        add("-p")
        add(prompt)
        if (!baseline) {
            add("--skill")
            add(skillPath)
        }
        add("--output-format")
        add("text")
    }

    val start = System.currentTimeMillis()
    val process = ProcessBuilder(cmd)
        .redirectErrorStream(true)
        .start()

    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor(5, TimeUnit.MINUTES).let {
        if (it) process.exitValue() else { process.destroyForcibly(); -1 }
    }
    val durationMs = System.currentTimeMillis() - start

    val outputFile = File(outputDir, "${input.nameWithoutExtension}_converted.kt")
    outputFile.writeText(output)

    File(outputDir, "timing.json").writeText(
        """{"duration_ms": $durationMs, "exit_code": $exitCode}"""
    )

    if (exitCode == 0) {
        println("  ✓ saved to ${outputFile.relativeTo(resultsDir)}")
        passed++
    } else {
        println("  ✗ claude exited with code $exitCode")
        failed++
    }
    println()
}

println("=== Done: $passed passed, $failed failed ===")
println("Run grade.main.kts to check conversion quality.")
