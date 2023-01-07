package com.vsharkovski.klox

class KloxInstance(private val klass: KloxClass) {
    private val fields = mutableMapOf<String, Any?>()

    fun get(name: Token): Any? =
        if (fields.containsKey(name.lexeme)) {
            // Return the field with this name.
            fields[name.lexeme]
        } else {
            // Return the method with this name, bound to this instance,
            // or throw a runtime error.
            klass.findMethod(name.lexeme)?.bind(this)
                ?: throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
        }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }

    override fun toString(): String = "${klass.name} instance"
}