package uk.ac.exeter.opendayrace.client.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static uk.ac.exeter.opendayrace.common.io.NetworkConstants.*;

public class ServerConnection {
    // Must be large enough to store all the bytes that will be handled at the same time
    private static final int BUFFER_SIZE = 4096;

    private final SocketChannel socket;
    private final ByteBuffer buffer;
    private boolean expectTime = false;

    public ServerConnection(InetSocketAddress address) throws IOException {
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
        if (expectTime) {

            expectTime = false;
            return;
        }
        // TODO Set state based on status code
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
