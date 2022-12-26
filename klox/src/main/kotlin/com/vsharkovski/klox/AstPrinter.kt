package com.vsharkovski.klox

class AstPrinter : Expr.Visitor<String> {
    fun print(expr: Expr): String =
        expr.accept(this)

    override fun visitAssignExpr(expr: Expr.Assign): String =
        parenthesize("var ${expr.name.lexeme}", expr.value)

    override fun visitBinaryExpr(expr: Expr.Binary): String =
        parenthesize(expr.operator.lexeme, expr.left, expr.right)

    override fun visitGroupingExpr(expr: Expr.Grouping): String =
        parenthesize("group", expr.expression)

    override fun visitLiteralExpr(expr: Expr.Literal): String =
        if (expr.value == null) "nil" else expr.value.toString()

    override fun visitTernaryExpr(expr: Expr.Ternary): String =
        parenthesize("ternary", expr.left, expr.middle, expr.right)

    override fun visitUnaryExpr(expr: Expr.Unary): String =
        parenthesize(expr.operator.lexeme, expr.right)

    override fun visitVariableExpr(expr: Expr.Variable): String =
        parenthesize("var ${expr.name.lexeme}")

    private fun parenthesize(name: String, vararg expressions: Expr): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in expressions) {
            builder.append(" ")
            builder.append(expr.accept(this))
        }
        builder.append(")")
        return builder.toString()
    }
}