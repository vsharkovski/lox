#include <stdio.h>

#include "debug.h"

void disassembleChunk(Chunk* chunk, const char* name)
{
    printf("== %s ==\n", name);
    
    int lineIndex = 0;
    ChunkLineInfo lineInfo = chunk->lines.info[lineIndex];
    // In lineInfo.count we track how many instructions are remaining in this line.
    
    for (int offset = 0; offset < chunk->count;)
    {
        bool changedLine = false;
        lineInfo.count--;
        if (lineInfo.count == 0)
        {
            lineIndex++;
            lineInfo = chunk->lines.info[lineIndex];
            changedLine = true;
        }

        offset = disassembleInstruction(chunk, offset, (offset == 0 || changedLine) ? lineInfo.number : -1);
    }
}

static int simpleInstruction(const char* name, int offset)
{
    printf("%s\n", name);
    return offset + 1;
}

static int constantInstruction(const char* name, Chunk* chunk, int offset)
{
    uint8_t constant = chunk->code[offset + 1];
    printf("%-16s %4d '", name, constant);
    printValue(chunk->constants.values[constant]);
    printf("'\n");
    return offset + 2;
}

int disassembleInstruction(Chunk* chunk, int offset, int lineNumber)
{
    printf("%04d ", offset);
    if (lineNumber == -1) {
        // No line number passed.
        printf("   | ");
    } else {
        printf("%4d ", lineNumber);
    }

    uint8_t instruction = chunk->code[offset];
    switch (instruction)
    {
    case OP_CONSTANT:
        return constantInstruction("OP_CONSTANT", chunk, offset);
    case OP_ADD:
        return simpleInstruction("OP_ADD", offset);
    case OP_SUBTRACT:
        return simpleInstruction("OP_SUBTRACT", offset);
    case OP_MULTIPLY:
        return simpleInstruction("OP_MULTIPLY", offset);
    case OP_DIVIDE:
        return simpleInstruction("OP_DIVIDE", offset);
    case OP_NEGATE:
        return simpleInstruction("OP_NEGATE", offset);
    case OP_RETURN:
        return simpleInstruction("OP_RETURN", offset);
    default:
        printf("Unknown opcode %d\n", instruction);
        return offset + 1;
    }
}
