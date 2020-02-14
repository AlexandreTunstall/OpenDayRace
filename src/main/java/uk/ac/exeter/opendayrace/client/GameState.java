package uk.ac.exeter.opendayrace.client;

import uk.ac.exeter.opendayrace.common.world.WorldPath;

import java.util.function.Consumer;

public class GameState {

    private volatile State state;
    private volatile Consumer<WorldPath> callback;
    private volatile int travelTime;

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

    enum State {
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
