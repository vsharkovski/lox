package com.vsharkovski.klox

sealed interface Stmt {
    interface Visitor<R> {
        fun visitBlockStmt(stmt: Block): R
//        fun visitClassStmt(stmt: Class): R
        fun visitExpressionStmt(stmt: Expression): R
//        fun visitFunctionStmt(stmt: Function): R
//        fun visitIfStmt(stmt: If): R
        fun visitPrintStmt(stmt: Print): R
//        fun visitReturnStmt(stmt: Return): R
        fun visitVarStmt(stmt: Var): R
//        fun visitWhileStmt(stmt: While): R
    }

    fun <R> accept(visitor: Visitor<R>): R

    data class Block(
        val statements: List<Stmt>
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitBlockStmt(this)
    }

    data class Expression(
        val expression: Expr
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitExpressionStmt(this)
    }

    data class Print(
        val expression: Expr
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitPrintStmt(this)
    }

    data class Var(
        val name: Token,
    ) : Stmt {
        var isInitialized: Boolean = false
            private set
        var initializer: Expr? = null
            private set

        constructor(name: Token, initializer: Expr?) : this(name) {
            this.isInitialized = true
            this.initializer = initializer
        }

        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitVarStmt(this)
    }
}