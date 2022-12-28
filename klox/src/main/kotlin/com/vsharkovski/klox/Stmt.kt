package com.vsharkovski.klox

sealed interface Stmt {
    interface Visitor<R> {
        fun visitBlockStmt(stmt: Block): R
        fun visitBreakStmt(stmt: Break): R

        //        fun visitClassStmt(stmt: Class): R
        fun visitExpressionStmt(stmt: Expression): R
        fun visitFunctionStmt(stmt: Function): R
        fun visitIfStmt(stmt: If): R
        fun visitReturnStmt(stmt: Return): R
        fun visitVarStmt(stmt: Var): R
        fun visitWhileStmt(stmt: While): R
    }

    fun <R> accept(visitor: Visitor<R>): R

    data class Block(
        val statements: List<Stmt>
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitBlockStmt(this)
    }

    data class Break(
        val keyword: Token
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitBreakStmt(this)
    }

    data class Expression(
        val expression: Expr
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitExpressionStmt(this)
    }

    data class Function(
        val name: Token,
        val params: List<Token>,
        val body: List<Stmt>
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitFunctionStmt(this)
    }

    data class If(
        val condition: Expr,
        val thenBranch: Stmt,
        val elseBranch: Stmt?
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitIfStmt(this)
    }

    data class Return(
        val keyword: Token,
        val value: Expr?
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitReturnStmt(this)
    }

    data class Var(
        val name: Token,
        val initializer: Expr?
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitVarStmt(this)
    }

    data class While(
        val condition: Expr,
        val body: Stmt
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitWhileStmt(this)
    }
}