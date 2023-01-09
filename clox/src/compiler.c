#include <stdio.h>
#include <stdlib.h>

#include "common.h"
#include "compiler.h"
#include "scanner.h"

#ifdef DEBUG_PRINT_CODE
#include "debug.h"
#endif

/**
 * @brief Parser struct.
 * @param current The next to be consumed token.
 * @param previous The last consumed token.
 */
typedef struct
{
    Token current;
    Token previous;
    bool hadError;
    bool panicMode;
} Parser;

typedef enum
{
    PREC_NONE,
    PREC_ASSIGNMENT, // =
    PREC_OR,         // or
    PREC_AND,        // and
    PREC_EQUALITY,   // == !=
    PREC_COMPARISON, // < > <= >=
    PREC_TERM,       // + -
    PREC_FACTOR,     // * /
    PREC_UNARY,      // ! -
    PREC_CALL,       // . ()
    PREC_PRIMARY
} Precedence;

typedef void (*ParseFn)();

typedef struct
{
    ParseFn prefix;
    ParseFn infix;
    Precedence precedence;
} ParseRule;


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
 * @brief Error at the last consumed token.
 */
static void error(const char* message)
{
    errorAt(&parser.previous, message);
}

/**
 * @brief Error at the next to be consumed token.
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
 * @brief Consume the next token if it has a given type.
 * Otherwise emit an error.
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
 * @brief Emit byte1. Then, emit byte2. 
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

/**
 * @brief Create a constant with the given value
 * in the current chunk.
 * @return The index of the constant in the pool table.
 */
static uint8_t makeConstant(Value value)
{
    int constant = addConstant(currentChunk(), value);
    if (constant > UINT8_MAX)
    {
        error("Too many constants in one chunk.");
        return 0;
    }

    return (uint8_t)constant;
}

/**
 * @brief Create a constant with the given value 
 * in the current chunk. Then, emit a constant
 * instruction and the byte to push the constant
 * to the stack at runtime.
 */
static void emitConstant(Value value)
{
    emitBytes(OP_CONSTANT, makeConstant(value));
}

static void endCompiler()
{
    emitReturn();
#ifdef DEBUG_PRINT_CODE
    if (!parser.hadError)
    {
        disassembleChunk(currentChunk(), "code");
    }
#endif
}

static void expression();
static ParseRule* getRule(TokenType type);
static void parsePrecedence(Precedence precedence);

/**
 * @brief Parse a left-associative binary expression.
 * It is assumed that the first operand has already been
 * compiled, and that the operator was just consumed.
 */
static void binary()
{
    TokenType operatorType = parser.previous.type;

    // Compile the right operand by parsing at the
    // correct precedence level (one above the operator's).
    // This way the operation is left-associative.
    ParseRule* rule = getRule(operatorType);
    parsePrecedence((Precedence)(rule->precedence + 1));

    // Emit the appropriate bytecode instruction.
    switch (operatorType)
    {
    case TOKEN_BANG_EQUAL:    emitBytes(OP_EQUAL, OP_NOT);   break;
    case TOKEN_EQUAL_EQUAL:   emitByte(OP_EQUAL);            break;
    case TOKEN_GREATER:       emitByte(OP_GREATER);          break;
    case TOKEN_GREATER_EQUAL: emitBytes(OP_LESS, OP_NOT);    break;
    case TOKEN_LESS:          emitByte(OP_LESS);             break;
    case TOKEN_LESS_EQUAL:    emitBytes(OP_GREATER, OP_NOT); break;
    case TOKEN_PLUS:          emitByte(OP_ADD);              break;
    case TOKEN_MINUS:         emitByte(OP_SUBTRACT);         break;
    case TOKEN_STAR:          emitByte(OP_MULTIPLY);         break;
    case TOKEN_SLASH:         emitByte(OP_DIVIDE);           break;
    default:                  return; // Unreachable.
    }
}

/**
 * @brief Parse a literal.
 * It is assumed that the keyword token was just consumed.
 */
static void literal()
{
    switch (parser.previous.type)
    {
    case TOKEN_FALSE: emitByte(OP_FALSE); break;
    case TOKEN_NIL:   emitByte(OP_NIL);   break;
    case TOKEN_TRUE:  emitByte(OP_TRUE);  break;
    default:          return; // Unreachable.
    }
}

/**
 * @brief Parse a parenthetical grouping expression.
 * It is assumed that the opening parenthesis was just consumed.
 */
static void grouping()
{
    expression();
    consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression.");
}

/**
 * @brief Parse a number.
 * It is assumed that the number's token was just consumed.
 */
static void number()
{
    // Convert the previously consumed token's lexeme
    // to a double value, and emit it as a constant.
    double value = strtod(parser.previous.start, NULL);
    emitConstant(NUMBER_VAL(value));
}

/**
 * @brief Parse a unary expression.
 * It is assumed that operand was just consumed.
 */
