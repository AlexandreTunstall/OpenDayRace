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
        // If this is the first join, the game thread is waiting, so we try waking it up
        notifyPathSelected();
    }

    public void removePlayer(ClientConnection player) {
        System.out.println("Client disconnected");
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
            System.out.println("Processing join queue");
            synchronized (joinQueue) {
                players.addAll(joinQueue);
                joinQueue.clear();
            }
            boolean doneWaiting = false;
            synchronized (notifier) {
                for (ClientConnection player : players) {
                    player.onGameReady();
                }
                while (!doneWaiting) {
                    try {
                        notifier.wait();
                    } catch (InterruptedException ignored) { }
                    doneWaiting = true;
                    System.out.println("Processing leave queue");
                    synchronized (leaveQueue) {
                        players.removeAll(leaveQueue);
                        leaveQueue.clear();
                    }
                    for (ClientConnection player : players) {
                        if (player.selectedPath == null) {
                            System.out.println("Not done waiting though");
                            doneWaiting = false;
                            break;
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
            double FIXED_TIME_PATH_TIME = 10;
            double WEIGHTED_PATH_WEIGHT = 2;
            // Calculate path times
            int left_left_time = (int) Math.ceil(left_1_players / WEIGHTED_PATH_WEIGHT + FIXED_TIME_PATH_TIME);
            int left_right_time = (int) Math.ceil(left_1_players / WEIGHTED_PATH_WEIGHT + right_2_players / WEIGHTED_PATH_WEIGHT);
            int right_right_time = (int) Math.ceil(FIXED_TIME_PATH_TIME + right_2_players / WEIGHTED_PATH_WEIGHT);
            int right_left_time = (int) Math.ceil(FIXED_TIME_PATH_TIME + FIXED_TIME_PATH_TIME);
            // Let the players know of their mistakes
            for (ClientConnection player : players) {
                // Send people down each path
                int time;
                switch (player.selectedPath) {
                    case LEFT_LEFT:
                        time = left_left_time;
                        break;
                    case LEFT_RIGHT:
                        time = left_right_time;
                        break;
                    case RIGHT_LEFT:
                        time = right_left_time;
                        break;
                    case RIGHT_RIGHT:
                        time = right_right_time;
                        break;
                    default:
                        throw new IllegalStateException("player selected invalid path");
                }
                player.onTimeCalculated(time, left_1_players, right_1_players, left_2_players, right_2_players);
            }
            if (players.size() > 0) {
                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void close() {
        closed = true;
    }
}
