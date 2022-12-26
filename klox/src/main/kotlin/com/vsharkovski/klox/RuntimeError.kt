package com.vsharkovski.klox

class RuntimeError(
    val token: Token,
    override val message: String
) : RuntimeException(message)