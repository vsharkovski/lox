#include <stdio.h>
#include <string.h>

#include "object.h"
#include "memory.h"
#include "value.h"

/**
 * @brief Initialize a value array. 
 */
void initValueArray(ValueArray* array)
{
    array->count = 0;
    array->capacity = 0;
    array->values = NULL;
}

/**
 * @brief Write a value to a value array. 
 */
void writeValueArray(ValueArray* array, Value value)
{
    if (array->count == array->capacity)
    {
        // Not enough space in the allocated array.
        // Grow the array to make room.
        int oldCapacity = array->capacity;
        array->capacity = GROW_CAPACITY(oldCapacity);
        array->values = GROW_ARRAY(Value, array->values, oldCapacity, array->capacity);
    }

    array->values[array->count] = value;
    array->count++;
}

/**
 * @brief Free a value array from memory.
 */
void freeValueArray(ValueArray* array)
{
    FREE_ARRAY(Value, array->values, array->capacity);
    initValueArray(array);
}

/**
 * @brief Print a value to stdout. 
 */
void printValue(Value value)
{
    switch(value.type)
    {
    case VAL_BOOL:
        printf(AS_BOOL(value) ? "true" : "false");
        break;
    case VAL_NIL:
        printf("nil");
        break;
    case VAL_NUMBER:
        printf("%g", AS_NUMBER(value));
        break;
    case VAL_OBJ:
        printObject(value);
        break;
    }
}

/**
 * @brief Compare equality of two values. 
 */
bool valuesEqual(Value a, Value b)
{
    if (a.type != b.type) return false;
    switch (a.type)
    {
    case VAL_BOOL:
        return AS_BOOL(a) == AS_BOOL(b);

    case VAL_NIL:
        return true;

    case VAL_NUMBER:
        return AS_NUMBER(a) == AS_NUMBER(b);

    case VAL_OBJ:
        ObjString* aString = AS_STRING(a);
        ObjString* bString = AS_STRING(b);
        return aString->length == bString->length &&
            memcmp(aString->chars, bString->chars, aString->length) == 0;

    default:
        return false; // Unreachable.
    }
}
