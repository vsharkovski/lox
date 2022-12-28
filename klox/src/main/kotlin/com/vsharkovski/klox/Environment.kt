package com.vsharkovski.klox

class Environment(
    private val enclosingEnvironment: Environment? = null
) {
    private val values = mutableMapOf<String, Any?>()

    fun get(name: Token): Any? =
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme]
        } else if (enclosingEnvironment != null) {
            enclosingEnvironment.get(name)
        } else {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }

    fun assign(name: Token, value: Any?): Unit =
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
        } else if (enclosingEnvironment != null) {
            enclosingEnvironment.assign(name, value)
        } else {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    private fun ancestor(distance: Int): Environment {
        var environment = this
        for (i in 0 until distance) {
            assert(environment.enclosingEnvironment != null)
            environment = environment.enclosingEnvironment!!
        }
        return environment
    }

    fun getAt(distance: Int, name: String): Any? {
        val targetEnvironment = ancestor(distance)
        assert(targetEnvironment.values.containsKey(name))
        return targetEnvironment.values[name]
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance).values[name.lexeme] = value
    }
}
