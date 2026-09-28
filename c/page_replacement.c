#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int find_page(const int *frames, int frame_count, int page) {
    for (int i = 0; i < frame_count; ++i) {
        if (frames[i] == page) return i;
    }
    return -1;
}

int fifo_faults(const int *refs, int ref_count, int frame_count) {
    int *frames = malloc((size_t)frame_count * sizeof(int));
    if (!frames) return -1;
    for (int i = 0; i < frame_count; ++i) frames[i] = -1;
    int faults = 0;
    int next = 0;
    for (int i = 0; i < ref_count; ++i) {
        if (find_page(frames, frame_count, refs[i]) >= 0) continue;
        frames[next] = refs[i];
        next = (next + 1) % frame_count;
        ++faults;
    }
    free(frames);
    return faults;
}

int lru_faults(const int *refs, int ref_count, int frame_count) {
    int *frames = malloc((size_t)frame_count * sizeof(int));
    int *last_used = malloc((size_t)frame_count * sizeof(int));
    if (!frames || !last_used) {
        free(frames);
        free(last_used);
        return -1;
    }
    for (int i = 0; i < frame_count; ++i) {
        frames[i] = -1;
        last_used[i] = -1;
    }
    int faults = 0;
    for (int i = 0; i < ref_count; ++i) {
        int slot = find_page(frames, frame_count, refs[i]);
        if (slot < 0) {
            slot = 0;
            for (int j = 1; j < frame_count; ++j) {
                if (last_used[j] < last_used[slot]) slot = j;
            }
            frames[slot] = refs[i];
            ++faults;
        }
        last_used[slot] = i;
    }
    free(frames);
    free(last_used);
    return faults;
}

int opt_faults(const int *refs, int ref_count, int frame_count) {
    int *frames = malloc((size_t)frame_count * sizeof(int));
    if (!frames) return -1;
    for (int i = 0; i < frame_count; ++i) frames[i] = -1;
    int faults = 0;
    for (int i = 0; i < ref_count; ++i) {
        if (find_page(frames, frame_count, refs[i]) >= 0) continue;
        int replace = -1;
        int farthest = -1;
        for (int j = 0; j < frame_count; ++j) {
            if (frames[j] < 0) {
                replace = j;
                break;
            }
            int next_use = ref_count + 1;
            for (int k = i + 1; k < ref_count; ++k) {
                if (refs[k] == frames[j]) {
                    next_use = k;
                    break;
                }
            }
            if (next_use > farthest) {
                farthest = next_use;
                replace = j;
            }
        }
        frames[replace] = refs[i];
        ++faults;
    }
    free(frames);
    return faults;
}

static void print_result(const char *name, int faults, int count) {
    double hit_ratio = count == 0 ? 0.0 : 1.0 - (double)faults / count;
    printf("%-4s faults: %d, hit ratio: %.2f%%\n", name, faults, hit_ratio * 100.0);
}

static int self_test(void) {
    const int refs[] = {7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2};
    const int count = (int)(sizeof(refs) / sizeof(refs[0]));
    int ok = fifo_faults(refs, count, 3) == 10
             && lru_faults(refs, count, 3) == 9
             && opt_faults(refs, count, 3) == 7;
    printf("page replacement self-test: %s\n", ok ? "PASS" : "FAIL");
    return ok ? 0 : 1;
}

int main(int argc, char **argv) {
    if (argc > 1 && strcmp(argv[1], "--self-test") == 0) return self_test();
    const int refs[] = {7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2};
    const int count = (int)(sizeof(refs) / sizeof(refs[0]));
    int frames = argc > 1 ? atoi(argv[1]) : 3;
    if (frames <= 0 || frames > 64) {
        fprintf(stderr, "frame count must be between 1 and 64\n");
        return 2;
    }
    printf("Reference string length: %d, frames: %d\n", count, frames);
    print_result("FIFO", fifo_faults(refs, count, frames), count);
    print_result("LRU", lru_faults(refs, count, frames), count);
    print_result("OPT", opt_faults(refs, count, frames), count);
    return 0;
}
