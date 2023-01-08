#include <stdio.h>

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
    printf("%g", value);
}
