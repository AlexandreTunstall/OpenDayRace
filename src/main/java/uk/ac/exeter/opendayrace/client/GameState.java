package uk.ac.exeter.opendayrace.client;

import uk.ac.exeter.opendayrace.common.world.Node;
import uk.ac.exeter.opendayrace.common.world.World;
import uk.ac.exeter.opendayrace.common.world.WorldPath;

import java.util.function.Consumer;

public class GameState {

    private final World world;
    private volatile State state;
    private volatile Consumer<WorldPath> callback;
    private volatile int travelTime;

    public GameState() {
        Node[] nodes = new Node[4];
        nodes[0] = new Node(152, 303, 645, 237);
        nodes[1] = new Node(487, 673, 722, 585);
        nodes[2] = new Node(645, 237, 1320, 273);
        nodes[3] = new Node(722, 585, 1260, 590);
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

    public World getWorld() {
        return world;
    }

    public State getState() {
        return state;
    }

    public int getTravelTime() {
        return travelTime;
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
        DISPLAYING_TIME
    }
}
