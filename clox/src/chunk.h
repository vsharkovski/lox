#ifndef CLOX_CHUNK_H
#define CLOX_CHUNK_H

#include "common.h"
#include "value.h"

/**
 * @brief Operation codes.
 */
typedef enum
{
    OP_CONSTANT,
    OP_NIL,
    OP_TRUE,
    OP_FALSE,
    OP_POP,
    OP_EQUAL,
    OP_GREATER,
    OP_LESS,
    OP_ADD,
    OP_SUBTRACT,
    OP_MULTIPLY,
    OP_DIVIDE,
    OP_NOT,
    OP_NEGATE,
    OP_PRINT,
    OP_RETURN
} OpCode;

typedef struct
{
    int number;
    int count;
} ChunkLineData;

/**
 * @brief Contains line information for each line with at least one instruction.
 */
typedef struct
{
    int capacity;
    int count;
    ChunkLineData* data;
} ChunkLines;

/**
 * @brief A chunk of bytecode instructions.
 */
typedef struct
{
    int capacity;
    int count;
    uint8_t* code;
    ChunkLines lines;
    ValueArray constants;
} Chunk;

void initChunk(Chunk* chunk);
void freeChunk(Chunk* chunk);
void writeChunk(Chunk* chunk, uint8_t byte, int line);
int addConstant(Chunk* chunk, Value value);

int getLine(Chunk* chunk, int offset);

#endif
