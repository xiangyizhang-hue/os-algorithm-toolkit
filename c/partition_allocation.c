#include <stdio.h>
#include <string.h>

#define MEMORY_SIZE 1024
#define MAX_BLOCKS 256

typedef enum { FIRST_FIT, BEST_FIT, WORST_FIT } Strategy;
typedef struct { int address; int size; int job_id; } Block;
typedef struct { Block blocks[MAX_BLOCKS]; int count; } MemoryManager;

void memory_init(MemoryManager *manager) {
    manager->blocks[0] = (Block){0, MEMORY_SIZE, 0};
    manager->count = 1;
}

static int choose_block(const MemoryManager *manager, int size, Strategy strategy) {
    int choice = -1;
    for (int i = 0; i < manager->count; ++i) {
        const Block *block = &manager->blocks[i];
        if (block->job_id != 0 || block->size < size) continue;
        if (strategy == FIRST_FIT) return i;
        if (choice < 0
                || (strategy == BEST_FIT && block->size < manager->blocks[choice].size)
                || (strategy == WORST_FIT && block->size > manager->blocks[choice].size)) {
            choice = i;
        }
    }
    return choice;
}

int memory_allocate(MemoryManager *manager, int job_id, int size, Strategy strategy) {
    if (job_id <= 0 || size <= 0 || manager->count >= MAX_BLOCKS) return 0;
    for (int i = 0; i < manager->count; ++i) {
        if (manager->blocks[i].job_id == job_id) return 0;
    }
    int index = choose_block(manager, size, strategy);
    if (index < 0) return 0;
    Block original = manager->blocks[index];
    manager->blocks[index].size = size;
    manager->blocks[index].job_id = job_id;
    if (original.size > size) {
        for (int i = manager->count; i > index + 1; --i) manager->blocks[i] = manager->blocks[i - 1];
        manager->blocks[index + 1] = (Block){original.address + size, original.size - size, 0};
        ++manager->count;
    }
    return 1;
}

static void remove_block(MemoryManager *manager, int index) {
    for (int i = index; i < manager->count - 1; ++i) manager->blocks[i] = manager->blocks[i + 1];
    --manager->count;
}

int memory_free(MemoryManager *manager, int job_id) {
    int index = -1;
    for (int i = 0; i < manager->count; ++i) {
        if (manager->blocks[i].job_id == job_id) { index = i; break; }
    }
    if (index < 0) return 0;
    manager->blocks[index].job_id = 0;
    if (index > 0 && manager->blocks[index - 1].job_id == 0) {
        manager->blocks[index - 1].size += manager->blocks[index].size;
        remove_block(manager, index--);
    }
    if (index + 1 < manager->count && manager->blocks[index + 1].job_id == 0) {
        manager->blocks[index].size += manager->blocks[index + 1].size;
        remove_block(manager, index + 1);
    }
    return 1;
}

int memory_is_valid(const MemoryManager *manager) {
    int next_address = 0;
    for (int i = 0; i < manager->count; ++i) {
        const Block *block = &manager->blocks[i];
        if (block->address != next_address || block->size <= 0) return 0;
        if (i > 0 && block->job_id == 0 && manager->blocks[i - 1].job_id == 0) return 0;
        next_address += block->size;
    }
    return next_address == MEMORY_SIZE;
}

void memory_print(const MemoryManager *manager) {
    puts("address  size  state");
    for (int i = 0; i < manager->count; ++i) {
        const Block *block = &manager->blocks[i];
        if (block->job_id == 0) printf("%7d %5d  free\n", block->address, block->size);
        else printf("%7d %5d  job-%d\n", block->address, block->size, block->job_id);
    }
}

static int self_test(void) {
    Strategy strategies[] = {FIRST_FIT, BEST_FIT, WORST_FIT};
    for (int s = 0; s < 3; ++s) {
        MemoryManager manager;
        memory_init(&manager);
        if (!memory_allocate(&manager, 1, 100, strategies[s])) return 1;
        if (!memory_allocate(&manager, 2, 200, strategies[s])) return 1;
        if (!memory_allocate(&manager, 3, 50, strategies[s])) return 1;
        if (!memory_free(&manager, 2)) return 1;
        if (!memory_allocate(&manager, 4, 150, strategies[s])) return 1;
        if (!memory_is_valid(&manager)) return 1;
        if (!memory_free(&manager, 1) || !memory_free(&manager, 3) || !memory_free(&manager, 4)) return 1;
        if (manager.count != 1 || manager.blocks[0].size != MEMORY_SIZE) return 1;
    }
    puts("partition allocation self-test: PASS");
    return 0;
}

int main(int argc, char **argv) {
    if (argc > 1 && strcmp(argv[1], "--self-test") == 0) return self_test();
    Strategy strategy = FIRST_FIT;
    if (argc > 1 && strcmp(argv[1], "best") == 0) strategy = BEST_FIT;
    if (argc > 1 && strcmp(argv[1], "worst") == 0) strategy = WORST_FIT;
    MemoryManager manager;
    memory_init(&manager);
    memory_allocate(&manager, 1, 120, strategy);
    memory_allocate(&manager, 2, 240, strategy);
    memory_allocate(&manager, 3, 80, strategy);
    memory_free(&manager, 2);
    memory_allocate(&manager, 4, 160, strategy);
    memory_print(&manager);
    return memory_is_valid(&manager) ? 0 : 1;
}
