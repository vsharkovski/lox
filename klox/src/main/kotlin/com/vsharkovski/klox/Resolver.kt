package com.vsharkovski.klox

class Resolver(
    private val interpreter: Interpreter
) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private enum class FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum class ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }

    private val scopes = mutableListOf<MutableMap<String, Boolean>>()
    private var currentFunction = FunctionType.NONE
    private var currentClass = ClassType.NONE
    private var isInsideLoop = false

    fun resolve(statements: List<Stmt>) {
        for (statement in statements)
            resolve(statement)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        if (!isInsideLoop)
            Klox.error(stmt.keyword, "Can't break if not inside a loop.")
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS

        declare(stmt.name)
        define(stmt.name)

        if (stmt.superclass != null) {
            if (stmt.name.lexeme == stmt.superclass.name.lexeme)
                Klox.error(stmt.superclass.name, "A class can't inherit from itself.")

            currentClass = ClassType.SUBCLASS
            resolve(stmt.superclass)
        }

        if (stmt.superclass != null) {
            beginScope()
            scopes.last()["super"] = true
        }

        beginScope()
        scopes.last()["this"] = true

        for (method in stmt.methods) {
            val declaration =
                if (method.name.lexeme == "init") FunctionType.INITIALIZER
                else FunctionType.METHOD

            resolveFunction(method, declaration)
        }

        endScope()

        if (stmt.superclass != null) endScope()

        currentClass = enclosingClass
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) =
        resolve(stmt.expression)

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null)
            resolve(stmt.elseBranch)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE)
            Klox.error(stmt.keyword, "Can't return from top-level code.")

        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Klox.error(stmt.keyword, "Can't return a value from an initializer.")
            }
            resolve(stmt.value)
        }
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        stmt.initializer?.let { resolve(it) }
        define(stmt.name)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)

        val isInsideEnclosingLoop = isInsideLoop
        isInsideLoop = true

        resolve(stmt.body)

        isInsideLoop = isInsideEnclosingLoop
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)
        for (argument in expr.arguments)
            resolve(argument)
    }

    override fun visitGetExpr(expr: Expr.Get) =
        resolve(expr.obj)

    override fun visitGroupingExpr(expr: Expr.Grouping) =
        resolve(expr.expression)

    override fun visitLiteralExpr(expr: Expr.Literal) {}

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitSetExpr(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitSuperExpr(expr: Expr.Super) =
        when (currentClass) {
            ClassType.NONE ->
                Klox.error(expr.keyword, "Can't use 'super' outside of a class.")
            ClassType.CLASS ->
                Klox.error(expr.keyword, "Can't use 'super' in a class with no superclass.")
            ClassType.SUBCLASS ->
                resolveLocal(expr, expr.keyword)
        }

    override fun visitTernaryExpr(expr: Expr.Ternary) {
        resolve(expr.left)
        resolve(expr.middle)
        resolve(expr.right)
    }

    override fun visitThisExpr(expr: Expr.This) =
        if (currentClass == ClassType.NONE) {
            Klox.error(expr.keyword, "Can't use 'this' outside of a class.")
        } else {
            resolveLocal(expr, expr.keyword)
        }

    override fun visitUnaryExpr(expr: Expr.Unary) =
        resolve(expr.right)

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (scopes.isNotEmpty() && scopes.last()[expr.name.lexeme] == false)
            Klox.error(expr.name, "Can't read local variable in its own initializer.")

        resolveLocal(expr, expr.name)
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val isInsideEnclosingLoop = isInsideLoop
        isInsideLoop = false

        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()

        currentFunction = enclosingFunction
        isInsideLoop = isInsideEnclosingLoop
    }

    private fun beginScope() {
        scopes.add(mutableMapOf())
    }

    private fun endScope() {
        scopes.removeLast()
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return

        val scope = scopes.last()
        if (scope.containsKey(name.lexeme))
            Klox.error(name, "Already a variable with this name in this scope.")

        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.last()[name.lexeme] = true
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.size - 1 downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }
}