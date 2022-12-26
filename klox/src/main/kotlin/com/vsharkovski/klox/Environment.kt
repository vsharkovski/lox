package com.vsharkovski.klox

class Environment(
    private val enclosingEnvironment: Environment? = null
) {
    private sealed interface Value
    private object UninitializedValue : Value
    private data class InitializedValue(val value: Any?) : Value

    private val values = mutableMapOf<String, Value>()

    fun get(name: Token): Any? {
        val currentEntry = values[name.lexeme]
        return if (currentEntry != null) {
            when (currentEntry) {
                is InitializedValue ->
                    currentEntry.value
                is UninitializedValue ->
                    throw RuntimeError(name, "Attempt to use uninitialized variable '${name.lexeme}'.")
            }
        } else if (enclosingEnvironment != null) {
            enclosingEnvironment.get(name)
        } else {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
    }

    fun assign(name: Token, value: Any?): Unit =
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = InitializedValue(value)
        } else if (enclosingEnvironment != null) {
            enclosingEnvironment.assign(name, value)
        } else {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }

    fun define(name: String) {
        values[name] = UninitializedValue
    }

    fun define(name: String, value: Any?) {
        values[name] = InitializedValue(value)
    }
}
