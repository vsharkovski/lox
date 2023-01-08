#include <stdio.h>

#include "common.h"
#include "scanner.h"
#include "compiler.h"

void compile(const char* source)
{
    initScanner(source);

    int line = -1;
    while (true)
    {
        Token token = scanToken();
        if (token.line == line)
        {
            printf("   | ");
        }
        else
        {
            printf("%4d ", token.line);
            line = token.line;
        }
        printf("%2d '%.*s'\n", token.type, token.length, token.start);

        if (token.type == TOKEN_EOF) break;
    }
}
