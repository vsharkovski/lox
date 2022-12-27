package com.vsharkovski.klox

sealed interface Stmt {
    interface Visitor<R> {
        fun visitBlockStmt(stmt: Block): R
        fun visitBreakStmt(stmt: Break): R
//        fun visitClassStmt(stmt: Class): R
        fun visitExpressionStmt(stmt: Expression): R
//        fun visitFunctionStmt(stmt: Function): R
        fun visitIfStmt(stmt: If): R
        fun visitPrintStmt(stmt: Print): R
//        fun visitReturnStmt(stmt: Return): R
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

    object Break : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitBreakStmt(this)
    }

    data class Expression(
        val expression: Expr
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitExpressionStmt(this)
    }

    data class If(
        val condition: Expr,
        val thenBranch: Stmt,
        val elseBranch: Stmt?
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitIfStmt(this)
    }

    data class Print(
        val expression: Expr
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitPrintStmt(this)
    }

    sealed interface Var : Stmt {
        val name: Token

        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitVarStmt(this)
    }

    data class UninitializedVar(override val name: Token) : Var

    data class InitializedVar(override val name: Token, val initializer: Expr) : Var

    data class While(
        val condition: Expr,
        val body: Stmt
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitWhileStmt(this)
    }
}