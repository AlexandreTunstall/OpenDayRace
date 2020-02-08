package uk.ac.exeter.opendayrace.common.world;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private double x1, y1, x2, y2;
    private List<Node> connectedNodes;

    public Node(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        connectedNodes = new ArrayList<>();
    }

    public double getX1() {
        return x1;
    }

    public double getY1() {
        return y1;
    }

    public double getX2() {
        return x2;
    }

    public double getY2() {
        return y2;
    }

    public void addConnection(Node node) {
        connectedNodes.add(node);
    }
}
