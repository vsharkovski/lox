package com.vsharkovski.klox

import java.io.File
import kotlin.system.exitProcess

object Klox {
    private val interpreter = Interpreter()
    private var hadError = false
    private var hadRuntimeError = false

    fun runFile(path: String) {
        runSource(File(path).readText())
        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
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
        val parser = Parser(tokens)
        val statements = parser.parse()

        // Stop if there was a syntax error.
        if (hadError) return

        interpreter.interpret(statements)
    }

    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    fun error(token: Token, message: String) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message)
        } else {
            report(token.line, " at '${token.lexeme}'", message)
        }
    }

    fun runtimeError(error: RuntimeError) {
        System.err.println("${error.message}\n[line ${error.token.line}]")
        hadRuntimeError = true
    }

    private fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error$where: $message")
        hadError = true
    }
}