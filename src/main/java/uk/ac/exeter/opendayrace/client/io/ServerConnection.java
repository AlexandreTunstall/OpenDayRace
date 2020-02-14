package uk.ac.exeter.opendayrace.client.io;

import uk.ac.exeter.opendayrace.client.GameState;
import uk.ac.exeter.opendayrace.common.world.WorldPath;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import static uk.ac.exeter.opendayrace.common.io.NetworkConstants.*;

public class ServerConnection {
    // Must be large enough to store all the bytes that will be handled at the same time
    private static final int BUFFER_SIZE = 4096;

    private final GameState game;
    private final Consumer<IOException> onException;
    private final SocketChannel socket;
    private final ByteBuffer buffer;

    public ServerConnection(GameState game, InetSocketAddress address, Consumer<IOException> onException) throws IOException {
        this.game = game;
        this.onException = onException;
        socket = SocketChannel.open(address);
        buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        buffer.put(VERSION);
        buffer.put(VERSION_END);
        writeBuffer();
        readStatus();
    }

    private void readStatus() throws IOException {
        buffer.limit(1);
        readBuffer();
        byte status = buffer.get();
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
        // TODO Set state based on status code
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
}
