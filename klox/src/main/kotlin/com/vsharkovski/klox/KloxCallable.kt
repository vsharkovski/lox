package com.vsharkovski.klox

interface KloxCallable {
    val arity: Int

    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}