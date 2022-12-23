package com.vsharkovski.klox

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Usage: klox [script]")
        exitProcess(64)
    } else if (args.size == 1) {
        Klox.runFile(args[0])
    } else {
        Klox.runPrompt()
    }
}
