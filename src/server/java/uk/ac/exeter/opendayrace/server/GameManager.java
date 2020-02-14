package uk.ac.exeter.opendayrace.server;

import uk.ac.exeter.opendayrace.common.world.WorldPath;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GameManager implements Runnable, AutoCloseable {
    private volatile boolean closed;
    private final Deque<ClientConnection> joinQueue;
    private final Deque<ClientConnection> leaveQueue;
    private final Object notifier;

    public GameManager() {
        // This will be written to by multiple threads, so it must be concurrent
        joinQueue = new ConcurrentLinkedDeque<>();
        leaveQueue = new ConcurrentLinkedDeque<>();
        notifier = new Object();
    }

    public void addPlayer(ClientConnection player) {
        joinQueue.add(player);
    }

    public void removePlayer(ClientConnection player) {
        leaveQueue.add(player);
        // In case the game thread is waiting for this player to select a path
        notifyPathSelected();
    }

    public void notifyPathSelected() {
        synchronized (notifier) {
            notifier.notifyAll();
        }
    }

    @Override
    public void run() {
        List<ClientConnection> players = new ArrayList<>();
        while (!closed) {
            synchronized (joinQueue) {
                players.addAll(joinQueue);
                joinQueue.clear();
            }
            synchronized (leaveQueue) {
                players.removeAll(leaveQueue);
                leaveQueue.clear();
            }
            for (ClientConnection player : players) {
                player.onGameReady();
            }
            boolean doneWaiting = false;
            synchronized (notifier) {
                while (!doneWaiting) {
                    doneWaiting = true;
                    for (ClientConnection player : players) {
                        if (player.selectedPath == null) {
                            doneWaiting = false;
                            break;
                        }
                    }
                    if (!doneWaiting) {
                        try {
                            notifier.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
            int left_1_players = 0;
            int left_2_players = 0;
            int right_1_players = 0;
            int right_2_players = 0;
            for (ClientConnection player : players) {
                if (player.selectedPath == WorldPath.LEFT_LEFT || player.selectedPath == WorldPath.LEFT_RIGHT) {
                    left_1_players++;
                } else {
                    right_1_players++;
                }
                if (player.selectedPath == WorldPath.LEFT_LEFT || player.selectedPath == WorldPath.RIGHT_LEFT) {
                    left_2_players++;
                } else {
                    right_2_players++;
                }
            }
            int FIXED_TIME_PATH_TIME = 45;
            int WEIGHTED_PATH_WEIGHT = 2;
            // Calculate path times
            int left_left_time = left_1_players / WEIGHTED_PATH_WEIGHT + FIXED_TIME_PATH_TIME;
            int left_right_time = left_1_players / WEIGHTED_PATH_WEIGHT + right_2_players / WEIGHTED_PATH_WEIGHT;
            int right_right_time = FIXED_TIME_PATH_TIME + right_2_players / WEIGHTED_PATH_WEIGHT;
            int right_left_time = FIXED_TIME_PATH_TIME + FIXED_TIME_PATH_TIME;
            // Let the players know of their mistakes
            for (ClientConnection player : players) {
                switch (player.selectedPath) {
                    case LEFT_LEFT:
                        player.onTimeCalculated(left_left_time);
                        break;
                    case LEFT_RIGHT:
                        player.onTimeCalculated(left_right_time);
                        break;
                    case RIGHT_LEFT:
                        player.onTimeCalculated(right_left_time);
                        break;
                    case RIGHT_RIGHT:
                        player.onTimeCalculated(right_right_time);
                        break;
                }
            }
        }
    }

    @Override
    public void close() {
        closed = true;
    }
}
