package com.vsharkovski.klox

import java.io.File
import kotlin.system.exitProcess

object Klox {
    var hadError: Boolean = false
        private set

    fun runFile(path: String) {
        runSource(File(path).readText())
        if (hadError) exitProcess(65)
    }

    fun runPrompt() {
        while (true) {
            print("> ")
            val line = readlnOrNull() ?: break
            runSource(line)
            hadError = false
        }
    }

    private fun runSource(source: String) {
        val scanner = Scanner(source)
        val tokens = scanner.scanTokens()

        for (token in tokens) {
            println(token)
        }
    }

    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error $where: $message")
        hadError = true
    }
}