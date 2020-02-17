package uk.ac.exeter.opendayrace.common.world;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private double x1, y1;
    private List<Node> connectedNodes;
    private boolean selected = false;
    public final boolean startingNode;
    public final boolean endingNode;

    public Node(double x1, double y1, boolean isStartingNode, boolean isEndingNode) {
        this.x1 = x1;
        this.y1 = y1;
        this.connectedNodes = new ArrayList<>();
        this.startingNode = isStartingNode;
        this.endingNode = isEndingNode;
    }

    public Node(double x1, double y1) {
        this(x1, y1, false, false);
    }

    public double getX1() {
        return x1;
    }

    public double getY1() {
        return y1;
    }

    public void setSelected(boolean val) { this.selected = val; }

    public boolean isSelected() { return this.selected; }

    public void addConnection(Node node) {
        connectedNodes.add(node);
    }

    public List<Node> getConnectedNodes() { return connectedNodes; }
}
