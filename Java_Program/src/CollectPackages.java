import java.util.*;

public class CollectPackages {
    static int n;                   // number of nodes
    static List<Integer>[] adj;     // adjacency list
    static int[] packages;          // 0 or 1 per node
    static int fullMask;            // bitmask of all packages
    static int[] collectMask;       // collectMask[node] = bitmask of packages collectible from node

    public static int minRoadsToCollectAll(int[] packages, int[][] roads) {
        n = packages.length;
        CollectPackages.packages = packages;

        // Build adjacency
        adj = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            adj[i] = new ArrayList<>();
        }
        for (int[] e : roads) {
            adj[e[0]].add(e[1]);
            adj[e[1]].add(e[0]);
        }

        // Identify which nodes have packages and set up bitmasks
        List<Integer> packageNodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (packages[i] == 1) packageNodes.add(i);
        }
        int p = packageNodes.size();   // number of package-nodes
        if (p == 0) return 0;          // no packages, cost is 0
        fullMask = (1 << p) - 1;

        // Precompute collectMask for each node (which packages can be collected from distance ≤ 2)
        collectMask = new int[n];
        for (int i = 0; i < n; i++) {
            // BFS up to distance 2 from node i
            int mask = 0;
            Queue<int[]> q = new LinkedList<>();
            boolean[] visited = new boolean[n];
            q.offer(new int[]{i, 0}); // (node, dist)
            visited[i] = true;
            while (!q.isEmpty()) {
                int[] cur = q.poll();
                int nd = cur[0], dist = cur[1];
                // If nd is a package-node, set the bit
                if (packages[nd] == 1) {
                    int idx = packageNodes.indexOf(nd);
                    if (idx != -1) {
                        mask |= (1 << idx);
                    }
                }
                if (dist < 2) {
                    for (int nxt : adj[nd]) {
                        if (!visited[nxt]) {
                            visited[nxt] = true;
                            q.offer(new int[]{nxt, dist+1});
                        }
                    }
                }
            }
            collectMask[i] = mask;
        }

        int ans = Integer.MAX_VALUE;
        // Try BFS from each possible start node
        for (int start = 0; start < n; start++) {
            ans = Math.min(ans, bfsFromStart(start, packageNodes));
        }
        return ans == Integer.MAX_VALUE ? -1 : ans;
    }

    // BFS in state-space: (node, bitmask)
    private static int bfsFromStart(int start, List<Integer> packageNodes) {
        // Dist map: dist[node][mask]
        Map<Long, Integer> dist = new HashMap<>(); // or use array if smaller constraints
        Queue<long[]> q = new LinkedList<>();      // each element is (node, mask, cost)

        // Initial state
        int initMask = collectMask[start];
        long initState = encode(start, initMask);
        dist.put(initState, 0);
        q.offer(new long[]{start, initMask});

        while (!q.isEmpty()) {
            long[] cur = q.poll();
            int node = (int) cur[0];
            int mask = (int) cur[1];
            int costSoFar = dist.get(encode(node, mask));

            // Collect from current node's radius-2
            int newMask = mask | collectMask[node];
            // If we have all packages and node == start => candidate answer
            if (newMask == fullMask && node == start) {
                return costSoFar;
            }

            // Move to neighbors
            for (int nxt : adj[node]) {
                int nxtMask = newMask; // we also collect from next node
                // but let's unify it by collecting only from standing positions
                // so we do not forcibly collect from edges
                long nxtState = encode(nxt, nxtMask);
                if (!dist.containsKey(nxtState)) {
                    dist.put(nxtState, costSoFar + 1);
                    q.offer(new long[]{nxt, nxtMask});
                }
            }
        }
        return Integer.MAX_VALUE; // cannot collect all returning to start
    }

    private static long encode(int node, int mask) {
        // Combine node + mask into a single long
        // node in lower bits, mask in upper bits or vice versa
        // but for safety just combine carefully
        return (((long) mask) << 20) | (node & 0xFFFFF);
    }

    // For testing
    public static void main(String[] args) {
        // Example - 1
        int[] packages1 = {1,0,0,0,0,1};
        int[][] roads1 = {{0,1},{1,2},{2,3},{3,4},{4,5}};
        int result1 = minRoadsToCollectAll(packages1, roads1);
        System.out.println("The minimum number of roads needed to traverse: " + result1); // Expect 2

        // Example - 2
        int[] packages2 = {0,0,0,1,1,0,0,1};
        int[][] roads2 = {{0,1},{0,2},{1,3},{1,4},{2,5},{5,6},{5,7}};
        int result2 = minRoadsToCollectAll(packages2, roads2);
        System.out.println("The minimum number of roads needed to traverse: " + result2); // Expect 2


        // Example - 3
        int[] packages3 = {1,0,0,1,0,0};
        int[][] roads3 = {{0,1},{1,2},{2,3},{3,4},{4,5}};
        int result3 = minRoadsToCollectAll(packages3, roads3);
        System.out.println("The minimum number of roads needed to traverse: " + result3); // Expect 0
    }
}

