package com.vsharkovski.klox

import com.vsharkovski.klox.TokenType.*

class Scanner(
    private val source: String
) {
    private val keywords: Map<String, TokenType> = mapOf(
        "and" to AND,
        "break" to BREAK,
        "class" to CLASS,
        "else" to ELSE,
        "false" to FALSE,
        "for" to FOR,
        "fun" to FUN,
        "if" to IF,
        "nil" to NIL,
        "or" to OR,
        "return" to RETURN,
        "super" to SUPER,
        "this" to THIS,
        "true" to TRUE,
        "var" to VAR,
        "while" to WHILE
    )
    private val tokens: MutableList<Token> = mutableListOf()

    private var start: Int = 0
    private var current: Int = 0
    private var line: Int = 1
    private var linePosition: Int = 0

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current
            scanToken()
        }

        tokens.add(Token(EOF, "", null, line, linePosition))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '-' -> addToken(MINUS)
            '+' -> addToken(PLUS)
            ';' -> addToken(SEMICOLON)
            ':' -> addToken(COLON)
            '?' -> addToken(QUESTION)
            '*' -> addToken(STAR)
            '!' -> addToken(if (advanceIfMatching('=')) BANG_EQUAL else BANG)
            '=' -> addToken(if (advanceIfMatching('=')) EQUAL_EQUAL else EQUAL)
            '<' -> addToken(if (advanceIfMatching('=')) LESS_EQUAL else LESS)
            '>' -> addToken(if (advanceIfMatching('=')) GREATER_EQUAL else GREATER)
            '/' -> if (advanceIfMatching('/')) {
                // A single-line comment goes until the end of the line.
                while (!isAtEnd() && peek() != '\n') advance()
            } else if (advanceIfMatching('*')) {
                scanBlockComment()
            } else {
                addToken(SLASH)
            }
            ' ', '\r', '\t' -> Unit
            '\n' -> {
                line++
                linePosition = 0
            }
            '"' -> scanString()
            else -> {
                if (c.isDigit()) {
                    scanNumber()
                } else if (c.isAlpha()) {
                    scanIdentifier()
                } else {
                    Klox.error(line, "Unexpected character '$c'.")
                }
            }
        }
    }

    private fun scanString() {
        while (peek() != '"') {
            if (peek() == '\n') {
                line++
                linePosition = 0
            }
            advance()
        }

        if (isAtEnd()) {
            Klox.error(line, "Unterminated string.")
            return
        }

        // The closing ".
        advance()

        // Trim the surrounding quotes.
        addToken(STRING, source.substring(start + 1, current - 1))
    }

    private fun scanNumber() {
        while (peek().isAsciiDigit()) advance()

        // Look for a fractional part.
        if (peek() == '.' && peekNext().isAsciiDigit()) {
            // Consume the '.'
            advance()
            while (peek().isAsciiDigit()) advance()
        }

        addToken(NUMBER, source.substring(start, current).toDouble())
    }

    private fun scanIdentifier() {
        while (peek().isAlphaNumeric()) advance()

        val text = source.substring(start, current)

        // Check if what we scanned is actually a reserved keyword instead of an identifier.
        val type = keywords[text] ?: IDENTIFIER

        addToken(type)
    }

    private fun scanBlockComment() {
        // Block comments can be nested, so the number of nested blocks at any point should be tracked.
        var depth = 1

        while (!isAtEnd()) {
            val c = advance()
            if (c == '\n') {
                line++
                linePosition = 0
            } else if (c == '/' && advanceIfMatching('*')) {
                // Entering a block comment.
                depth++
            } else if (c == '*' && advanceIfMatching('/')) {
                // Exiting a block comment.
                depth--
                if (depth == 0) {
                    // Now outside any block comments, so stop.
                    break
                }
            }
        }

        if (depth > 0) {
            // Reached end of source but did not exit all nested comment blocks.
            Klox.error(line, "Unclosed block comment.")
        }
    }

    private fun isAtEnd(): Boolean =
        current >= source.length

    private fun advance(): Char {
        linePosition++
        return source[current++]
    }

    private fun advanceIfMatching(expected: Char): Boolean {
        if (isAtEnd() || source[current] != expected) return false
        current++
        linePosition++
        return true
    }

    private fun peek(): Char =
        if (isAtEnd()) 0.toChar() else source[current]

    private fun peekNext(): Char =
        if (current + 1 >= source.length) 0.toChar() else source[current + 1]

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal, line, linePosition))
    }
}

private fun Char.isAsciiDigit() =
    this in '0'..'9'

private fun Char.isAlpha() =
    this in 'a'..'z'
            || this in 'A'..'Z'
            || this == '_'

private fun Char.isAlphaNumeric() =
    this.isAsciiDigit() || this.isAlpha()