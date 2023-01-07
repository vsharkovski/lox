package com.vsharkovski.klox

class KloxFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment,
    private val isInitializer: Boolean
) : KloxCallable {
    override val arity = declaration.params.size

    /**
     * Generate a function with an environment where 'this'
     * is the given instance.
     */
    fun bind(instance: KloxInstance): KloxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return KloxFunction(declaration, environment, isInitializer)
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (i in 0 until declaration.params.size)
            environment.define(declaration.params[i].lexeme, arguments[i])

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            return if (isInitializer) closure.getAt(0, "this")
            else returnValue.value
        }

        return if (isInitializer) closure.getAt(0, "this")
        else null
    }

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"
}
