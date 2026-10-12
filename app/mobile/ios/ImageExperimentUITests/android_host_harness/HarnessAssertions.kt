package com.tellpal.imageexperiment.host

import java.util.concurrent.atomic.AtomicInteger

object HarnessAssertions {
    private val count = AtomicInteger()
    fun case(name: String, operation: () -> Unit) {
        operation()
        count.incrementAndGet()
        println("HARNESS_CASE $name PASS")
    }
    fun count(): Int = count.get()
    fun equal(actual: Any?, expected: Any?, label: String) {
        check(actual == expected) { "HARNESS_ASSERTION $label" }
    }
    fun truth(value: Boolean, label: String) { check(value) { "HARNESS_ASSERTION $label" } }
}

fun main(arguments: Array<String>) {
    require(arguments.size in 4..5)
    val root = java.io.File(arguments[0])
    HostedFixture.install(java.io.File(arguments[1]))
    try {
        if (arguments.getOrNull(4) == "propagation") LauncherHarness.propagation(root)
        else {
            NativeContracts.run(root)
            ActivityHarness.run(root, java.io.File(arguments[2]))
            LauncherHarness.run(root, java.io.File(arguments[3]))
        }
        println("HARNESS_TOTAL ${HarnessAssertions.count()}")
    } finally { HostRuntime.close() }
}
