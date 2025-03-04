// Binary Search Tree (BST) Node Class
class BSTNode {
    int i, j;                   // Indices from returns1 and returns2
    int product;                // The product returns1[i] * returns2[j]
    BSTNode left, right;        // Left and right children


    BSTNode(int i, int j, int product) {
        this.i = i;
        this.j = j;
        this.product = product;
        this.left = this.right = null;
    }
}


// Binary Search Tree Class
class BST {
    BSTNode root;

    public BST() {
        root = null;
    }

    // Insert a new node into the BST
    public void insert(BSTNode node) {
        root = insertRec(root, node);
    }

    public BSTNode insertRec(BSTNode root, BSTNode node) {
        if (root == null) {
            root = node;
            return root;
        }

        if (node.product < root.product) {
            root.left = insertRec(root.left, node);
        }
        else {
            root.right = insertRec(root.right, node);
        }
        return root;
    }

    // In-order traversal to find the k-th smallest product
    public int kthSmallest(int k) {
        int[] result = new int[1];
        int[] count = new int[1]; // To keep track of how many nodes we've visited
        inOrderTraversal(root, k, count, result);
        return result[0];
    }

    // Recursive in-order traversal
    private void inOrderTraversal(BSTNode node, int k, int[] count, int[] result) {
        if (node == null) return;

        // Traverse the left subtree
        inOrderTraversal(node.left, k, count, result);

        // Visit the current node
        count[0]++;
        if (count[0] == k) {
            result[0] = node.product;
            return;
        }

        // Traverse the right subtree
        inOrderTraversal(node.right, k, count, result);
    }
}


public class KthSmallestInvestmentProduct {
    public static int findKthSmallestProduct(int[] returns1, int[] returns2, int k) {
        BST bst = new BST();
        // Insert all possible combinations of products into the BST
        for (int i = 0; i < returns1.length; i++) {
            for (int j = 0; j < returns2.length; j++) {
                int product = returns1[i] * returns2[j];
                bst.insert(new BSTNode(i, j, product));
            }
        }
        // Use in-order traversal to find the k-th smallest product
        return bst.kthSmallest(k);
    }

    public static void main(String[] args) {
        // Example 1
        int[] returns1 = {2, 5};
        int[] returns2 = {3, 4};
        int k = 2;
        System.out.println("The 2nd smallest product is: " + findKthSmallestProduct(returns1, returns2, k)); // Output: 8

        // Example 2
        int[] returns1_2 = {-4, -2, 0, 3};
        int[] returns2_2 = {2, 4};
        int k_2 = 6;
        System.out.println("The 6th smallest product is: " + findKthSmallestProduct(returns1_2, returns2_2, k_2)); // Output: 0

        // Example 3
        int[] returns1_3 = {1, 2, 3, 4};
        int[] returns2_3 = {-1, 0, 1, 2, 3};
        int k_3 = 3;
        System.out.println("The 3th smallest product is: " + findKthSmallestProduct(returns1_3, returns2_3, k_3)); // Output: -2

        // Example 3
        int[] returns1_4 = {1000, 200, 3000, 400, 2500};
        int[] returns2_4 = {1000, 100, 3500, 2000, 500};
        int k_4 = 5;
        System.out.println("The 5th smallest product is: " + findKthSmallestProduct(returns1_4, returns2_4, k_4));
    }
}



