package com.vsharkovski.klox

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size > 2) {
        println("Usage: klox [script] [debug_printing (default: false)]")
        exitProcess(64)
    } else if (args.size == 2) {
        if (args[1].lowercase().toBooleanStrictOrNull() == null) {
            println("Usage: klox [script] [debug_printing (default: false)]")
            exitProcess(64)
        } else {
            Klox.runFile(args[0], args[1].toBoolean())
        }
    } else if (args.size == 1) {
        Klox.runFile(args[0])
    } else {
        Klox.runPrompt()
    }
}
