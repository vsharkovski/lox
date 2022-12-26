package com.vsharkovski.klox

import com.vsharkovski.klox.TokenType.*

/*
Expression grammar:
    program        → declaration* EOF ;
    declaration    → varDecl
                   | statement ;
    varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
    statement      → exprStmt
                   | printStmt ;
    exprStmt       → expression ";" ;
    printStmt      → "print" expression ";" ;
    expression     → assignment ;
    assignment     → IDENTIFIER "=" assignment
                   | commaBlock ;
    commaBlock     → ternary ( "," ternary )* ;
    ternary        → equality ( "?" equality ":" equality )* ;
    equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    term           → factor ( ( "-" | "+" ) factor )* ;
    factor         → unary ( ( "/" | "*" ) unary )* ;
    unary          → ( "!" | "-" ) unary
                   | primary ;
    primary        → "true" | "false" | "nil"
                   | NUMBER | STRING
                   | "(" expression ")"
                   | IDENTIFIER ;
 */

class Parser(
    private val tokens: List<Token>
) {
    private class ParseError : RuntimeException()

    private var current: Int = 0

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            parseDeclaration()?.let { statements.add(it) }
        }

        return statements
    }

    private fun parseDeclaration(): Stmt? =
        try {
            if (advanceIfMatching(VAR))
                parseVarDeclaration()
            else
                parseStatement()
        } catch (error: ParseError) {
            synchronize()
            null
        }

    private fun parseVarDeclaration(): Stmt {
        val name = consumeOrError(IDENTIFIER, "Expect variable name.")
        val initializer = if (advanceIfMatching(EQUAL)) parseExpression() else null

        consumeOrError(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun parseStatement(): Stmt =
        if (advanceIfMatching(PRINT))
            parsePrintStatement()
        else
            parseExpressionStatement()

    private fun parsePrintStatement(): Stmt {
        val value = parseExpression()
        consumeOrError(SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun parseExpressionStatement(): Stmt {
        val expr = parseExpression()
        consumeOrError(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun parseExpression(): Expr =
        parseAssignment()

    private fun parseAssignment(): Expr {
        val expr = parseCommaBlock()

        if (advanceIfMatching(EQUAL)) {
            val token = previous()
            val value = parseAssignment()

            if (expr is Expr.Variable) {
                return Expr.Assign(expr.name, value)
            }

            error(token, "Invalid assignment target.")
        }

        return expr
    }

    private fun parseCommaBlock(): Expr =
        parseLeftAssociativeBinary({ parseTernary() }, COMMA)

    private fun parseTernary(): Expr {
        var expr = parseEquality()

        while (advanceIfMatching(QUESTION)) {
            val firstOperator = previous()
            val middleExpr = parseEquality()

            if (advanceIfMatching(COLON)) {
                val secondOperator = previous()
                val rightExpr = parseEquality()
                expr = Expr.Ternary(expr, firstOperator, middleExpr, secondOperator, rightExpr)
            } else {
                // Not a proper ternary operator, so interpret it as binary and stop looking.
                expr = Expr.Binary(expr, firstOperator, middleExpr)
                break
            }
        }

        return expr
    }

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

    private fun parsePrimary(): Expr =
        if (advanceIfMatching(FALSE)) {
            Expr.Literal(false)
        } else if (advanceIfMatching(TRUE)) {
            Expr.Literal(true)
        } else if (advanceIfMatching(NUMBER, STRING)) {
            Expr.Literal(previous().literal)
        } else if (advanceIfMatching(LEFT_PAREN)) {
            val expr = parseExpression()
            consumeOrError(RIGHT_PAREN, "Expect ')' after expression.")
            Expr.Grouping(expr)
        } else if (advanceIfMatching(IDENTIFIER)) {
            Expr.Variable(previous())
        } else {
            throw error(peek(), "Expect expression.")
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