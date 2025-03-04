import java.util.*;

public class EmployeeRewards {
    public static int calculateMinRewards(int[] ratings) {
        // Validation
        if (ratings == null || ratings.length == 0) {
            return 0;
        }
        else if (ratings.length == 1) {
            return 1;
        }

        int n = ratings.length;
        int[] rewards = new int[n];
        // Initialize rewards array with 1 for each employee
        // Fills the value in array. It is efficient than using loops
        Arrays.fill(rewards, 1);

        /*
        Left-to-Right Pass:
        If current employee has higher rating than previous,
        give current employee one more reward than the previous.
        */
        for (int i = 1; i < n; i++) {
            if (ratings[i] > ratings[i - 1]) {
                rewards[i] = rewards[i - 1] + 1;
            }
        }

        /*
         Right-to-Left Pass:
         If current employee has higher rating than next,
         ensure the current employee gets more rewards than the next one.
         */
        for (int i = n - 2; i >= 0; i--) {
            if (ratings[i] > ratings[i + 1]) {
                // Math.max -> preserve any higher values from Left-to-Right pass
                rewards[i] = Math.max(rewards[i], rewards[i + 1] + 1);
            }
        }

        // Sum up all the rewards to get the total minimum rewards needed.
        int totalRewards = 0;
        for (int reward : rewards) {
            totalRewards += reward;
        }

        return totalRewards;
    }

    public static void main(String[] args) {
        // Example 1
        int[] ratings1 = {1, 0, 2};
        System.out.println("Minimum rewards of ratings1 = " + calculateMinRewards(ratings1)); // Expected output: 5

        // Example 2
        int[] ratings2 = {1, 2, 2};
        System.out.println("Minimum rewards of ratings2 = " + calculateMinRewards(ratings2)); // Expected output: 4

        // Example 3
        int[] ratings3 = {2, 4, 0, 5, 7};
        System.out.println("Minimum rewards of ratings3 = " + calculateMinRewards(ratings3)); // Expected output: 9
    }
}
