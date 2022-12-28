package com.vsharkovski.klox

import com.vsharkovski.klox.TokenType.*

class Interpreter(
    private val printSingleExpressionStatements: Boolean = false
) : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    val globals = Environment()
    private var environment = globals
    private var loopBroken = false

    init {
        globals.define("clock", object : KloxCallable {
            override val arity = 0

            override fun call(interpreter: Interpreter, arguments: List<Any?>): Double {
                return System.currentTimeMillis().toDouble() / 1000.0
            }

            override fun toString(): String = "<native fn>"
        })
    }

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements)
                execute(statement)
        } catch (error: RuntimeError) {
            Klox.runtimeError(error)
        }
    }

    private fun evaluate(expr: Expr): Any? =
        expr.accept(this)

    private fun execute(stmt: Stmt) =
        stmt.accept(this)

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previousEnvironment = this.environment
        try {
            this.environment = environment

            for (statement in statements)
                execute(statement)
        } finally {
            this.environment = previousEnvironment
        }
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(this.environment))
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        loopBroken = true
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        val value = evaluate(stmt.expression)
        if (printSingleExpressionStatements)
            println(stringify(value))
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = KloxFunction(stmt)
        environment.define(stmt.name.lexeme, function)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition)))
            execute(stmt.thenBranch)
        else if (stmt.elseBranch != null)
            execute(stmt.elseBranch)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitVarStmt(stmt: Stmt.Var) =
        when (stmt) {
            is Stmt.UninitializedVar -> {
                environment.define(stmt.name.lexeme)
            }
            is Stmt.InitializedVar -> {
                // Evaluate value if it is not nil.
                val value = evaluate(stmt.initializer)

                environment.define(stmt.name.lexeme, value)
            }
        }

    override fun visitWhileStmt(stmt: Stmt.While) {
        // While the condition is truthy and the loop has not been broken, execute the body.
        while (isTruthy(evaluate(stmt.condition)) && !loopBroken)
            execute(stmt.body)

        // If the loop was broken, reset this flag.
        if (loopBroken)
            loopBroken = false
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        environment.assign(expr.name, value)
        return value
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? =
        expr.value

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)

        return if (expr.operator.type == OR && isTruthy(left))
            left
        else if (expr.operator.type == AND && !isTruthy(left))
            left
        else
            evaluate(expr.right)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? =
        evaluate(expr.expression)

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            BANG -> !isTruthy(right)
            MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }
            else -> null
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? =
        environment.get(expr.name)

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            COMMA ->
                // Block operator (e.g. "a, b") always discards left result and returns right.
                right
            BANG_EQUAL ->
                !isEqual(left, right)
            EQUAL_EQUAL ->
                isEqual(left, right)
            GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) > (right as Double)
            }
            GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) >= (right as Double)
            }
            LESS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) < (right as Double)
            }
            LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) <= (right as Double)
            }
            MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) - (right as Double)
            }
            PLUS -> {
                if (left is Double && right is Double)
                    left + right
                else if (left is String && right is String)
                    left + right
                else if (left is String)
                    left + stringify(right)
                else if (right is String)
                    stringify(left) + right
                else
                    throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
            }
            SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                if ((right as Double).equals(0.0)) {
                    throw RuntimeError(expr.operator, "Division by zero.")
                }
                (left as Double) / right
            }
            STAR -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) * (right as Double)
            }
            else -> null
        }
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)
        val arguments = expr.arguments.map { evaluate(it) }

        // Ensure the callee is a function.
        if (callee !is KloxCallable)
            throw RuntimeError(expr.paren, "Can only call functions and classes.")

        // Ensure the number of passed arguments is correct.
        if (arguments.size != callee.arity)
            throw RuntimeError(expr.paren, "Expected ${callee.arity} arguments but got ${arguments.size}.")

        return callee.call(this, arguments)
    }

    override fun visitTernaryExpr(expr: Expr.Ternary): Any? {
        if (expr.firstOperator.type != QUESTION)
            throw RuntimeError(expr.firstOperator, "Operator must be ternary expression operator.")
        if (expr.secondOperator.type != COLON)
            throw RuntimeError(expr.secondOperator, "Operator must be ternary expression operator.")

        val condition = evaluate(expr.left)
        val firstOption = evaluate(expr.middle)
        val secondOption = evaluate(expr.right)

        return if (isTruthy(condition))
            firstOption
        else
            secondOption
    }

    private fun isTruthy(obj: Any?): Boolean =
        when (obj) {
            null -> false
            is Boolean -> obj
            is Double -> !obj.equals(0.0)
            else -> true
        }

    private fun isEqual(left: Any?, right: Any?): Boolean =
        if (left == null && right == null)
            true
        else
            left?.equals(right) ?: false

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun stringify(obj: Any?): String =
        when (obj) {
            null -> "nil"
            is Double -> {
                val text = obj.toString()
                if (text.endsWith(".0"))
                    text.substring(0, text.length - 2)
                else
                    text
            }
            else -> obj.toString()
        }
}

