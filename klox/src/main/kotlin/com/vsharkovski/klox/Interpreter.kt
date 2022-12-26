package com.vsharkovski.klox

import com.vsharkovski.klox.TokenType.*

class Interpreter : Expr.Visitor<Any?> {
    fun interpret(expression: Expr) {
        try {
            val value = evaluate(expression)
            println(stringify(value))
        } catch (error: RuntimeError) {
            Klox.runtimeError(error)
        }
    }

    private fun evaluate(expr: Expr): Any? =
        expr.accept(this)

    override fun visitLiteralExpr(expr: Expr.Literal): Any? =
        expr.value

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
                (left as Double) / (right as Double)
            }
            STAR -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) * (right as Double)
            }
            else -> null
        }
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

