package com.vsharkovski.klox

class Environment(
    private val enclosingEnvironment: Environment? = null
) {
    private val values = mutableMapOf<String, EnvironmentValue>()

    fun get(name: Token): Any? {
        val currentEntry = values[name.lexeme]
        return if (currentEntry != null) {
            if (currentEntry.isInitialized)
                currentEntry.value
            else
                throw RuntimeError(name, "Attempt to use uninitialized variable '${name.lexeme}'.")
        } else if (enclosingEnvironment != null) {
            enclosingEnvironment.get(name)
        } else {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
    }

    fun assign(name: Token, value: Any?): Unit =
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = EnvironmentValue(true, value)
        } else if (enclosingEnvironment != null) {
            enclosingEnvironment.assign(name, value)
        } else {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }

    fun define(name: String) {
        values[name] = EnvironmentValue(false, null)
    }

    fun define(name: String, value: Any?) {
        values[name] = EnvironmentValue(true, value)
    }
}

private data class EnvironmentValue(
    val isInitialized: Boolean,
    val value: Any?
)
