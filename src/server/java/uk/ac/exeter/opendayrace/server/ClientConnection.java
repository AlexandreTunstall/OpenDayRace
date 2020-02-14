package uk.ac.exeter.opendayrace.server;

import uk.ac.exeter.opendayrace.common.world.WorldPath;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import static uk.ac.exeter.opendayrace.common.io.NetworkConstants.*;

public class ClientConnection implements AutoCloseable {
    // Must be large enough to store all the bytes ensureRead can request
    private static final int BUFFER_SIZE = 4096;

    private final GameManager game;
    private final AsynchronousSocketChannel socket;
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;
    private final EnsureRead ensureRead;
    private final EnsureWrite ensureWrite;

    // Player state (only written to by the socket thread)
    volatile WorldPath selectedPath;

    public ClientConnection(GameManager game, AsynchronousSocketChannel socket) {
        this.game = game;
        this.socket = socket;
        readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        ensureRead = new EnsureRead();
        ensureWrite = new EnsureWrite();
        readVersion();
    }

    private void readVersion() {
        readBuffer.clear();
        readBuffer.limit(2);
        socket.read(readBuffer, this::parseVersion, ensureRead);
    }

    private void readSelection() {
        readBuffer.clear();
        readBuffer.limit(1);
        socket.read(readBuffer, this::parseSelection, ensureRead);
    }

    private void parseVersion() {
        if (readBuffer.get() != VERSION || readBuffer.get() != VERSION_END) {
            sendStatus(STATUS_INCOMPATIBLE_VERSION, this::closeSafe);
        }
        sendStatus(STATUS_OK, this::addPlayer);
    }

    private void parseSelection() {
        switch (readBuffer.get()) {
            case PATH_LEFT_LEFT:
                selectedPath = WorldPath.LEFT_LEFT;
                break;
            case PATH_LEFT_RIGHT:
                selectedPath = WorldPath.LEFT_RIGHT;
                break;
            case PATH_RIGHT_LEFT:
                selectedPath = WorldPath.RIGHT_LEFT;
                break;
            case PATH_RIGHT_RIGHT:
                selectedPath =  WorldPath.RIGHT_RIGHT;
                break;
            default:
                sendStatus(STATUS_INVALID_SELECTION, this::closeSafe);
                return;
        }
        game.notifyPathSelected();
    }

    private void sendStatus(byte status, Runnable next) {
        writeBuffer.put(status);
        writeBuffer.flip();
        socket.write(writeBuffer, next, ensureWrite);
    }

    private void sendInt(int time, Runnable next) {
        writeBuffer.putInt(time);
        writeBuffer.flip();
        socket.write(writeBuffer, next, ensureWrite);
    }

    private void addPlayer() {
        game.addPlayer(this);
    }

    private void closeSafe() {
        try {
            close();
        } catch (Exception e) {
            System.err.println("Caught exception when closing client connection");
            e.printStackTrace();
        }
    }

    public void onGameReady() {
        selectedPath = null;
        sendStatus(STATUS_AWAITING_SELECTION, this::readSelection);
    }

    public void onTimeCalculated(int time) {
        sendStatus(STATUS_PATH_TIME, () -> sendInt(time, () -> {}));
    }

    @Override
    public void close() throws Exception {
        socket.close();
        game.removePlayer(this);
    }

    private abstract static class AbstractHandler<A> implements CompletionHandler<Integer, A> {
        protected final ByteBuffer buffer;
        private final String message;

        private AbstractHandler(ByteBuffer buffer, String message) {
            this.buffer = buffer;
            this.message = message;
        }

        @Override
        public void failed(Throwable throwable, A a) {
            System.err.println(message);
            throwable.printStackTrace();
        }
    }

    private class EnsureRead extends AbstractHandler<Runnable> {
        private EnsureRead() {
            super(readBuffer, "Failed to read from the client socket");
        }

        @Override
        public void completed(Integer bytes, Runnable attachment) {
            if (buffer.position() != buffer.position()) {
                socket.read(buffer, attachment, this);
                return;
            }
            buffer.flip();
            attachment.run();
        }
    }

    private class EnsureWrite extends AbstractHandler<Runnable> {
        private EnsureWrite() {
            super(writeBuffer, "Failed to write to the client socket");
        }

        @Override
        public void completed(Integer bytes, Runnable attachment) {
            if (buffer.position() != buffer.limit()) {
                socket.write(buffer, attachment, this);
                return;
            }
            buffer.clear();
            attachment.run();
        }
    }
}
