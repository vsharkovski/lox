#include "common.h"
#include "chunk.h"
#include "debug.h"

int main(int argc, const char* argv[])
{
    Chunk chunk;
    initChunk(&chunk);

    int constant = addConstant(&chunk, 1.2);
    writeChunk(&chunk, OP_CONSTANT, 5);
    writeChunk(&chunk, constant, 5);

    writeChunk(&chunk, OP_RETURN, 5);

    writeChunk(&chunk, OP_RETURN, 7);
    
    disassembleChunk(&chunk, "test chunk");
    
    freeChunk(&chunk);
    return 0;
}
