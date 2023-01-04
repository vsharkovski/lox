package com.vsharkovski.klox

import com.vsharkovski.klox.TokenType.*

/*
Complete grammar:
    program        → declaration* EOF ;
    declaration    → classDecl
                   | funDecl
                   | varDecl
                   | statement ;
    classDecl      → "class" IDENTIFIER "{" function* "}" ;
    funDecl        → "fun" function ;
    function       → IDENTIFIER "(" parameters? ")" block ;
    parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
    varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
    statement      → breakStmt
                   | exprStmt
                   | forStmt
                   | ifStmt
                   | returnStmt
                   | whileStmt
                   | block ;
    breakStmt      → "break" ";" ;
    exprStmt       → expression ";" ;
    forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                     expression? ";"
                     expression? ")" statement ;
    ifStmt         → "if" "(" expression ")" statement
                   ( "else" statement )? ;
    returnStmt     → "return" expression? ";" ;
    whileStmt      → "while" "(" expression ")" statement ;
    block          → "{" declaration "}" ;
    expression     → commaBlock ;
    commaBlock     → assignment ( "," assignment )* ;
    assignment     → IDENTIFIER "=" assignment
                   | ternary ;
    ternary        → logic_or ( "?" logic_or ":" logic_or )* ;
    logic_or       → logic_and ( "or" logic_and )* ;
    logic_and      → equality ( "and" equality )* ;
    equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    term           → factor ( ( "-" | "+" ) factor )* ;
    factor         → unary ( ( "/" | "*" ) unary )* ;
    unary          → ( "!" | "-" ) unary
                   | call ;
    call           → primary ( "(" arguments? ")" )* ;
    arguments      → assignment ( "," assignment )* ;
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

    private fun parseDeclaration(): Stmt? = try {
        if (advanceIfMatching(CLASS)) parseClassDeclaration()
        else if (advanceIfMatching(FUN)) parseFunction("function")
        else if (advanceIfMatching(VAR)) parseVarDeclaration()
        else parseStatement()
    } catch (error: ParseError) {
        synchronize()
        null
    }

    private fun parseClassDeclaration(): Stmt.Class {
        val name = consumeOrError(IDENTIFIER, "Expect class name.")
        consumeOrError(LEFT_BRACE, "Expect '{' before class body.")

        val methods = mutableListOf<Stmt.Function>()
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(parseFunction("method"))
        }

        consumeOrError(RIGHT_BRACE, "Expect '}' after class body.")

        return Stmt.Class(name, methods)
    }

    private fun parseVarDeclaration(): Stmt.Var {
        val name = consumeOrError(IDENTIFIER, "Expect variable name.")
        val initializer = if (advanceIfMatching(EQUAL)) parseExpression() else null
        consumeOrError(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun parseStatement(): Stmt = if (advanceIfMatching(BREAK)) parseBreakStatement()
    else if (advanceIfMatching(FOR)) parseForStatement()
    else if (advanceIfMatching(IF)) parseIfStatement()
    else if (advanceIfMatching(RETURN)) parseReturnStatement()
    else if (advanceIfMatching(WHILE)) parseWhileStatement()
    else if (advanceIfMatching(LEFT_BRACE)) Stmt.Block(parseBlock())
    else parseExpressionStatement()

    private fun parseBreakStatement(): Stmt {
        val keyword = previous()
        consumeOrError(SEMICOLON, "Expect ';' after break.")
        return Stmt.Break(keyword)
    }

    private fun parseExpressionStatement(): Stmt {
        val expr = parseExpression()
        consumeOrError(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun parseForStatement(): Stmt {
        consumeOrError(LEFT_PAREN, "Expect '(' after 'for'.")

        val initializer = if (advanceIfMatching(SEMICOLON)) {
            null
        } else if (advanceIfMatching(VAR)) {
            parseVarDeclaration()
        } else {
            parseExpressionStatement()
        }

        val condition = if (check(SEMICOLON)) null else parseExpression()
        consumeOrError(SEMICOLON, "Expect ';' after loop condition.")

        val increment = if (check(RIGHT_PAREN)) null else parseExpression()
        consumeOrError(RIGHT_PAREN, "Expect ')' after for clause.")

        // Parse the content of the for loop.
        var body = parseStatement()

        if (increment != null) {
            // body = { content; increment }
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        }

        // body = while (condition) { content; increment? }
        body = Stmt.While(condition ?: Expr.Literal(true), body)

        // Body = initializer; while (condition) { content; increment? }
        if (initializer != null) {
            body = Stmt.Block(listOf(initializer, body))
        }

        return body
    }

    private fun parseFunction(kind: String): Stmt.Function {
        val name = consumeOrError(IDENTIFIER, "Expect $kind name.")
        consumeOrError(LEFT_PAREN, "Expect '(' after $kind name.")

        val parameters = mutableListOf<Token>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) error(peek(), "Can't have more than 255 parameters.")

                parameters.add(consumeOrError(IDENTIFIER, "Expect parameter name."))
            } while (advanceIfMatching(COMMA))
        }
        consumeOrError(RIGHT_PAREN, "Expect ')' after parameters.")

        consumeOrError(LEFT_BRACE, "Expect '{' before $kind body.")
        val body = parseBlock()
        return Stmt.Function(name, parameters, body)
    }

    private fun parseIfStatement(): Stmt {
        consumeOrError(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = parseExpression()
        consumeOrError(RIGHT_PAREN, "Expect ')' after if condition.")

        val thenBranch = parseStatement()
        val elseBranch = if (advanceIfMatching(ELSE)) parseStatement() else null

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun parseReturnStatement(): Stmt {
        val keyword = previous()
        val value = if (check(SEMICOLON)) null else parseExpression()
        consumeOrError(SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun parseWhileStatement(): Stmt {
        consumeOrError(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = parseExpression()
        consumeOrError(RIGHT_PAREN, "Expect ')' after condition.")
        val body = parseStatement()

        return Stmt.While(condition, body)
    }

    private fun parseBlock(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            parseDeclaration()?.let { statements.add(it) }
        }

        consumeOrError(RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun parseExpression(): Expr = parseCommaBlock()

    private fun parseCommaBlock(): Expr = parseLeftAssociativeBinary({ parseAssignment() }, COMMA)

    private fun parseAssignment(): Expr {
        val expr = parseTernary()

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

    private fun parseTernary(): Expr {
        var expr = parseOr()

        while (advanceIfMatching(QUESTION)) {
            val firstOperator = previous()
            val middleExpr = parseOr()

            if (advanceIfMatching(COLON)) {
                val secondOperator = previous()
                val rightExpr = parseOr()
                expr = Expr.Ternary(expr, firstOperator, middleExpr, secondOperator, rightExpr)
            } else {
                throw error(
                    peek(), "Expect ':' after 'then' branch of ternary conditional operator '?:'."
                )
            }
        }

        return expr
    }

    private fun parseOr(): Expr = parseLeftAssociativeBinary({ parseAnd() }, OR)

    private fun parseAnd(): Expr = parseLeftAssociativeBinary({ parseEquality() }, AND)

    private fun parseEquality(): Expr = parseLeftAssociativeBinary({ parseComparison() }, BANG_EQUAL, EQUAL_EQUAL)

    private fun parseComparison(): Expr =
        parseLeftAssociativeBinary({ parseTerm() }, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)

    private fun parseTerm(): Expr = parseLeftAssociativeBinary({ parseFactor() }, MINUS, PLUS)

    private fun parseFactor(): Expr = parseLeftAssociativeBinary({ parseUnary() }, SLASH, STAR)

    private fun parseUnary(): Expr = if (advanceIfMatching(BANG, MINUS)) {
        val operator = previous()
        val right = parseUnary()
        Expr.Unary(operator, right)
    } else {
        parseCall()
    }

    private fun parseCall(): Expr {
        var expr = parsePrimary()

        while (true) {
            if (advanceIfMatching(LEFT_PAREN)) {
                expr = finishParseCall(expr)
            } else {
                break
            }
        }

        return expr
    }

    private fun finishParseCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) error(peek(), "Can't have more than 255 arguments.")

                // We parse on the level of 'assignment' instead of 'expression'
                // because otherwise our commaBlock expression would consume all arguments
                // into one expression
                arguments.add(parseAssignment())
            } while (advanceIfMatching(COMMA))
        }

        val paren = consumeOrError(RIGHT_PAREN, "Expect ')' after arguments.")

        return Expr.Call(callee, paren, arguments)
    }

    private fun parsePrimary(): Expr = if (advanceIfMatching(FALSE)) {
        Expr.Literal(false)
    } else if (advanceIfMatching(TRUE)) {
        Expr.Literal(true)
    } else if (advanceIfMatching(NIL)) {
        Expr.Literal(null)
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
        nextHigherPrecedenceParseFn: () -> Expr, vararg types: TokenType
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

    private fun check(type: TokenType): Boolean = if (isAtEnd()) false else peek().type == type

    private fun isAtEnd(): Boolean = peek().type == EOF

    private fun peek(): Token = tokens[current]

    private fun previous(): Token = tokens[current - 1]

    private fun error(token: Token, message: String): ParseError {
        Klox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return

            when (peek().type) {
                CLASS, FUN, VAR, FOR, IF, WHILE, RETURN -> return
                else -> advance()
            }
        }
    }
}