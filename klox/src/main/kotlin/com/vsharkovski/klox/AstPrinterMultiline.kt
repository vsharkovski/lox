package com.vsharkovski.klox

private const val INDENTATION = "  "

class AstPrinterMultiline : Stmt.Visitor<String>, Expr.Visitor<String> {
    fun print(stmt: Stmt) = stmt.accept(this)

    fun print(expr: Expr) = expr.accept(this)

    override fun visitBlockStmt(stmt: Stmt.Block): String =
        stmt.statements.fold("Block:") { result, curr ->
            "${result}\n${curr.accept(this).prependIndent(INDENTATION)}"
        }

    override fun visitExpressionStmt(stmt: Stmt.Expression): String =
        "Expression:\n${stmt.expression.accept(this).prependIndent(INDENTATION)}"

    override fun visitIfStmt(stmt: Stmt.If): String = buildString {
        append("If:\n(condition)\n")
        append(stmt.condition.accept(this@AstPrinterMultiline).prependIndent(INDENTATION))
        append("\n(then)\n")
        append(stmt.thenBranch.accept(this@AstPrinterMultiline).prependIndent(INDENTATION))
        if (stmt.elseBranch != null) {
            append("\n(else)\n")
            append(stmt.elseBranch.accept(this@AstPrinterMultiline).prependIndent(INDENTATION))
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print): String =
        "Print:\n${stmt.expression.accept(this).prependIndent(INDENTATION)}"

    override fun visitVarStmt(stmt: Stmt.Var): String =
        when (stmt) {
            is Stmt.UninitializedVar ->
                "UninitializedVar: ${stmt.name.lexeme}"
            is Stmt.InitializedVar ->
                "InitializedVar: ${stmt.name.lexeme}, value:\n${
                    (stmt.initializer?.accept(this) ?: "").prependIndent(INDENTATION)
                }"
        }

    override fun visitAssignExpr(expr: Expr.Assign): String =
        "Assign: ${expr.name.lexeme}, value:\n${
            expr.value.accept(this).prependIndent(INDENTATION)
        }"

    override fun visitBinaryExpr(expr: Expr.Binary): String = buildString {
        append("Binary: ${expr.operator.lexeme}\n(left)\n")
        append(expr.left.accept(this@AstPrinterMultiline).prependIndent(INDENTATION))
        append("\n(right)\n")
        append(expr.right.accept(this@AstPrinterMultiline).prependIndent(INDENTATION))
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String =
        "Group:\n${expr.expression.accept(this).prependIndent(INDENTATION)}"

    override fun visitLiteralExpr(expr: Expr.Literal): String =
        "Literal: ${if (expr.value == null) "nil" else expr.value.toString()}"

    override fun visitLogicalExpr(expr: Expr.Logical): String = buildString {
        append("Logical: ${expr.operator.lexeme}\n(left)\n")
        append(expr.left.accept(this@AstPrinterMultiline).prependIndent(INDENTATION))
        append("\n(right)\n")
        append(expr.right.accept(this@AstPrinterMultiline).prependIndent(INDENTATION))
    }

    override fun visitTernaryExpr(expr: Expr.Ternary): String = buildString {
        append("Ternary:\n(condition)\n")
        append(expr.left.accept(this@AstPrinterMultiline).prependIndent(INDENTATION))
        append("\n(then)\n")
        append(expr.middle.accept(this@AstPrinterMultiline).prependIndent(INDENTATION))
        append("\n(else)\n")
        append(expr.right.accept(this@AstPrinterMultiline).prependIndent(INDENTATION))
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String =
        "Unary: ${expr.operator.lexeme}, right:\n${
            expr.right.accept(this).prependIndent(INDENTATION)
        }"

    override fun visitVariableExpr(expr: Expr.Variable): String =
        "Var: ${expr.name.lexeme}"
}