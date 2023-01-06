package com.vsharkovski.klox

class KloxClass(
    val name: String,
    private val methods: Map<String, KloxFunction>
) : KloxCallable {
    fun findMethod(name: String): KloxFunction? = methods[name]

    override val arity: Int = 0

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any = KloxInstance(this)

    override fun toString(): String = name
}