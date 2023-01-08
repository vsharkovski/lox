#include <stdio.h>
#include <stdlib.h>

#include "common.h"
#include "compiler.h"
#include "scanner.h"

typedef struct
{
    Token current;
    Token previous;
    bool hadError;
    bool panicMode;
} Parser;

Parser parser;
Chunk* compilingChunk;

/**
 * @brief Get the current chunk being compiled. 
 */
static Chunk* currentChunk()
{
    return compilingChunk;
}

/**
 * @brief Error at a token, with an error message. 
 */
static void errorAt(Token* token, const char* message)
{
    if (parser.panicMode) return;

    parser.panicMode = true;
    fprintf(stderr, "[line %d] Error", token->line);

    if (token->type == TOKEN_EOF)
    {
        fprintf(stderr, " at end");
    }
    else if (token->type == TOKEN_ERROR)
    {
        // Nothing.
    }
    else
    {
        fprintf(stderr, " at '%.*s'", token->length, token->start);
    }

    fprintf(stderr, ": %s\n", message);
    parser.hadError = true;
}

/**
 * @brief Error at the previously scanned token.
 */
static void error(const char* message)
{
    errorAt(&parser.previous, message);
}

/**
 * @brief Error at the current scanned token.
 */
static void errorAtCurrent(const char* message)
{
    errorAt(&parser.current, message);
}

/**
 * @brief Scan a token. If an error occurs,
 * keep scanning until otherwise.
 */
static void advance()
{
    parser.previous = parser.current;

    while (true)
    {
        parser.current = scanToken();
        if (parser.current.type != TOKEN_ERROR) break;

        errorAtCurrent(parser.current.start);
    }
}

/**
 * @brief Consume a token, or emit an error with a message. 
 */
static void consume(TokenType type, const char* message)
{
    if (parser.current.type == type)
    {
        advance();
        return;
    }

    errorAtCurrent(message);
}

/**
 * @brief Write a byte to the current chunk.
 */
static void emitByte(uint8_t byte)
{
    writeChunk(currentChunk(), byte, parser.previous.line);
}

/**
 * @brief Emit byte1. Then, emit byt2. 
 */
static void emitBytes(uint8_t byte1, uint8_t byte2)
{
    emitByte(byte1);
    emitByte(byte2);
}

/**
 * @brief Emit a return instruction.
 */

static void emitReturn()
{
    emitByte(OP_RETURN);
}

static void endCompiler()
{
    emitReturn();
}

static void expression()
{

}

/**
 * @brief Compile source into a chunk.
 * @return True if there was no error.
 * @return False if there was a parser error.
 */
bool compile(const char* source, Chunk* chunk)
{
    initScanner(source);
    compilingChunk = chunk;

    parser.hadError = false;
    parser.panicMode = false;

    advance();
    expression();
    consume(TOKEN_EOF, "Expect end of expression.");
    endCompiler();
    return !parser.hadError;
}
