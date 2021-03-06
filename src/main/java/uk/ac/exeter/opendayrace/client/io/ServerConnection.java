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
    private final ByteBuffer readBuffer, writeBuffer;
    private final AtomicBoolean reconnect;

    public ServerConnection(GameState game, SocketAddress address, Consumer<IOException> onException) throws IOException {
        this.game = game;
        this.address = address;
        this.onException = onException;
        readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        writeBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        reconnect = new AtomicBoolean(true);
        game.setReconnect(this::reconnect);
    }

    private void readStatus() throws IOException {
        System.out.println("Reading status");
        readBuffer.limit(1);
        readBuffer();
        byte status = readBuffer.get();
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
                readBuffer.clear();
                readBuffer.limit(4);
                readBuffer();
                game.setShowPaths(readBuffer.getInt());
                break;
            case PLAYER_PATH_COUNT_LEFT_1:
                readSetPathCount(0);
                break;
            case PLAYER_PATH_COUNT_RIGHT_1:
                readSetPathCount(1);
                break;
            case PLAYER_PATH_COUNT_LEFT_2:
                readSetPathCount(2);
                break;
            case PLAYER_PATH_COUNT_RIGHT_2:
                readSetPathCount(3);
                break;
        }
    }

    private void readSetPathCount(int i) throws IOException {
        readBuffer.clear();
        readBuffer.limit(4);
        readBuffer();
        int count = readBuffer.getInt();
        System.out.println("Player Count Got: " + count);
        game.setPlayerPathCounts(i, count);
    }

    private void sendSelection(WorldPath selection) {
        System.out.println("Sending selection");
        game.setWaitingForPlayers();
        writeBuffer.clear();
        switch (selection) {
            case LEFT_LEFT:
                writeBuffer.put(PATH_LEFT_LEFT);
                break;
            case LEFT_RIGHT:
                writeBuffer.put(PATH_LEFT_RIGHT);
                break;
            case RIGHT_LEFT:
                writeBuffer.put(PATH_RIGHT_LEFT);
                break;
            case RIGHT_RIGHT:
                writeBuffer.put(PATH_RIGHT_RIGHT);
                break;
        }
        writeBuffer.flip();
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
        while (writeBuffer.position() < writeBuffer.limit()) {
            socket.write(writeBuffer);
        }
        writeBuffer.clear();
    }

    private void readBuffer() throws IOException {
        while (readBuffer.position() < readBuffer.limit()) {
            socket.read(readBuffer);
        }
        readBuffer.flip();
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
                    writeBuffer.clear();
                    writeBuffer.put(VERSION);
                    writeBuffer.put(VERSION_END);
                    writeBuffer.flip();
                    writeBuffer();
                }
                readBuffer.clear();
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
