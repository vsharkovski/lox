package com.vsharkovski.klox

class KloxInstance(private val klass: KloxClass) {
    private val fields = mutableMapOf<String, Any?>()

    fun get(name: Token): Any? =
        if (fields.containsKey(name.lexeme)) {
            fields[name.lexeme]
        } else {
            throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
        }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }

    override fun toString(): String = "${klass.name} instance"
}