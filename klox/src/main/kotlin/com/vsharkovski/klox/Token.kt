package com.vsharkovski.klox

// If Token is a data class, there will be issues with resolving and binding, where
// a for loop of the form "for (var i = 0; i < 5; i = i+1)" will misidentify a token
// in the last expression.

class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int
) {
    override fun toString(): String =
        "Token(type=$type, lexeme=$lexeme, literal=$literal, line=$line)"
}
