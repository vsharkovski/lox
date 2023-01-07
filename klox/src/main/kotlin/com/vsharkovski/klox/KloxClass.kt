package com.vsharkovski.klox

class KloxClass(
    val name: String,
    private val superclass: KloxClass?,
    private val methods: Map<String, KloxFunction>
) : KloxCallable {
    fun findMethod(name: String): KloxFunction? =
        methods[name] ?: superclass?.findMethod(name)

    override val arity: Int = findMethod("init")?.arity ?: 0

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        // Create instance whose class is this object.
        val instance = KloxInstance(this)

        // If an initializer method is present, immediately bind it and call it.
        findMethod("init")?.bind(instance)?.call(interpreter, arguments)

        return instance
    }

    override fun toString(): String = name
}