static void unary()
{
    TokenType operatorType = parser.previous.type;

    // Compile the operand.
    parsePrecedence(PREC_UNARY);

    // Emit the operator instruction.
    switch (operatorType)
    {
    case TOKEN_BANG:  emitByte(OP_NOT);    break;
    case TOKEN_MINUS: emitByte(OP_NEGATE); break;
    default:          return; // Unreachable.
    }
}

ParseRule rules[] = {
    [TOKEN_LEFT_PAREN]    = {grouping, NULL,   PREC_NONE},
    [TOKEN_RIGHT_PAREN]   = {NULL,     NULL,   PREC_NONE},
    [TOKEN_LEFT_BRACE]    = {NULL,     NULL,   PREC_NONE},
    [TOKEN_RIGHT_BRACE]   = {NULL,     NULL,   PREC_NONE},
    [TOKEN_COMMA]         = {NULL,     NULL,   PREC_NONE},
    [TOKEN_DOT]           = {NULL,     NULL,   PREC_NONE},
    [TOKEN_MINUS]         = {unary,    binary, PREC_TERM},
    [TOKEN_PLUS]          = {NULL,     binary, PREC_TERM},
    [TOKEN_SEMICOLON]     = {NULL,     NULL,   PREC_NONE},
    [TOKEN_SLASH]         = {NULL,     binary, PREC_FACTOR},
    [TOKEN_STAR]          = {NULL,     binary, PREC_FACTOR},
    [TOKEN_BANG]          = {unary,    NULL,   PREC_NONE},
    [TOKEN_BANG_EQUAL]    = {NULL,     binary, PREC_EQUALITY},
    [TOKEN_EQUAL]         = {NULL,     NULL,   PREC_NONE},
    [TOKEN_EQUAL_EQUAL]   = {NULL,     binary, PREC_EQUALITY},
    [TOKEN_GREATER]       = {NULL,     binary, PREC_COMPARISON},
    [TOKEN_GREATER_EQUAL] = {NULL,     binary, PREC_COMPARISON},
    [TOKEN_LESS]          = {NULL,     binary, PREC_COMPARISON},
    [TOKEN_LESS_EQUAL]    = {NULL,     binary, PREC_COMPARISON},
    [TOKEN_IDENTIFIER]    = {NULL,     NULL,   PREC_NONE},
    [TOKEN_STRING]        = {NULL,     NULL,   PREC_NONE},
    [TOKEN_NUMBER]        = {number,   NULL,   PREC_NONE},
    [TOKEN_AND]           = {NULL,     NULL,   PREC_NONE},
    [TOKEN_CLASS]         = {NULL,     NULL,   PREC_NONE},
    [TOKEN_ELSE]          = {NULL,     NULL,   PREC_NONE},
    [TOKEN_FALSE]         = {literal,  NULL,   PREC_NONE},
    [TOKEN_FOR]           = {NULL,     NULL,   PREC_NONE},
    [TOKEN_FUN]           = {NULL,     NULL,   PREC_NONE},
    [TOKEN_IF]            = {NULL,     NULL,   PREC_NONE},
    [TOKEN_NIL]           = {literal,  NULL,   PREC_NONE},
    [TOKEN_OR]            = {NULL,     NULL,   PREC_NONE},
    [TOKEN_PRINT]         = {NULL,     NULL,   PREC_NONE},
    [TOKEN_RETURN]        = {NULL,     NULL,   PREC_NONE},
    [TOKEN_SUPER]         = {NULL,     NULL,   PREC_NONE},
    [TOKEN_THIS]          = {NULL,     NULL,   PREC_NONE},
    [TOKEN_TRUE]          = {literal,  NULL,   PREC_NONE},
    [TOKEN_VAR]           = {NULL,     NULL,   PREC_NONE},
    [TOKEN_WHILE]         = {NULL,     NULL,   PREC_NONE},
    [TOKEN_ERROR]         = {NULL,     NULL,   PREC_NONE},
    [TOKEN_EOF]           = {NULL,     NULL,   PREC_NONE},
};

/**
 * @brief Parse an infix expression at the given precedence
 * level or higher. If cannot do that, parse a prefix expression.
 */
static void parsePrecedence(Precedence precedence)
{
    // Consume the next token and find the prefix parser for it.
    advance();
    ParseFn prefixRule = getRule(parser.previous.type)->prefix;
    if (prefixRule == NULL)
    {
        // The token is not part of a prefix expression,
        // which is a syntax error.
        error("Expect expression.");
        return;
    }

    // Compile the rest of the prefix expression.
    prefixRule(); 

    // While there is an infix parser for the next token,
    // and that infix parser has a precedence >= our precedence,
    // consume the token (which is an infix operator) and
    // compile the rest of the infix expression.
    while (precedence <= getRule(parser.current.type)->precedence)
    {
        advance();
        ParseFn infixRule = getRule(parser.previous.type)->infix;
        infixRule();
    }
}

/**
 * @brief Get a parse rule associated with a token type.
 */
static ParseRule* getRule(TokenType type)
{
    return &rules[type];
}

/**
 * @brief Parse an expression.
 */
static void expression()
{
    parsePrecedence(PREC_ASSIGNMENT);
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
