CC ?= gcc
CFLAGS ?= -std=c11 -Wall -Wextra -Wpedantic -O2
BIN_DIR := build

.PHONY: all test clean java

all: $(BIN_DIR)/page_replacement $(BIN_DIR)/partition_allocation java

$(BIN_DIR):
	mkdir -p $(BIN_DIR)

$(BIN_DIR)/page_replacement: c/page_replacement.c | $(BIN_DIR)
	$(CC) $(CFLAGS) $< -o $@

$(BIN_DIR)/partition_allocation: c/partition_allocation.c | $(BIN_DIR)
	$(CC) $(CFLAGS) $< -o $@

java:
	mkdir -p $(BIN_DIR)/java
	javac -encoding UTF-8 -d $(BIN_DIR)/java java/src/*.java

test: all
	./$(BIN_DIR)/page_replacement --self-test
	./$(BIN_DIR)/partition_allocation --self-test
	java -cp $(BIN_DIR)/java BankerSafetyTest

clean:
	rm -rf $(BIN_DIR)
