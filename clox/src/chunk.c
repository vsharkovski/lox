#include <stdlib.h>

#include "chunk.h"
#include "memory.h"

/**
 * @brief Initialize chunk line data.
 */
static void initChunkLines(ChunkLines* lines)
{
    lines->capacity = 0;
    lines->count = 0;
    lines->data = NULL;
}

/**
 * @brief Free chunk line data from memory. 
 */
static void freeChunkLines(ChunkLines* lines)
{
    FREE_ARRAY(ChunkLineData, lines->data, lines->capacity);
    initChunkLines(lines);
}

/**
 * @brief Update line info given the line number of a byte currently being written.
 */
static inline void addLineData(ChunkLines* lines, int lineNumber)
{
    if (lines->count == 0 || lines->data[lines->count - 1].number != lineNumber)
    {
        // Last line number is not this bytes's. Add new line data.
        if (lines->count == lines->capacity)
        {
            // Not enough space in the allocated array. Grow the array to make room.
            int oldCapacity = lines->capacity;
            lines->capacity = GROW_CAPACITY(oldCapacity);
            lines->data = GROW_ARRAY(ChunkLineData, lines->data, oldCapacity, lines->capacity);
        }

        ChunkLineData* lineData = &lines->data[lines->count];
        lineData->number = lineNumber;
        lineData->count = 1;

        lines->count++;
    }
    else
    {
        // Increase byte count for this line.
        lines->data[lines->count - 1].count++;
    }
}

/**
 * @brief Initialize a chunk.
 */
void initChunk(Chunk* chunk)
{
    chunk->count = 0;
    chunk->capacity = 0;
    chunk->code = NULL;
    initChunkLines(&chunk->lines);
    initValueArray(&chunk->constants);
}

/**
 * @brief Free a chunk from memory.
 */
void freeChunk(Chunk* chunk)
{
    FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
    freeChunkLines(&chunk->lines);
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
    }

    chunk->code[chunk->count] = byte;
    chunk->count++;
    addLineData(&chunk->lines, line);
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

/**
 * @brief Get the Line number of an instruction.
 * @param chunk The instruction's chunk.
 * @param offset The offset of the instruction in the chunk.
 * @return The line number of the instruction.
 */
int getLine(Chunk* chunk, int offset)
{
    int lineIndex = 0;

    // Jump forward 'offset' times and keep track which line we are at.
    while (offset > 0)
    {
        int instructionsInLine = chunk->lines.data[lineIndex].count;

        if (offset - instructionsInLine >= 0)
        {
            // Jump across all bytes for this line and move to the next.
            offset -= instructionsInLine;
            lineIndex++;
        }
        else
        {
            // This line has more bytes than we have remaining to jump.
            break;
        }
    }

    return chunk->lines.data[lineIndex].number;
}
