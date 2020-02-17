package uk.ac.exeter.opendayrace.client;

import uk.ac.exeter.opendayrace.common.world.Node;
import uk.ac.exeter.opendayrace.common.world.World;
import uk.ac.exeter.opendayrace.common.world.WorldPath;

import java.util.*;
import java.util.function.Consumer;

public class GameState {

    private final World world;
    private volatile State state;
    private volatile Consumer<WorldPath> callback;
    private volatile int travelTime;
    private volatile Runnable reconnect;
    private volatile List<Node> selectedPath = new ArrayList<Node>();

    public GameState() {
        Node[] nodes = new Node[3];
        //nodes[0] = new Node(152, 303);
        //nodes[0] = new Node(487, 673);
        nodes[0] = new Node(1930, 640, true, false);
        //nodes[0].addConnection(nodes[1]);
        nodes[1] = new Node(1560, 1400, true, false);
        //nodes[0].addConnection(nodes[2]);
        nodes[0].addConnection(nodes[1]);
        nodes[1].addConnection(nodes[0]);
        nodes[2] = new Node(3300, 1300, false, true);
        nodes[0].addConnection(nodes[2]);
        //nodes[5] = new Node(1260, 590);
        nodes[1].addConnection(nodes[2]);
        world = new World(nodes);
        state = State.CONNECTING;
    }

    public void setConnecting() {
        state = State.CONNECTING;
    }

    public void setAwaitingSelection(Consumer<WorldPath> callback) {
        this.callback = callback;
        state = State.AWAITING_SELECTION;
    }

    public void setWaitingForPlayers() {
        state = State.WAITING_FOR_PLAYERS;
    }

    public void setShowPaths(int travelTime) {
        this.travelTime = travelTime;
        state = State.DISPLAYING_TIME;
    }

    public void setWorldPath(WorldPath path) {
        callback.accept(path);
    }

    public void setLostConnection() {
        state = State.LOST_CONNECTION;
    }

    public void setReconnect(Runnable reconnect) {
        this.reconnect = reconnect;
    }

    public World getWorld() {
        return world;
    }

    public State getState() {
        return state;
    }

    public int getTravelTime() {
        return travelTime;
    }

    public void addToPath(Node node) {
        this.selectedPath.add(node);
    }

    public Node getLastNodeInPath() {
        if (this.selectedPath.isEmpty()) return null;
        return this.selectedPath.get(this.selectedPath.size() - 1);
    }

    public WorldPath calculatePath() {
        if (this.selectedPath.get(0) == this.world.getNodes()[0]) {
            // LEFT
            if (this.selectedPath.size() > 2) return WorldPath.LEFT_RIGHT;
            return WorldPath.LEFT_LEFT;
        } else {
            //RIGHT
            if (this.selectedPath.size() > 2) return WorldPath.RIGHT_LEFT;
            return WorldPath.RIGHT_RIGHT;
        }
    }

    public void popFromPath() {
        if (this.selectedPath.size() > 0) {
            this.selectedPath.remove(this.selectedPath.size() - 1);
        }
    }

    public enum State {
        /**
         * Connecting to the server
         */
        CONNECTING,
        /**
         * Waiting for the player to select a path
         */
        AWAITING_SELECTION,
        /**
         * Waiting for other players to select a path
         */
        WAITING_FOR_PLAYERS,
        /**
         * Displaying the path to the player
         */
        DISPLAYING_TIME,
        /**
         * Lost the connection and awaiting user input before trying to reconnect
         */
        LOST_CONNECTION
    }
}
