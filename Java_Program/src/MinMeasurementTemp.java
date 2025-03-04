import java.util.*;

public class MinMeasurementTemp {

    public static int minMeasurement(int k, int n){
        int[][] dp = new int[k+1][n+1];
        // dp[k][n] represents the minimum measurements
        // k -> samples materials
        // n -> temperature levels
        // To find minimum measurement/attempts to find critical temperature for the materials to change their properties.

        for (int i = 1; i <= k ; i++) {
            for (int j = 1; j <= n; j++) {
                if (i == 1) {   // if k (sample material) is 1, the attempts is n
                    dp[i][j] = j;
                }
                else if(j == 1){    // if n (temperature level) is 1, the attempts is 1
                    dp[i][j] = 1;
                }
                else{
                    int min = Integer.MAX_VALUE;
                    for (int cj = j-1, pj = 0; cj >=0 ; cj--, pj++) { // cj -> current row      // pj -> previous row
                        int value1 = dp[i][cj];     // does not change
                        int value2 = dp[i-1][pj];   // changes
                        int val = Math.max(value1, value2);
                        min = Math.min(val, min);
                    }
                    dp[i][j] = min + 1;
                }
            }
        }
        return dp[k][n];
    }

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        // k -> samples material and n -> temperature levels
        System.out.print("Enter the no. of material samples: ");
        int k = scan.nextInt();
        System.out.print("Enter the no. of temperature levels: ");
        int n = scan.nextInt();

        System.out.println("The minimum measurement/attempts to find critical temperature is " + minMeasurement(k, n));

    }
}


