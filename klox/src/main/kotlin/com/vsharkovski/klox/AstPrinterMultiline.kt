package com.vsharkovski.klox

private const val INDENTATION = "  "


class AstPrinterMultiline : Stmt.Visitor<String>, Expr.Visitor<String> {
    fun print(stmt: Stmt): String = stmt.accept(this)

    fun print(expr: Expr): String = expr.accept(this)

    private fun printIndented(stmt: Stmt): String =
        print(stmt).prependIndent(INDENTATION)

    private fun printIndented(expr: Expr): String =
        print(expr).prependIndent(INDENTATION)

    override fun visitBlockStmt(stmt: Stmt.Block): String =
        stmt.statements.fold("Block:") { result, curr ->
            "${result}\n${printIndented(curr)}"
        }

    override fun visitBreakStmt(stmt: Stmt.Break): String =
        "Break"

    override fun visitExpressionStmt(stmt: Stmt.Expression): String =
        "Expression:\n${printIndented(stmt.expression)}"

    override fun visitIfStmt(stmt: Stmt.If): String = buildString {
        append("If:\n(condition)\n${printIndented(stmt.condition)}\n(then)\n${printIndented(stmt.thenBranch)}")
        if (stmt.elseBranch != null)
            append("\n(else)\n${printIndented(stmt.elseBranch)}")
    }

    override fun visitPrintStmt(stmt: Stmt.Print): String =
        "Print:\n${printIndented(stmt.expression)}"

    override fun visitVarStmt(stmt: Stmt.Var): String =
        when (stmt) {
            is Stmt.UninitializedVar ->
                "UninitializedVar: ${stmt.name.lexeme}"
            is Stmt.InitializedVar ->
                "InitializedVar: ${stmt.name.lexeme}, value:\n${printIndented(stmt.initializer)}"
        }

    override fun visitWhileStmt(stmt: Stmt.While): String =
        "While:\n(condition)\n${printIndented(stmt.condition)}\n(body)\n${printIndented(stmt.body)}"

    override fun visitAssignExpr(expr: Expr.Assign): String =
        "Assign: ${expr.name.lexeme}, value:\n${printIndented(expr.value)}"

    override fun visitBinaryExpr(expr: Expr.Binary): String =
        "Binary: ${expr.operator.lexeme}\n(left)\n${printIndented(expr.left)}\n(right)\n${printIndented(expr.right)}"

    override fun visitGroupingExpr(expr: Expr.Grouping): String =
        "Group:\n${printIndented(expr.expression)}"

    override fun visitLiteralExpr(expr: Expr.Literal): String =
        "Literal: ${if (expr.value == null) "nil" else expr.value.toString()}"

    override fun visitLogicalExpr(expr: Expr.Logical): String =
        "Logical: ${expr.operator.lexeme}\n(left)\n${printIndented(expr.left)}\n(right)\n${printIndented(expr.right)}"

    override fun visitTernaryExpr(expr: Expr.Ternary): String =
        "Ternary:\n(condition)\n${printIndented(expr.left)}\n(then)\n${printIndented(expr.middle)}\n(else)\n${
            printIndented(expr.right)
        }"

    override fun visitUnaryExpr(expr: Expr.Unary): String =
        "Unary: ${expr.operator.lexeme}, right:\n${printIndented(expr.right)}"

    override fun visitVariableExpr(expr: Expr.Variable): String =
        "Var: ${expr.name.lexeme}"
}