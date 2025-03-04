import java.util.*;

// Edge class represents an edge in the graph with endpoints u and v, and a connection cost
class Edge implements Comparable<Edge> {
    int u, v, cost;
    Edge(int u, int v, int cost) {
        this.u = u;
        this.v = v;
        this.cost = cost;
    }
    @Override
    public int compareTo(Edge other) {
        return Integer.compare(this.cost, other.cost);
    }
}

// DSU (Disjoint Set Union) class for efficient union-find operations in Kruskal's algorithm
class DSU {
    int[] parent;
    DSU(int n) {
        parent = new int[n + 1]; // +1 for virtual node 0
        for (int i = 0; i <= n; i++) {
            parent[i] = i;
        }
    }
    int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]); // Path compression
        }
        return parent[x];
    }
    boolean union(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false;
        parent[py] = px;
        return true;
    }
}

public class MinConnectionCost {
    public static int minCostToConnect(int n, int[] modules, int[][] connections) {
        List<Edge> edges = new ArrayList<>();

        // Add edges from the virtual node (0) to each device (1-indexed) with cost equal to module installation cost
        for (int i = 0; i < n; i++) {
            edges.add(new Edge(0, i + 1, modules[i]));
        }

        // Add all bidirectional connections (device indices are assumed 1-indexed)
        for (int[] conn : connections) {
            int u = conn[0], v = conn[1], cost = conn[2];
            edges.add(new Edge(u, v, cost));
        }

        // Sort edges by cost (Kruskal's algorithm)
        Collections.sort(edges);

        DSU dsu = new DSU(n);
        int totalCost = 0, edgesAdded = 0;

        // Iterate over edges and add them if they connect disjoint components
        for (Edge e : edges) {
            if (dsu.union(e.u, e.v)) {
                totalCost += e.cost;
                edgesAdded++;
                // MST for n+1 nodes requires n edges; stop early if MST is complete
                if (edgesAdded == n) break;
            }
        }
        return totalCost;
    }

    public static void main(String[] args) {
        // Example-1
        int n1 = 3;
        int[] modules1 = {1, 2, 2};
        int[][] connections1 = {{1, 2, 1}, {2, 3, 1}};
        int result1 = minCostToConnect(n1, modules1, connections1);
        System.out.println("The minimum total cost to connect all devices: " + result1); // Expected Output: 3

        // Example-2
        int n2 = 4;
        int[] modules2 = {2, 3, 1, 4};
        int[][] connections2 = {{1, 2, 1}, {1, 3, 3}, {2, 4, 2}, {3, 4, 4}};
        int result2 = minCostToConnect(n2, modules2, connections2);
        System.out.println("The minimum total cost to connect all devices: " + result2); // Expected Output: 6

        // Example-3
        int n3 = 1;
        int[] modules3 = {5};
        int[][] connections3 = {};  // No connections
        int result3 = minCostToConnect(n3, modules3, connections3);
        System.out.println("The minimum total cost to connect all devices: " + result3); //Expected Output: 5

    }
}

