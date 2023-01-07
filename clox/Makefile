# Compiler and linker flags.
CC        := gcc
SRC_DIR   := src
BIN_DIR   := bin
BIN_EXE   := $(BIN_DIR)/main
OUTPUT    := $(if $(findstring Windows_NT,$(OS)),$(BIN_EXE).exe,$(BIN_EXE).out)
OBJ_DIR   := $(BIN_DIR)/obj
INC_DIRS  := -I$(SRC_DIR)
LIB_DIRS  :=
SRC_FILES := $(wildcard $(SRC_DIR)/*.c)
H_FILES   := $(wildcard $(SRC_DIR)/*.h)
OBJ_FILES := $(patsubst $(SRC_DIR)/%.c,$(OBJ_DIR)/%.o,$(SRC_FILES))
C_FLAGS   := -O2 -Wall -Wextra
LD_FLAGS  := 
MAKEFLAGS += -j8

# Compile the object files and place them in their own directory.
$(OBJ_DIR)/%.o: $(SRC_DIR)/%.c $(H_FILES) | $(OBJ_DIR)
	$(CC) $(C_FLAGS) $(INC_DIRS) -c -o $@ $<

# Link the object files together to create the final executable.
$(OUTPUT): $(OBJ_FILES) Makefile
	$(CC) $(LIB_DIRS) $(LD_FLAGS) $(OBJ_FILES) -o $(OUTPUT)

# Create directories when needed.
$(OBJ_DIR): | $(BIN_DIR)
	mkdir $(OBJ_DIR) 

$(BIN_DIR):
	mkdir $(BIN_DIR)

# When typing 'make', compile and link the executable.
all: $(OUTPUT)

# When typing 'make run', build and run the executable.
run: $(OUTPUT)
	./$(OUTPUT)

# When typing 'make clean', clean up object files and executable.
clean:
	rm $(OBJ_DIR)/*.o
	rm $(OUTPUT)
