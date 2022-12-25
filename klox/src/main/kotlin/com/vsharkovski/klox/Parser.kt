package com.vsharkovski.klox

import com.vsharkovski.klox.TokenType.*

class Parser(
    private val tokens: List<Token>
) {
    private class ParseError : RuntimeException()

    private var current: Int = 0

    fun parse(): Expr? {
        return try {
            parseExpression()
        } catch (error: ParseError) {
            null
        }
    }

    /*
    Expression grammar:
        expression     → block ;
        block          → ternary ( "," ternary )* ;
        ternary        → equality ( "?:" equality )* ;
        equality       → comparison ( ( "!=" | "==" ) comparison )* ;
        comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
        term           → factor ( ( "-" | "+" ) factor )* ;
        factor         → unary ( ( "/" | "*" ) unary )* ;
        unary          → ( "!" | "-" ) unary
                       | primary ;
        primary        → NUMBER | STRING | "true" | "false" | "nil"
                       | "(" expression ")" ;
     */

    private fun parseExpression(): Expr =
        parseBlock()

    private fun parseBlock(): Expr =
        parseLeftAssociativeBinary({ parseTernary() }, COMMA)

    private fun parseTernary(): Expr =
        parseLeftAssociativeBinary({ parseEquality() }, QUESTION_COLON)

    private fun parseEquality(): Expr =
        parseLeftAssociativeBinary({ parseComparison() }, BANG_EQUAL, EQUAL_EQUAL)

    private fun parseComparison(): Expr =
        parseLeftAssociativeBinary({ parseTerm() }, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)

    private fun parseTerm(): Expr =
        parseLeftAssociativeBinary({ parseFactor() }, MINUS, PLUS)

    private fun parseFactor(): Expr =
        parseLeftAssociativeBinary({ parseUnary() }, SLASH, STAR)

    private fun parseUnary(): Expr =
        if (advanceIfMatching(BANG, MINUS)) {
            val operator = previous()
            val right = parseUnary()
            Expr.Unary(operator, right)
        } else {
            parsePrimary()
        }

    private fun parsePrimary(): Expr {
        val curr = advance()
        return when (curr.type) {
            FALSE ->
                Expr.Literal(false)
            TRUE ->
                Expr.Literal(true)
            NIL ->
                Expr.Literal(null)
            NUMBER, STRING ->
                Expr.Literal(curr.literal)
            LEFT_PAREN -> {
                val expr = parseExpression()
                consumeOrError(RIGHT_PAREN, "Expect ')' after expression.")
                Expr.Grouping(expr)
            }
            else ->
                throw error(curr, "Expect expression.")
        }
    }

    /**
     * Parse a left-associative binary expression group.
     * This corresponds to a grammar rule of the form:
     *  a → b ( ( type_1 | type_2 | ... | type_k ) b )* ;
     * Where 'a' is the current group, 'b' is the next higher precedence group,
     * and type_1, ..., type_k are the token types to match for this group.
     */
    private fun parseLeftAssociativeBinary(
        nextHigherPrecedenceParseFn: () -> Expr,
        vararg types: TokenType
    ): Expr {
        var expr = nextHigherPrecedenceParseFn()

        while (advanceIfMatching(*types)) {
            val operator = previous()
            val right = nextHigherPrecedenceParseFn()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun advanceIfMatching(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consumeOrError(type: TokenType, errorMessage: String): Token {
        if (check(type)) return advance()
        throw error(peek(), errorMessage)
    }

    private fun check(type: TokenType): Boolean =
        if (isAtEnd()) false else peek().type == type

    private fun isAtEnd(): Boolean =
        peek().type == EOF

    private fun peek(): Token =
        tokens[current]

    private fun previous(): Token =
        tokens[current - 1]

    private fun error(token: Token, message: String): ParseError {
        Klox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return

            when (peek().type) {
                CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
                else -> advance()
            }
        }
    }
}