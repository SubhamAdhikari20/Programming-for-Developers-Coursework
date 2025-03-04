import java.util.Arrays;

public class LexicographicalClosestPair {
    // Method to find the lexicographically smallest pair with the minimum distance
    public static int[] findClosestPair(int[] x_coords, int[] y_coords) {
        int n = x_coords.length;

        // Initialize minDistance to a large value and bestPair as invalid indices
        int minDistance = Integer.MAX_VALUE;
        int bestI = -1, bestJ = -1;

        // Iterate over all pairs (i, j) with i < j
        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                // Calculate distance between points i and j
                int distance = Math.abs(x_coords[i] - x_coords[j]) + Math.abs(y_coords[i] - y_coords[j]);

                // If we find a smaller distance, update minDistance and best pair
                if (distance < minDistance) {
                    minDistance = distance;
                    bestI = i;
                    bestJ = j;
                }
                // If the same distance is found, check lexicographical order:
                // (i, j) is lexicographically smaller if i < bestI or (i == bestI and j < bestJ)
                else if (distance == minDistance) {
                    if (i < bestI || (i == bestI && j < bestJ)) {
                        bestI = i;
                        bestJ = j;
                    }
                }
            }
        }

        return new int[] {bestI, bestJ};
    }

    public static void main(String[] args) {
        // Example 1:
        int[] x_coords1 = {1, 2, 3, 2, 4};
        int[] y_coords1 = {2, 3, 1, 2, 3};
        int[] result1 = findClosestPair(x_coords1, y_coords1);
        System.out.println("The closest pair is: " + Arrays.toString(result1)); // Expected output: [0, 3]

        // Example 2:
        int[] x_coords2 = {1, 5, 3, 9, 4};
        int[] y_coords2 = {8, 3, 1, 6, 5};
        int[] result2 = findClosestPair(x_coords2, y_coords2);
        System.out.println("The closest pair is: " + Arrays.toString(result2)); // Expected output: [1, 4]

    }
}
