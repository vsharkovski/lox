package com.vsharkovski.klox

private const val INDENTATION = "  "


class AstPrinterMultiline : Stmt.Visitor<String>, Expr.Visitor<String> {
    fun print(stmt: Stmt): String = stmt.accept(this)

    private fun print(expr: Expr): String = expr.accept(this)

    private fun printIndented(stmt: Stmt): String =
        print(stmt).prependIndent(INDENTATION)

    private fun printIndented(expr: Expr): String =
        print(expr).prependIndent(INDENTATION)

    override fun visitBlockStmt(stmt: Stmt.Block): String =
        "Block:${
            stmt.statements.fold("") { result, curr ->
                "$result\n${printIndented(curr)}"
            }
        }"

    override fun visitBreakStmt(stmt: Stmt.Break): String =
        "Break"

    override fun visitClassStmt(stmt: Stmt.Class): String =
        "Class: ${stmt.name.lexeme}, methods:${
            stmt.methods.fold("") { result, curr ->
                "$result\n${printIndented(curr)}"
            }
        }"

    override fun visitExpressionStmt(stmt: Stmt.Expression): String =
        "Expression:\n${printIndented(stmt.expression)}"

    override fun visitFunctionStmt(stmt: Stmt.Function): String =
        "Function: ${stmt.name.lexeme}, ${stmt.params.map { it.lexeme }}${
            stmt.body.fold("\n(body)") { result, curr ->
                "$result\n${printIndented(curr)}"
            }
        }"

    override fun visitIfStmt(stmt: Stmt.If): String = buildString {
        append("If:\n(condition)\n${printIndented(stmt.condition)}\n(then)\n${printIndented(stmt.thenBranch)}")
        if (stmt.elseBranch != null)
            append("\n(else)\n${printIndented(stmt.elseBranch)}")
    }

    override fun visitReturnStmt(stmt: Stmt.Return): String =
        "Return: \n${stmt.value?.let { printIndented(it) } ?: "nil"}"

    override fun visitVarStmt(stmt: Stmt.Var): String =
        "Var: ${stmt.name.lexeme}, value:\n${stmt.initializer?.let { printIndented(it) } ?: "nil"}"

    override fun visitWhileStmt(stmt: Stmt.While): String =
        "While:\n(condition)\n${printIndented(stmt.condition)}\n(body)\n${printIndented(stmt.body)}"

    override fun visitAssignExpr(expr: Expr.Assign): String =
        "Assign: ${expr.name.lexeme}, value:\n${printIndented(expr.value)}"

    override fun visitBinaryExpr(expr: Expr.Binary): String =
        "Binary: ${expr.operator.lexeme}\n(left)\n${printIndented(expr.left)}\n(right)\n${printIndented(expr.right)}"

    override fun visitCallExpr(expr: Expr.Call): String =
        "Call:\n(callee)\n${printIndented(expr.callee)}${
            expr.arguments.fold("\n(arguments)") { result, curr ->
                "$result\n${printIndented(curr)}"
            }
        }"

    override fun visitGetExpr(expr: Expr.Get): String =
        "Get:\n(object)\n${printIndented(expr.obj)}\nname: ${expr.name}"

    override fun visitGroupingExpr(expr: Expr.Grouping): String =
        "Group:\n${printIndented(expr.expression)}"

    override fun visitLiteralExpr(expr: Expr.Literal): String =
        "Literal: ${if (expr.value == null) "nil" else expr.value.toString()}"

    override fun visitLogicalExpr(expr: Expr.Logical): String =
        "Logical: ${expr.operator.lexeme}\n(left)\n${printIndented(expr.left)}\n(right)\n${printIndented(expr.right)}"

    override fun visitSetExpr(expr: Expr.Set): String =
        "Set:\n(object)\n${printIndented(expr.obj)}\nname: ${expr.name}\n(value)\n${printIndented(expr.value)}"

    override fun visitTernaryExpr(expr: Expr.Ternary): String =
        "Ternary:\n(condition)\n${printIndented(expr.left)}\n(then)\n${printIndented(expr.middle)}\n(else)\n${
            printIndented(expr.right)
        }"

    override fun visitThisExpr(expr: Expr.This): String = "This"

    override fun visitUnaryExpr(expr: Expr.Unary): String =
        "Unary: ${expr.operator.lexeme}, right:\n${printIndented(expr.right)}"

    override fun visitVariableExpr(expr: Expr.Variable): String =
        "Var: ${expr.name.lexeme}"
}