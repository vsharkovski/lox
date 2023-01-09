#include <stdio.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "value.h"
#include "vm.h"

#define ALLOCATE_OBJ(type, objectType) \
    (type*)allocateObject(sizeof(type), objectType);

/**
 * @brief Allocate an object with a given size and type. 
 */
static Obj* allocateObject(size_t size, ObjType type)
{
    Obj* object = (Obj*)reallocate(NULL, 0, size);
    object->type = type;

    // Add the object to the global object linked list.
    object->next = vm.objects;
    vm.objects = object;
    return object;
}

/**
 * @brief Allocate a string object. 
 * @param chars The string's buffer.
 * @param length The length of the buffer.
 */
static ObjString* allocateString(char* chars, int length)
{
    ObjString* string = ALLOCATE_OBJ(ObjString, OBJ_STRING);
    string->length = length;
    string->chars = chars;
    return string;
}

/**
 * @brief Create a string object with a given buffer.
 * @param chars The string's buffer.
 * @param length The string length.
 * @return Pointer to the constructed string.
 */
ObjString* takeString(char* chars, int length)
{
    return allocateString(chars, length);
}

/**
 * @brief Create a string object with a buffer
 * which is a copy of the provided buffer.
 * @param chars The source buffer to copy from.
 * @param length The string length.
 * @return Pointer to the constructed string.
 */
ObjString* copyString(const char* chars, int length)
{
    char* heapChars = ALLOCATE(char, length + 1);
    memcpy(heapChars, chars, length);
    heapChars[length] = '\0';
    return allocateString(heapChars, length);
}

void printObject(Value value)
{
    switch (OBJ_TYPE(value))
    {
    case OBJ_STRING:
        printf("%s", AS_CSTRING(value));
        break;
    }
}
