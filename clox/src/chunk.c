#include <stdlib.h>

#include "chunk.h"
#include "memory.h"

/**
 * @brief Initialize a chunk.
 */
void initChunk(Chunk* chunk)
{
    chunk->count = 0;
    chunk->capacity = 0;
    chunk->code = NULL;
    chunk->lines = NULL;
    initValueArray(&chunk->constants);
}

/**
 * @brief Free a chunk from memory.
 */
void freeChunk(Chunk* chunk)
{
    FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
    FREE_ARRAY(int, chunk->lines, chunk->capacity);
    freeValueArray(&chunk->constants);
    initChunk(chunk);
}

/**
 * @brief Write a byte to a chunk.
 */
void writeChunk(Chunk* chunk, uint8_t byte, int line)
{
    if (chunk->count == chunk->capacity)
    {
        // Not enough space in the allocated array of the chunk.
        // Grow the array to make room.
        int oldCapacity = chunk->capacity;
        chunk->capacity = GROW_CAPACITY(oldCapacity);
        chunk->code = GROW_ARRAY(uint8_t, chunk->code, oldCapacity, chunk->capacity);
        chunk->lines = GROW_ARRAY(int, chunk->lines, oldCapacity, chunk->capacity);
    }

    chunk->code[chunk->count] = byte;
    chunk->lines[chunk->count] = line;
    chunk->count++;
}

/**
 * @brief Add a constant to a chunk's constant pool.
 * @return The index where the constant was appended.
 */
int addConstant(Chunk* chunk, Value value)
{
    int index = chunk->constants.count;
    writeValueArray(&chunk->constants, value);
    return index;
}
