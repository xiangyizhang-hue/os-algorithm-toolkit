import java.util.List;

public final class BankerSafetyTest {
    public static void main(String[] args) {
        int[] available = {3, 3, 2};
        int[][] allocation = {{0, 1, 0}, {2, 0, 0}, {3, 0, 2}, {2, 1, 1}, {0, 0, 2}};
        int[][] max = {{7, 5, 3}, {3, 2, 2}, {9, 0, 2}, {2, 2, 2}, {4, 3, 3}};
        int[][] need = new int[max.length][max[0].length];
        for (int i = 0; i < max.length; i++) {
            for (int j = 0; j < max[i].length; j++) need[i][j] = max[i][j] - allocation[i][j];
        }
        List<Integer> sequence = BankerSafety.safeSequence(available, allocation, need);
        require(sequence.size() == 5, "classic state should be safe");
        require(BankerSafety.requestIsSafe(1, new int[]{1, 0, 2}, available, allocation, need),
                "P1 request should be safe");
        require(!BankerSafety.requestIsSafe(4, new int[]{3, 3, 0}, available, allocation, need),
                "P4 request should be unsafe");
        System.out.println("banker safety self-test: PASS, sequence=" + sequence);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
