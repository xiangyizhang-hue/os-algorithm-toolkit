import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Testable core for the Banker's safety and trial-allocation algorithm. */
public final class BankerSafety {
    private BankerSafety() {}

    public static List<Integer> safeSequence(int[] available, int[][] allocation, int[][] need) {
        validateDimensions(available, allocation, need);
        int[] work = available.clone();
        boolean[] finished = new boolean[allocation.length];
        List<Integer> sequence = new ArrayList<>();
        boolean progressed;
        do {
            progressed = false;
            for (int process = 0; process < allocation.length; process++) {
                if (!finished[process] && canFinish(need[process], work)) {
                    for (int resource = 0; resource < work.length; resource++) {
                        work[resource] += allocation[process][resource];
                    }
                    finished[process] = true;
                    sequence.add(process);
                    progressed = true;
                }
            }
        } while (progressed);
        return sequence.size() == allocation.length ? sequence : Collections.emptyList();
    }

    public static boolean requestIsSafe(int process, int[] request, int[] available,
                                        int[][] allocation, int[][] need) {
        validateDimensions(available, allocation, need);
        if (process < 0 || process >= allocation.length || request.length != available.length) {
            throw new IllegalArgumentException("invalid process or request size");
        }
        int[] trialAvailable = available.clone();
        int[][] trialAllocation = copy(allocation);
        int[][] trialNeed = copy(need);
        for (int resource = 0; resource < request.length; resource++) {
            if (request[resource] < 0 || request[resource] > trialNeed[process][resource]
                    || request[resource] > trialAvailable[resource]) return false;
            trialAvailable[resource] -= request[resource];
            trialAllocation[process][resource] += request[resource];
            trialNeed[process][resource] -= request[resource];
        }
        return !safeSequence(trialAvailable, trialAllocation, trialNeed).isEmpty();
    }

    private static boolean canFinish(int[] need, int[] work) {
        for (int i = 0; i < need.length; i++) if (need[i] > work[i]) return false;
        return true;
    }

    private static int[][] copy(int[][] source) {
        return Arrays.stream(source).map(int[]::clone).toArray(int[][]::new);
    }

    private static void validateDimensions(int[] available, int[][] allocation, int[][] need) {
        if (allocation.length == 0 || allocation.length != need.length) {
            throw new IllegalArgumentException("allocation and need must have the same processes");
        }
        for (int process = 0; process < allocation.length; process++) {
            if (allocation[process].length != available.length || need[process].length != available.length) {
                throw new IllegalArgumentException("resource dimensions do not match");
            }
        }
    }
}
