package com.vsharkovski.klox

class Environment {
    private val values = mutableMapOf<String, Any?>()

    fun get(name: Token): Any? =
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme]
        } else {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'")
        }

    fun define(name: String, value: Any?) {
        values[name] = value
    }
}