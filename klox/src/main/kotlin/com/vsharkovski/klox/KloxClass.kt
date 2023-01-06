package com.vsharkovski.klox

class KloxClass(val name: String) : KloxCallable {
    override fun toString(): String = name

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any = KloxInstance(this)

    override val arity: Int = 0
}