package com.vsharkovski.klox

import java.io.File
import kotlin.system.exitProcess

object Klox {
    private var hadError = false
    private var hadRuntimeError = false

    fun runFile(path: String, printDebug: Boolean = false) {
        val interpreter = Interpreter()
        runSource(File(path).readText(), interpreter, printDebug)

        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
    }

    fun runPrompt() {
        val interpreter = Interpreter(true)
        while (true) {
            print("> ")
            val line = readlnOrNull() ?: break
            runSource(line, interpreter, false)
            hadError = false
        }
    }

    private fun runSource(source: String, interpreter: Interpreter, printDebug: Boolean) {
        val scanner = Scanner(source)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens)
        val statements = parser.parse()

        if (printDebug) {
            println("############ SYNTAX TREE BEGIN")
            val astPrinter = AstPrinterMultiline()
            statements.forEach { println(astPrinter.print(it)) }
            println("############ SYNTAX TREE END")
        }

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