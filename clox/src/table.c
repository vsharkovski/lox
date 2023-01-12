#include <stdlib.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "table.h"
#include "value.h"

#define TABLE_MAX_LOAD 0.75

/**
 * @brief Initialize empty hash table.
 */
void initTable(Table* table)
{
    table->count = 0;
    table->capacity = 0;
    table->entries = NULL;
}

/**
 * @brief Free hash table from memory. 
 */
void freeTable(Table* table)
{
    FREE_ARRAY(Entry, table->entries, table->capacity);
    initTable(table);
}

/**
 * @brief Find the appropriate place for a key in a table.
 */
static Entry* findEntry(Entry* entries, int capacity, ObjString* key)
{
    uint32_t index = key->hash % capacity;
    Entry* tombstone = NULL; // The first tombstone we pass.

    // Do linear probing.
    while (true)
    {
        while (index < capacity)
        {
            Entry* entry = entries + index;
            if (entry->key == NULL)
            {
                if (IS_NIL(entry->value))
                {
                    // Empty entry. If we have passed a tombstone,
                    // return it instead. Otherwise this is the first
                    // actually empty entry, so return it.
                    return tombstone != NULL ? tombstone : entry;
                }
                else
                {
                    // We found a tombstone.
                    if (tombstone == NULL) tombstone = entry;
                }
            }
            else if (entry->key == key)
            {
                // We found the key.
                return entry;
            }

            index++;
        }

        index = 0;
    }
}

/**
 * @brief Get an entry from a table and store its value.
 * @param table The table to search.
 * @param key The key to search for.
 * @param value The place to store the found entry's value.
 * @return Whether the entry was found.
 */
bool tableGet(Table* table, ObjString* key, Value* value)
{
    if (table->count == 0) return false;

    Entry* entry = findEntry(table->entries, table->capacity, key);
    if (entry->key == NULL) return false;

    *value = entry->value;
    return true;
}

/**
 * @brief Delete an entry from a table.
 * @return Whether the entry was found and deleted.
 */
bool tableDelete(Table* table, ObjString* key)
{
    if (table->count == 0) return false;

    // Find the entry.
    Entry* entry = findEntry(table->entries, table->capacity, key);
    if (entry->key == NULL) return false;

    // Place a tombstone in the entry.
    entry->key = NULL;
    entry->value = BOOL_VAL(true);
    return true;
}

/**
 * @brief Adjust table capacity to new capacity.
 * All old entries are copied over to the new array.
 */
static void adjustCapacity(Table* table, int capacity)
{
    // Allocate new array.
    Entry* entries = ALLOCATE(Entry, capacity);
    for (int i = 0; i < capacity; i++)
    {
        entries[i].key = NULL;
        entries[i].value = NIL_VAL;
    }

    // Copy non-tombstone entries to new array.
    table->count = 0;
    for (int i = 0; i < table->capacity; i++)
    {
        Entry* entry = table->entries + i;
        if (entry->key == NULL) continue;

        Entry* dest = findEntry(entries, capacity, entry->key);
        dest->key = entry->key;
        dest->value = entry->value;
        table->count++;
    }

    // Free old array and update table fields.
    FREE_ARRAY(Entry, table->entries, table->capacity);
    table->entries = entries;
    table->capacity = capacity;
}

/**
 * @brief Add entry to table.
 * @return True if a new entry was added
 * (the key was not in the table).
 * @return False if an entry was overwritten
 * (the key was in the table).
 */
bool tableSet(Table* table, ObjString* key, Value value)
{
    if (table->count == table->capacity * TABLE_MAX_LOAD)
    {
        int capacity = GROW_CAPACITY(table->capacity);
        adjustCapacity(table, capacity);
    }

    Entry* entry = findEntry(table->entries, table->capacity, key);
    bool isNewKey = entry->key == NULL;

    // Increment count only if inserting into a non-tombstone
    // empty position (only if not replacing a tombstone).
    // This ensures that count always equals
    // the number of entries plus the number of tombstones.
    if (isNewKey && IS_NIL(entry->value)) table->count++;

    entry->key = key;
    entry->value = value;
    return isNewKey;
}

/**
 * @brief Add all entries from one table to another.
 */
void tableAddAll(Table* from, Table* to)
{
    for (int i = 0; i < from->capacity; i++)
    {
        Entry* entry = from->entries + i;
        if (entry->key != NULL)
        {
            tableSet(to, entry->key, entry->value);
        }
    }
}

/**
 * @brief Find a key equal to a given buffer.
 * @param table The table to search.
 * @param chars The buffer.
 * @param length The length of the buffer.
 * @param hash The hash of the buffer.
 */
ObjString* tableFindString(Table* table, const char* chars, int length, uint32_t hash)
{
    if (table->count == 0) return NULL;

    uint32_t index = hash % table->capacity;

    while (true)
    {
        while (index < table->capacity)
        {
            Entry* entry = table->entries + index;
            if (entry->key == NULL)
            {
                // Stop if we find an empty non-tombstone entry.
                if (IS_NIL(entry->value)) return NULL;
            }
            else if (entry->key->length == length && entry->key->hash == hash
                && memcmp(entry->key->chars, chars, length) == 0)
            {
                // We found the string.
                return entry->key;
            }

            index = index+1;
        }

        index = 0;
    }
}
