package com.mycompany.networkoptimizer;

import javax.swing.*;
import javax.swing.border.*;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class OptimizerDemo extends JFrame {

    // UI Components
    private GraphPanel graphPanel;
    private ControlPanel controlPanel;
    private ToolPanel toolPanel;
    private JLabel statusBar; // for user messages

    // Data Model
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private Node selectedNode;
    private final AtomicReference<Edge> tempEdge = new AtomicReference<>();
    private static final double LATENCY_FACTOR = 1000.0;

    public OptimizerDemo() {
        configureLookAndFeel();
        initComponents();
        configureWindow();
    }

    private void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() {
        graphPanel = new GraphPanel(nodes, edges, tempEdge);
        controlPanel = new ControlPanel(
                (alpha, beta) -> handleOptimize(alpha, beta),
                (start, end) -> handlePathFind(start, end),
                nodes
        );
        toolPanel = new ToolPanel(tool -> handleToolChange(tool));
        statusBar = new JLabel("Ready");
        statusBar.setBorder(new EmptyBorder(5, 10, 5, 10));
        statusBar.setForeground(Color.WHITE);
        statusBar.setBackground(new Color(45, 45, 50));
        statusBar.setOpaque(true);

        setupGraphInteractions();

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(toolPanel, BorderLayout.NORTH);
        mainPanel.add(graphPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.EAST);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        add(mainPanel);
    }

    // --- Interaction Handlers ---
    private void handleToolChange(Tool tool) {
        graphPanel.clearPreview();
        graphPanel.repaint();
        updateStatus("Tool changed to: " + tool.toString());
    }

    private void setupGraphInteractions() {
        // Add a mouse listener that handles press, release, drag, and double-click events
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePress(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseRelease(e);
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDrag(e);
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                if (toolPanel.getCurrentTool() == Tool.EDGE_CREATION && tempEdge.get() != null) {
                    graphPanel.setPreviewPoint(e.getPoint());
                }
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleEdgeDoubleClick(e);
                }
            }
        };
        graphPanel.addMouseListener(mouseAdapter);
        graphPanel.addMouseMotionListener(mouseAdapter);
    }

    private void handleMousePress(MouseEvent e) {
        Point p = e.getPoint();
        if (SwingUtilities.isRightMouseButton(e)) {
            showContextMenu(p);
            return;
        }

        Optional<Node> clickedNode = nodes.stream()
                .filter(n -> n.getBounds().contains(p))
                .findFirst();

        if (clickedNode.isPresent()) {
            selectedNode = clickedNode.get();
            if (toolPanel.getCurrentTool() == Tool.EDGE_CREATION) {
                // Begin edge creation and show preview
                tempEdge.set(new Edge(selectedNode, null, 0, 0));
                graphPanel.setPreviewPoint(p);
                updateStatus("Edge creation: select target node.");
            }
        } else if (toolPanel.getCurrentTool() == Tool.NODE_CREATION) {
            Node newNode = new Node("Node " + (nodes.size() + 1), p.x, p.y);
            nodes.add(newNode);
            controlPanel.refreshCombos(nodes);
            graphPanel.repaint();
            updateStatus("New node created: " + newNode.getId());
        }
    }

    private void handleMouseRelease(MouseEvent e) {
        if (tempEdge.get() != null && selectedNode != null) {
            nodes.stream()
                    .filter(n -> n != selectedNode && n.getBounds().contains(e.getPoint()))
                    .findFirst()
                    .ifPresent(target -> {
                        Edge newEdge = promptForEdgeProperties(selectedNode, target);
                        if (newEdge != null) {
                            edges.add(newEdge);
                            controlPanel.refreshCombos(nodes);
                            updateStatus("New edge created between " + selectedNode.getId() + " and " + target.getId());
                        }
                    });
            tempEdge.set(null);
            graphPanel.clearPreview();
        }
        selectedNode = null;
        graphPanel.repaint();
    }

    private void handleMouseDrag(MouseEvent e) {
        if (selectedNode != null && toolPanel.getCurrentTool() == Tool.SELECTION) {
            selectedNode.setPosition(e.getX(), e.getY());
            graphPanel.repaint();
            updateStatus("Moving node: " + selectedNode.getId());
        } else if (toolPanel.getCurrentTool() == Tool.EDGE_CREATION && tempEdge.get() != null) {
            graphPanel.setPreviewPoint(e.getPoint());
            graphPanel.repaint();
        }
    }

    // Handle double-click events on edges (to edit cost/bandwidth)
    private void handleEdgeDoubleClick(MouseEvent e) {
        Point p = e.getPoint();
        for (Edge edge : edges) {
            int midX = (edge.source.getX() + edge.target.getX()) / 2;
            int midY = (edge.source.getY() + edge.target.getY()) / 2;
            Point mid = new Point(midX, midY);
            if (mid.distance(p) < 20) { // threshold distance
                editEdgeProperties(edge);
                break;
            }
        }
    }

    private void editEdgeProperties(Edge edge) {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        JTextField costField = new JTextField(String.valueOf(edge.getCost()));
        JTextField bandwidthField = new JTextField(String.valueOf(edge.getBandwidth()));

        panel.add(new JLabel("Edit Cost:"));
        panel.add(costField);
        panel.add(new JLabel("Edit Bandwidth (Mbps):"));
        panel.add(bandwidthField);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Edit Edge Properties",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                double cost = Double.parseDouble(costField.getText());
                double bandwidth = Double.parseDouble(bandwidthField.getText());
                if (cost <= 0 || bandwidth <= 0) {
                    JOptionPane.showMessageDialog(this, "Cost and Bandwidth must be positive numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                edge.setCost(cost);
                edge.setBandwidth(bandwidth);
                graphPanel.repaint();
                updateMetrics();
                updateStatus("Edge updated between " + edge.source.getId() + " and " + edge.target.getId());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid numeric values.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Edge promptForEdgeProperties(Node source, Node target) {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        JTextField costField = new JTextField("10");
        JTextField bandwidthField = new JTextField("100");

        panel.add(new JLabel("Cost:"));
        panel.add(costField);
        panel.add(new JLabel("Bandwidth (Mbps):"));
        panel.add(bandwidthField);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Edge Properties",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                double cost = Double.parseDouble(costField.getText());
                double bandwidth = Double.parseDouble(bandwidthField.getText());
                if (cost <= 0 || bandwidth <= 0) {
                    JOptionPane.showMessageDialog(this, "Cost and Bandwidth must be positive numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                return new Edge(source, target, cost, bandwidth);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid numeric values.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return null;
    }

    private void showContextMenu(Point p) {
        JPopupMenu contextMenu = new JPopupMenu();

        nodes.stream()
                .filter(n -> n.getBounds().contains(p))
                .findFirst()
                .ifPresent(node -> {
                    JMenuItem deleteNode = new JMenuItem("Delete Node");
                    deleteNode.addActionListener(e -> deleteNode(node));
                    contextMenu.add(deleteNode);
                });

        edges.stream()
                .filter(e -> e.containsPoint(p))
                .findFirst()
                .ifPresent(edge -> {
                    JMenuItem deleteEdge = new JMenuItem("Delete Edge");
                    deleteEdge.addActionListener(e -> deleteEdge(edge));
                    contextMenu.add(deleteEdge);
                });

        contextMenu.show(graphPanel, p.x, p.y);
    }

    private void deleteNode(Node node) {
        nodes.remove(node);
        edges.removeIf(e -> e.connects(node));
        controlPanel.refreshCombos(nodes);
        graphPanel.repaint();
        updateStatus("Deleted node: " + node.getId());
    }

    private void deleteEdge(Edge edge) {
        edges.remove(edge);
        graphPanel.repaint();
        updateStatus("Deleted an edge.");
    }

    // --- Core Functionality ---
    private void handleOptimize(double alpha, double beta) {
        if (!isGraphConnected()) {
            JOptionPane.showMessageDialog(this, "Graph is not connected!", "Optimization Error", JOptionPane.ERROR_MESSAGE);
            updateStatus("Optimization failed: Graph is not connected.");
            return;
        }

        new SwingWorker<List<Edge>, Void>() {
            @Override
            protected List<Edge> doInBackground() {
                return new KruskalOptimizer().findOptimalNetwork(nodes, edges, alpha, beta);
            }

            @Override
            protected void done() {
                try {
                    List<Edge> optimalEdges = get();
                    edges.forEach(e -> e.setActive(false));
                    optimalEdges.forEach(e -> e.setActive(true));
                    updateMetrics();
                    graphPanel.repaint();
                    updateStatus("Optimization complete. Optimal network highlighted.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    updateStatus("Error during optimization.");
                }
            }
        }.execute();
    }

    private void handlePathFind(Node start, Node end) {
        new SwingWorker<List<Node>, Void>() {
            @Override
            protected List<Node> doInBackground() {
                return new DijkstraPathFinder().findShortestPath(edges, start, end);
            }

            @Override
            protected void done() {
                try {
                    List<Node> path = get();
                    if (path.isEmpty()) {
                        JOptionPane.showMessageDialog(OptimizerDemo.this,
                                "No path exists between selected nodes",
                                "Path Finding",
                                JOptionPane.INFORMATION_MESSAGE);
                        updateStatus("Path finding: No path exists.");
                    } else {
                        highlightPath(path);
                        updateStatus("Path found between " + start.getId() + " and " + end.getId());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    updateStatus("Error during path finding.");
                }
            }
        }.execute();
    }

    // --- Helper Methods ---
    private void updateMetrics() {
        double totalCost = edges.stream().mapToDouble(Edge::getCost).sum();
        double avgLatency = edges.stream()
                .mapToDouble(e -> (e.getBandwidth() > 0 ? LATENCY_FACTOR / e.getBandwidth() : Double.POSITIVE_INFINITY))
                .average()
                .orElse(0);
        controlPanel.updateMetrics(totalCost, avgLatency);
    }

    private void highlightPath(List<Node> path) {
        edges.forEach(e -> e.setHighlighted(false));
        for (int i = 0; i < path.size() - 1; i++) {
            Node a = path.get(i);
            Node b = path.get(i + 1);
            edges.stream()
                    .filter(e -> e.connects(a, b))
                    .forEach(e -> e.setHighlighted(true));
        }
        graphPanel.repaint();
    }

    private boolean isGraphConnected() {
        if (nodes.isEmpty()) {
            return false;
        }
        Set<Node> visited = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();
        queue.add(nodes.get(0));

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (visited.add(current)) {
                edges.stream()
                        .filter(e -> e.connects(current))
                        .map(e -> e.otherEnd(current))
                        .forEach(queue::add);
            }
        }
        return visited.size() == nodes.size();
    }

    private void updateStatus(String message) {
        statusBar.setText(message);
    }

    private void configureWindow() {
        setPreferredSize(new Dimension(1280, 720));
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(NetworkOptimizer::new);
    }

    // --- Domain Model Classes ---
    static class Node {
        private final String id;
        private int x, y;
        private static final int SIZE = 40;

        public Node(String id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        public Rectangle getBounds() {
            return new Rectangle(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public String getId() {
            return id;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    static class Edge {
        private final Node source, target;
        private double cost;
        private double bandwidth;
        private boolean active;
        private boolean highlighted;

        public Edge(Node source, Node target, double cost, double bandwidth) {
            this.source = source;
            this.target = target;
            this.cost = cost;
            this.bandwidth = bandwidth;
        }

        public boolean containsPoint(Point p) {
            Line2D line = new Line2D.Float(source.getX(), source.getY(), target.getX(), target.getY());
            return line.ptSegDist(p) < 5;
        }

        public boolean connects(Node node) {
            return source == node || target == node;
        }

        public boolean connects(Node a, Node b) {
            return (source == a && target == b) || (source == b && target == a);
        }

        public Node otherEnd(Node node) {
            return node == source ? target : source;
        }

        public double getCost() {
            return cost;
        }

        public double getBandwidth() {
            return bandwidth;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }

        public void setBandwidth(double bandwidth) {
            this.bandwidth = bandwidth;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void setHighlighted(boolean highlighted) {
            this.highlighted = highlighted;
        }
    }

    // --- Algorithm Implementations ---
    static class KruskalOptimizer {
        public List<Edge> findOptimalNetwork(List<Node> nodes, List<Edge> edges, double alpha, double beta) {
            List<Edge> sortedEdges = new ArrayList<>(edges);
            sortedEdges.sort(Comparator.comparingDouble(e ->
                    alpha * e.getCost() + beta * (LATENCY_FACTOR / e.getBandwidth())));

            UnionFind uf = new UnionFind(nodes);
            List<Edge> mst = new ArrayList<>();

            for (Edge edge : sortedEdges) {
                Node a = edge.source;
                Node b = edge.target;
                if (uf.find(a) != uf.find(b)) {
                    uf.union(a, b);
                    mst.add(edge);
                    if (mst.size() == nodes.size() - 1) {
                        break;
                    }
                }
            }
            return mst;
        }
    }

    static class DijkstraPathFinder {
        public List<Node> findShortestPath(List<Edge> edges, Node start, Node end) {
            Map<Node, Double> dist = new HashMap<>();
            Map<Node, Node> prev = new HashMap<>();
            PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));

            dist.put(start, 0.0);
            pq.add(start);

            while (!pq.isEmpty()) {
                Node u = pq.poll();
                for (Edge e : getAdjacentEdges(edges, u)) {
                    Node v = e.otherEnd(u);
                    double weight = e.getBandwidth() > 0 ? LATENCY_FACTOR / e.getBandwidth() : Double.POSITIVE_INFINITY;
                    double alt = dist.getOrDefault(u, Double.MAX_VALUE) + weight;
                    if (alt < dist.getOrDefault(v, Double.MAX_VALUE)) {
                        dist.put(v, alt);
                        prev.put(v, u);
                        pq.add(v);
                    }
                }
            }
            return buildPath(prev, end);
        }

        private List<Edge> getAdjacentEdges(List<Edge> edges, Node node) {
            List<Edge> adjacent = new ArrayList<>();
            for (Edge e : edges) {
                if (e.connects(node)) {
                    adjacent.add(e);
                }
            }
            return adjacent;
        }

        private List<Node> buildPath(Map<Node, Node> prev, Node end) {
            LinkedList<Node> path = new LinkedList<>();
            for (Node at = end; at != null; at = prev.get(at)) {
                path.addFirst(at);
            }
            return path.size() > 1 ? path : Collections.emptyList();
        }
    }

    // --- Utility Classes ---
    static class UnionFind {
        private final Map<Node, Node> parent = new HashMap<>();

        public UnionFind(List<Node> nodes) {
            nodes.forEach(n -> parent.put(n, n));
        }

        public Node find(Node n) {
            if (parent.get(n) != n) {
                parent.put(n, find(parent.get(n)));
            }
            return parent.get(n);
        }

        public void union(Node a, Node b) {
            Node rootA = find(a);
            Node rootB = find(b);
            if (rootA != rootB) {
                parent.put(rootB, rootA);
            }
        }
    }

    // --- UI Components ---
    static class GraphPanel extends JPanel {
        private static final Color NODE_GRADIENT_START = new Color(100, 200, 255);
        private static final Color NODE_GRADIENT_END = new Color(0, 100, 200);
        private static final Color ACTIVE_EDGE = new Color(50, 200, 100);
        private static final Color HIGHLIGHT_EDGE = new Color(255, 100, 100);
        private static final Stroke DASHED_STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);

        private final List<Node> nodes;
        private final List<Edge> edges;
        private final AtomicReference<Edge> tempEdge;
        private Point previewPoint = null; // For edge preview

        public GraphPanel(List<Node> nodes, List<Edge> edges, AtomicReference<Edge> tempEdge) {
            this.nodes = nodes;
            this.edges = edges;
            this.tempEdge = tempEdge;
            setBackground(new Color(30, 30, 35));
        }

        public void setPreviewPoint(Point p) {
            this.previewPoint = p;
            repaint();
        }

        public void clearPreview() {
            this.previewPoint = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            paintEdges(g2);
            if (previewPoint != null) {
                paintPreviewEdge(g2);
            }
            paintNodes(g2);
        }

        private void paintEdges(Graphics2D g2) {
            for (Edge e : edges) {
                Color color = e.highlighted ? HIGHLIGHT_EDGE
                        : e.active ? ACTIVE_EDGE : new Color(150, 150, 150);
                paintEdge(g2, e, color, e.highlighted ? 4 : e.active ? 3 : 2);
            }
        }

        private void paintEdge(Graphics2D g2, Edge e, Color color, int width) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(e.source.getX(), e.source.getY(), e.target.getX(), e.target.getY());

            String label = String.format("$%.1f | %.1fms", e.getCost(),
                    e.getBandwidth() > 0 ? LATENCY_FACTOR / e.getBandwidth() : 0);
            FontMetrics fm = g2.getFontMetrics();
            Point2D mid = new Point2D.Float(
                    (e.source.getX() + e.target.getX()) / 2f,
                    (e.source.getY() + e.target.getY()) / 2f
            );

            g2.setColor(new Color(255, 255, 255, 200));
            g2.fillRoundRect(
                    (int) mid.getX() - fm.stringWidth(label) / 2 - 3,
                    (int) mid.getY() - fm.getHeight() / 2 - 3,
                    fm.stringWidth(label) + 6,
                    fm.getHeight() + 6,
                    8, 8
            );
            g2.setColor(Color.BLACK);
            g2.drawString(label, (int) mid.getX() - fm.stringWidth(label) / 2, (int) mid.getY() + fm.getAscent() / 2);
        }

        private void paintPreviewEdge(Graphics2D g2) {
            if (tempEdge.get() != null && previewPoint != null) {
                Node source = tempEdge.get().source;
                g2.setColor(Color.YELLOW);
                g2.setStroke(DASHED_STROKE);
                g2.drawLine(source.getX(), source.getY(), previewPoint.x, previewPoint.y);
            }
        }

        private void paintNodes(Graphics2D g2) {
            for (Node n : nodes) {
                // Shadow
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillOval(n.getX() - 22, n.getY() - 22, 44, 44);

                // Gradient fill
                GradientPaint gradient = new GradientPaint(
                        n.getX() - 20, n.getY() - 20, NODE_GRADIENT_START,
                        n.getX() + 20, n.getY() + 20, NODE_GRADIENT_END
                );
                g2.setPaint(gradient);
                g2.fillOval(n.getX() - 20, n.getY() - 20, 40, 40);

                // Border
                g2.setColor(new Color(255, 255, 255, 100));
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(n.getX() - 20, n.getY() - 20, 40, 40);

                // Label
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(n.getId(), n.getX() - fm.stringWidth(n.getId()) / 2, n.getY() + fm.getAscent() / 4);
            }
        }
    }

    static class ControlPanel extends JPanel {
        private final JLabel costLabel = new JLabel("Total Cost: $0.00");
        private final JLabel latencyLabel = new JLabel("Avg Latency: 0ms");
        private final JComboBox<Node> startCombo = new JComboBox<>();
        private final JComboBox<Node> endCombo = new JComboBox<>();

        public ControlPanel(BiConsumer<Double, Double> optimizeHandler,
                            BiConsumer<Node, Node> pathHandler,
                            List<Node> nodes) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(15, 15, 15, 15));
            setBackground(new Color(45, 45, 50));

            // Optimization Controls
            JPanel optimizePanel = createSectionPanel("Network Optimization");
            JTextField alphaField = new JTextField("1.0");
            JTextField betaField = new JTextField("1.0");
            JButton optimizeBtn = createButton("Optimize", e -> {
                try {
                    optimizeHandler.accept(
                            Double.parseDouble(alphaField.getText()),
                            Double.parseDouble(betaField.getText())
                    );
                } catch (NumberFormatException ex) {
                    showError("Invalid weights");
                }
            });
            optimizeBtn.setToolTipText("Optimize the network based on the given weights.");

            optimizePanel.add(createLabelField("Cost Weight (α):", alphaField));
            optimizePanel.add(Box.createVerticalStrut(10));
            optimizePanel.add(createLabelField("Latency Weight (β):", betaField));
            optimizePanel.add(Box.createVerticalStrut(15));
            optimizePanel.add(optimizeBtn);

            // Path Finding Controls
            JPanel pathPanel = createSectionPanel("Path Finding");
            startCombo.setRenderer(new NodeRenderer());
            endCombo.setRenderer(new NodeRenderer());
            JButton pathBtn = createButton("Find Path", e -> {
                Node start = (Node) startCombo.getSelectedItem();
                Node end = (Node) endCombo.getSelectedItem();
                if (start != null && end != null && start != end) {
                    pathHandler.accept(start, end);
                }
            });
            pathBtn.setToolTipText("Find the shortest path between selected nodes.");

            pathPanel.add(createLabelCombo("Start:", startCombo));
            pathPanel.add(Box.createVerticalStrut(5));
            pathPanel.add(createLabelCombo("End:", endCombo));
            pathPanel.add(Box.createVerticalStrut(15));
            pathPanel.add(pathBtn);

            // Metrics Display
            JPanel metricsPanel = createSectionPanel("Metrics");
            metricsPanel.add(costLabel);
            metricsPanel.add(Box.createVerticalStrut(5));
            metricsPanel.add(latencyLabel);

            add(optimizePanel);
            add(Box.createVerticalStrut(20));
            add(pathPanel);
            add(Box.createVerticalStrut(20));
            add(metricsPanel);
            add(Box.createVerticalGlue());

            refreshCombos(nodes);
        }

        public void refreshCombos(List<Node> nodes) {
            DefaultComboBoxModel<Node> model = new DefaultComboBoxModel<>();
            nodes.forEach(model::addElement);
            startCombo.setModel(model);
            endCombo.setModel(model);
        }

        public void updateMetrics(double cost, double latency) {
            costLabel.setText(String.format("Total Cost: $%.2f", cost));
            latencyLabel.setText(String.format("Avg Latency: %.2fms", latency));
        }

        private JPanel createSectionPanel(String title) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(new CompoundBorder(
                    new TitledBorder(title),
                    new EmptyBorder(10, 10, 10, 10)
            ));
            panel.setBackground(new Color(55, 55, 60));
            return panel;
        }

        private JButton createButton(String text, ActionListener listener) {
            JButton btn = new JButton(text);
            btn.addActionListener(listener);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(200, 30));
            btn.setBackground(new Color(80, 80, 90));
            btn.setForeground(Color.WHITE);
            return btn;
        }

        private JPanel createLabelField(String labelText, JComponent comp) {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel label = new JLabel(labelText);
            label.setForeground(Color.WHITE);
            panel.add(label, BorderLayout.NORTH);
            panel.add(comp, BorderLayout.CENTER);
            panel.setOpaque(false);
            return panel;
        }

        private JPanel createLabelCombo(String labelText, JComboBox<Node> combo) {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel label = new JLabel(labelText);
            label.setForeground(Color.WHITE);
            panel.add(label, BorderLayout.NORTH);
            panel.add(combo, BorderLayout.CENTER);
            panel.setOpaque(false);
            return panel;
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static class ToolPanel extends JPanel {
        private final ButtonGroup group = new ButtonGroup();
        private Tool currentTool = Tool.SELECTION;

        public ToolPanel(Consumer<Tool> toolConsumer) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
            setBorder(new EmptyBorder(5, 10, 5, 10));
            setBackground(new Color(45, 45, 50));

            Arrays.stream(Tool.values()).forEach(tool -> {
                JToggleButton btn = new JToggleButton(tool.toString());
                btn.setFocusPainted(false);
                btn.setBackground(new Color(60, 60, 70));
                btn.setForeground(Color.WHITE);
                btn.addActionListener(e -> {
                    currentTool = tool;
                    toolConsumer.accept(tool);
                });
                btn.setToolTipText("Tool: " + tool.toString());
                group.add(btn);
                add(btn);
            });
            if (group.getElements().hasMoreElements()) {
                group.getElements().nextElement().setSelected(true);
            }
        }

        public Tool getCurrentTool() {
            return currentTool;
        }
    }

    enum Tool {
        SELECTION("Select"),
        NODE_CREATION("Add Node"),
        EDGE_CREATION("Add Edge");

        private final String label;

        Tool(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    static class NodeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Node) {
                setText(((Node) value).getId());
            }
            return this;
        }
    }
}
