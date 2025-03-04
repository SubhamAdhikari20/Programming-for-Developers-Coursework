import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class NetworkOptimizer extends JFrame {

    private GraphPanel graphPanel;
    private ControlPanel controlPanel;

    // Graph Data Structures
    private List<Node> nodes;
    private List<Edge> edges;

    public NetworkOptimizer() {
        setTitle("Network Optimizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Initialize nodes and edges (sample data)
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
        initializeGraph();

        // Create GUI panels
        graphPanel = new GraphPanel();
        controlPanel = new ControlPanel();

        setLayout(new BorderLayout());
        add(graphPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);
    }

    // Sample data initialization
    private void initializeGraph() {
        // Create sample nodes with names and (x,y) positions
        nodes.add(new Node("Server A", 150, 100));
        nodes.add(new Node("Client B", 400, 150));
        nodes.add(new Node("Client C", 250, 300));
        nodes.add(new Node("Server D", 550, 350));
        nodes.add(new Node("Client E", 700, 200));

        // Create sample edges with cost and bandwidth values
        edges.add(new Edge(nodes.get(0), nodes.get(1), 10, 50));
        edges.add(new Edge(nodes.get(0), nodes.get(2), 15, 40));
        edges.add(new Edge(nodes.get(1), nodes.get(2), 5, 80));
        edges.add(new Edge(nodes.get(1), nodes.get(3), 12, 60));
        edges.add(new Edge(nodes.get(2), nodes.get(3), 8, 70));
        edges.add(new Edge(nodes.get(3), nodes.get(4), 20, 30));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new NetworkOptimizer().setVisible(true);
        });
    }

    // ---------------------
    // Node Class
    // ---------------------
    class Node {
        String name;
        int x, y;
        public Node(String name, int x, int y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

    // ---------------------
    // Edge Class
    // ---------------------
    class Edge {
        Node source, target;
        double cost;
        double bandwidth;
        boolean optimized = false;
        boolean shortestPath = false;
        public Edge(Node source, Node target, double cost, double bandwidth) {
            this.source = source;
            this.target = target;
            this.cost = cost;
            this.bandwidth = bandwidth;
        }
    }

    // ---------------------
    // GraphPanel: Visualize the network graph with interactivity
    // ---------------------
    class GraphPanel extends JPanel {
        private final Font nodeFont = new Font("Segoe UI", Font.BOLD, 14);
        private Node draggedNode = null;
        private int offsetX, offsetY;

        public GraphPanel() {
            setBackground(new Color(30, 30, 30));
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    for (Node node : nodes) {
                        int size = 30;
                        int nodeX = node.x - size / 2;
                        int nodeY = node.y - size / 2;
                        Rectangle rect = new Rectangle(nodeX, nodeY, size, size);
                        if (rect.contains(e.getPoint())) {
                            draggedNode = node;
                            offsetX = e.getX() - node.x;
                            offsetY = e.getY() - node.y;
                            break;
                        }
                    }
                }
                public void mouseReleased(MouseEvent e) {
                    draggedNode = null;
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (draggedNode != null) {
                        draggedNode.x = e.getX() - offsetX;
                        draggedNode.y = e.getY() - offsetY;
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw edges
            for (Edge edge : edges) {
                int x1 = edge.source.x;
                int y1 = edge.source.y;
                int x2 = edge.target.x;
                int y2 = edge.target.y;

                // Highlight shortest path edges in red, then optimized edges in green, else default gradient
                if (edge.shortestPath) {
                    g2.setColor(Color.RED);
                    g2.setStroke(new BasicStroke(4));
                } else if (edge.optimized) {
                    g2.setColor(Color.GREEN);
                    g2.setStroke(new BasicStroke(4));
                } else {
                    GradientPaint gp = new GradientPaint(x1, y1, Color.LIGHT_GRAY, x2, y2, Color.DARK_GRAY);
                    g2.setPaint(gp);
                    g2.setStroke(new BasicStroke(3));
                }
                g2.drawLine(x1, y1, x2, y2);

                // Display edge information at midpoint
                String label = String.format("C=%.1f, BW=%.1f", edge.cost, edge.bandwidth);
                int midX = (x1 + x2) / 2;
                int midY = (y1 + y2) / 2;
                g2.setFont(new Font("Arial", Font.PLAIN, 12));
                g2.setColor(Color.WHITE);
                g2.drawString(label, midX, midY);
            }

            // Draw nodes
            for (Node node : nodes) {
                int size = 30;
                int x = node.x - size / 2;
                int y = node.y - size / 2;

                // Draw drop shadow
                g2.setColor(new Color(0, 0, 0, 100));
                g2.fillOval(x + 3, y + 3, size, size);

                // Create a radial gradient for the node
                float[] dist = {0.0f, 1.0f};
                Color[] colors = {new Color(100, 200, 100), new Color(50, 150, 50)};
                RadialGradientPaint rgp = new RadialGradientPaint(
                        new Point(x + size / 2, y + size / 2), size / 2, dist, colors);
                g2.setPaint(rgp);
                g2.fillOval(x, y, size, size);

                // Draw node border
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(x, y, size, size);

                // Draw node label
                g2.setFont(nodeFont);
                g2.drawString(node.name, node.x - size / 2, y - 5);
            }
        }
    }

    // ---------------------
    // ControlPanel: User Controls & Real-Time Analysis
    // ---------------------
    class ControlPanel extends JPanel {
        private JTextField alphaField, betaField;
        private JLabel costLabel, latencyLabel;
        private JButton optimizeBtn, shortestPathBtn, addNodeBtn, addEdgeBtn;

        public ControlPanel() {
            setBackground(new Color(40, 40, 40));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setPreferredSize(new Dimension(300, 800));

            // Weighting factors for cost and latency
            alphaField = new JTextField("1.0", 5);
            betaField = new JTextField("1.0", 5);
            optimizeBtn = new JButton("Optimize Network");
            shortestPathBtn = new JButton("Shortest Path");
            addNodeBtn = new JButton("Add Node");
            addEdgeBtn = new JButton("Add Edge");

            costLabel = new JLabel("Total Cost: 0");
            latencyLabel = new JLabel("Avg Latency: 0");
            costLabel.setForeground(Color.WHITE);
            latencyLabel.setForeground(Color.WHITE);
            costLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            latencyLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

            add(createLabelPanel("Alpha (Cost Weight): ", alphaField));
            add(createLabelPanel("Beta (Latency Weight): ", betaField));
            add(Box.createVerticalStrut(10));
            add(optimizeBtn);
            add(Box.createVerticalStrut(10));
            add(shortestPathBtn);
            add(Box.createVerticalStrut(10));
            add(addNodeBtn);
            add(Box.createVerticalStrut(10));
            add(addEdgeBtn);
            add(Box.createVerticalStrut(30));
            add(costLabel);
            add(Box.createVerticalStrut(10));
            add(latencyLabel);
            add(Box.createVerticalGlue());

            // Button Actions
            optimizeBtn.addActionListener(e -> optimizeNetwork());
            shortestPathBtn.addActionListener(e -> computeShortestPath());
            addNodeBtn.addActionListener(e -> addNode());
            addEdgeBtn.addActionListener(e -> addEdge());
        }

        private JPanel createLabelPanel(String labelText, JTextField textField) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panel.setOpaque(false);
            JLabel label = new JLabel(labelText);
            label.setForeground(Color.WHITE);
            panel.add(label);
            panel.add(textField);
            return panel;
        }

        private void optimizeNetwork() {
            // Reset flags
            for (Edge edge : edges) {
                edge.optimized = false;
                edge.shortestPath = false;
            }

            double alpha, beta;
            try {
                alpha = Double.parseDouble(alphaField.getText());
                beta = Double.parseDouble(betaField.getText());
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid alpha or beta value.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Compute MST using Kruskal's algorithm with a weighted value: alpha*cost + beta*(1/bandwidth)
            List<Edge> mst = computeMST(alpha, beta);
            double totalCost = 0;
            double totalLatency = 0;
            for (Edge edge : mst) {
                edge.optimized = true;
                totalCost += edge.cost;
                totalLatency += (1.0 / edge.bandwidth);
            }
            costLabel.setText(String.format("Total Cost: %.1f", totalCost));
            latencyLabel.setText(String.format("Avg Latency: %.3f", totalLatency / mst.size()));
            graphPanel.repaint();
        }

        private List<Edge> computeMST(double alpha, double beta) {
            List<Edge> result = new ArrayList<>();
            List<Edge> sortedEdges = new ArrayList<>(edges);
            // Sort edges by weighted value: alpha*cost + beta*(1/bandwidth)
            Collections.sort(sortedEdges, (e1, e2) -> {
                double w1 = alpha * e1.cost + beta * (1.0 / e1.bandwidth);
                double w2 = alpha * e2.cost + beta * (1.0 / e2.bandwidth);
                return Double.compare(w1, w2);
            });

            // Union-Find structure for nodes
            Map<Node, Node> parent = new HashMap<>();
            for (Node node : nodes) {
                parent.put(node, node);
            }
            // Find with path compression
            Function<Node, Node> find = new Function<Node, Node>() {
                public Node apply(Node n) {
                    if (parent.get(n) != n) {
                        parent.put(n, this.apply(parent.get(n)));
                    }
                    return parent.get(n);
                }
            };

            // Union function
            BiConsumer<Node, Node> union = (n1, n2) -> {
                parent.put(find.apply(n1), find.apply(n2));
            };

            for (Edge edge : sortedEdges) {
                Node root1 = find.apply(edge.source);
                Node root2 = find.apply(edge.target);
                if (root1 != root2) {
                    result.add(edge);
                    union.accept(root1, root2);
                }
            }
            return result;
        }

        private void computeShortestPath() {
            // Reset shortestPath flags
            for (Edge edge : edges) {
                edge.shortestPath = false;
            }

            if (nodes.size() < 2) {
                JOptionPane.showMessageDialog(this, "Not enough nodes to compute a path.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create selection dialogs for source and target
            String[] nodeNames = nodes.stream().map(n -> n.name).toArray(String[]::new);
            String sourceName = (String) JOptionPane.showInputDialog(this, "Select source node:", "Shortest Path",
                    JOptionPane.PLAIN_MESSAGE, null, nodeNames, nodeNames[0]);
            String targetName = (String) JOptionPane.showInputDialog(this, "Select target node:", "Shortest Path",
                    JOptionPane.PLAIN_MESSAGE, null, nodeNames, nodeNames[0]);
            if (sourceName == null || targetName == null) return;

            Node source = null, target = null;
            for (Node node : nodes) {
                if (node.name.equals(sourceName)) source = node;
                if (node.name.equals(targetName)) target = node;
            }
            if (source == null || target == null) return;

            // Use Dijkstra's algorithm, with weight = 1/bandwidth (latency proxy)
            Map<Node, Double> dist = new HashMap<>();
            Map<Node, Edge> prevEdge = new HashMap<>();
            for (Node node : nodes) {
                dist.put(node, Double.MAX_VALUE);
            }
            dist.put(source, 0.0);

            PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
            queue.add(source);

            while (!queue.isEmpty()) {
                Node u = queue.poll();
                if(u.equals(target)) break;
                // Check each edge connected to u
                for (Edge edge : edges) {
                    if (edge.source.equals(u) || edge.target.equals(u)) {
                        Node v = edge.source.equals(u) ? edge.target : edge.source;
                        double weight = 1.0 / edge.bandwidth;
                        double alt = dist.get(u) + weight;
                        if (alt < dist.get(v)) {
                            dist.put(v, alt);
                            prevEdge.put(v, edge);
                            queue.remove(v);
                            queue.add(v);
                        }
                    }
                }
            }

            // Reconstruct path from source to target
            List<Edge> path = new ArrayList<>();
            Node current = target;
            while (!current.equals(source)) {
                Edge edge = prevEdge.get(current);
                if (edge == null) {
                    JOptionPane.showMessageDialog(this, "No path found.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                path.add(edge);
                current = edge.source.equals(current) ? edge.target : edge.source;
            }

            // Mark edges in the shortest path
            for (Edge edge : path) {
                edge.shortestPath = true;
            }
            graphPanel.repaint();
        }

        private void addNode() {
            JTextField nameField = new JTextField();
            JTextField xField = new JTextField();
            JTextField yField = new JTextField();
            Object[] message = {
                    "Node Name:", nameField,
                    "X Coordinate:", xField,
                    "Y Coordinate:", yField
            };
            int option = JOptionPane.showConfirmDialog(this, message, "Add Node", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                String name = nameField.getText().trim();
                int x, y;
                try {
                    x = Integer.parseInt(xField.getText().trim());
                    y = Integer.parseInt(yField.getText().trim());
                } catch(NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid coordinates.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                nodes.add(new Node(name, x, y));
                graphPanel.repaint();
            }
        }

        private void addEdge() {
            if (nodes.size() < 2) {
                JOptionPane.showMessageDialog(this, "Not enough nodes to add an edge.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String[] nodeNames = nodes.stream().map(n -> n.name).toArray(String[]::new);
            JComboBox<String> sourceBox = new JComboBox<>(nodeNames);
            JComboBox<String> targetBox = new JComboBox<>(nodeNames);
            JTextField costField = new JTextField();
            JTextField bandwidthField = new JTextField();
            Object[] message = {
                    "Source Node:", sourceBox,
                    "Target Node:", targetBox,
                    "Cost:", costField,
                    "Bandwidth:", bandwidthField
            };
            int option = JOptionPane.showConfirmDialog(this, message, "Add Edge", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                String sourceName = (String) sourceBox.getSelectedItem();
                String targetName = (String) targetBox.getSelectedItem();
                double cost, bandwidth;
                try {
                    cost = Double.parseDouble(costField.getText().trim());
                    bandwidth = Double.parseDouble(bandwidthField.getText().trim());
                } catch(NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid cost or bandwidth.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Node source = null, target = null;
                for (Node node : nodes) {
                    if (node.name.equals(sourceName)) source = node;
                    if (node.name.equals(targetName)) target = node;
                }
                if (source == null || target == null) return;
                edges.add(new Edge(source, target, cost, bandwidth));
                graphPanel.repaint();
            }
        }
    }
}
