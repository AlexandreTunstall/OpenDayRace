package uk.ac.exeter.opendayrace.server;

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
            // TODO Handle path logic and send message to clients
        }
    }

    @Override
    public void close() {
        closed = true;
    }
}
