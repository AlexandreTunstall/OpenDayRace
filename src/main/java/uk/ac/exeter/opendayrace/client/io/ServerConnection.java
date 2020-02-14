package uk.ac.exeter.opendayrace.client.io;

import uk.ac.exeter.opendayrace.client.GameState;
import uk.ac.exeter.opendayrace.common.world.WorldPath;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static uk.ac.exeter.opendayrace.common.io.NetworkConstants.*;

public class ServerConnection implements Runnable {
    // Must be large enough to store all the bytes that will be handled at the same time
    private static final int BUFFER_SIZE = 4096;

    private final GameState game;
    private final SocketAddress address;
    private final Consumer<IOException> onException;
    private SocketChannel socket;
    private final ByteBuffer buffer;
    private final AtomicBoolean reconnect;

    public ServerConnection(GameState game, SocketAddress address, Consumer<IOException> onException) throws IOException {
        this.game = game;
        this.address = address;
        this.onException = onException;
        buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        reconnect = new AtomicBoolean(true);
        game.setReconnect(this::reconnect);
    }

    private void readStatus() throws IOException {
        System.out.println("Reading status");
        buffer.limit(1);
        readBuffer();
        byte status = buffer.get();
        System.out.println("Got " + status);
        if (status < 0) {
            throw new IOException("Server sent failure status code: " + status);
        }
        switch (status) {
            case STATUS_OK:
                game.setWaitingForPlayers();
                break;
            case STATUS_AWAITING_SELECTION:
                game.setAwaitingSelection(this::sendSelection);
                break;
            case STATUS_SHOW_PATHS:
                buffer.clear();
                buffer.limit(1);
                readBuffer();
                game.setShowPaths(buffer.getInt());
                break;
        }
    }

    private void sendSelection(WorldPath selection) {
        game.setWaitingForPlayers();
        buffer.clear();
        switch (selection) {
            case LEFT_LEFT:
                buffer.put(PATH_LEFT_LEFT);
                break;
            case LEFT_RIGHT:
                buffer.put(PATH_LEFT_RIGHT);
                break;
            case RIGHT_LEFT:
                buffer.put(PATH_RIGHT_LEFT);
                break;
            case RIGHT_RIGHT:
                buffer.put(PATH_RIGHT_RIGHT);
                break;
        }
        buffer.flip();
        try {
            writeBuffer();
        } catch (IOException e) {
            onException.accept(e);
            try {
                socket.close();
            } catch (IOException e2) {
                onException.accept(e2);
            }
            reconnect();
        }
    }

    private void writeBuffer() throws IOException {
        while (buffer.position() < buffer.limit()) {
            socket.write(buffer);
        }
        buffer.clear();
    }

    private void readBuffer() throws IOException {
        while (buffer.position() < buffer.limit()) {
            socket.read(buffer);
        }
        buffer.flip();
    }

    private void reconnect() {
        game.setConnecting();
        synchronized (reconnect) {
            reconnect.set(true);
            reconnect.notifyAll();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (reconnect.getAndSet(false)) {
                    socket = SocketChannel.open(address);
                    buffer.clear();
                    buffer.put(VERSION);
                    buffer.put(VERSION_END);
                    buffer.flip();
                    writeBuffer();
                }
                buffer.clear();
                readStatus();
            } catch (IOException e) {
                onException.accept(e);
                synchronized (reconnect) {
                    game.setLostConnection();
                    reconnect.set(false);
                    try {
                        reconnect.wait();
                    } catch (InterruptedException ignored) { }
                }
            }
        }
    }
}
