package uk.ac.exeter.opendayrace.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicInteger;

public class Server implements AutoCloseable, CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {
    private static final int DEFAULT_PORT = 53892;  // Mashed keyboard
    private static final int SERVER_THREADS = 50;

    private final AsynchronousServerSocketChannel socket;
    private final GameManager game;

    public Server(AsynchronousServerSocketChannel socket) {
        this.socket = socket;
        game = new GameManager();
        System.out.println("Waiting for connections");
        new Thread(game, "Game").start();
        queue(socket);
    }

    public static void main(String[] args) {
        InetAddress address = null;
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            if (args.length > 2) {
                System.err.println(args.length + " arguments were specified, but only 2 are supported (the rest will be ignored)");
            }
            try {
                address = InetAddress.getByName(args[0]);
            } catch (UnknownHostException e) {
                System.err.println("Failed to parse the given address");
                System.err.println("Usage: [address] [port]");
                return;
            }
            if (args.length > 1) {
                try {
                    port = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse the given port number");
                    System.err.println("Usage: [address] [port]");
                    return;
                }
            }
        }
        if (address == null) {
            try {
                address = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                System.err.println("Failed to get the any address, please try again or specify the address manually");
                e.printStackTrace();
                return;
            }
        }
        try {
            final AtomicInteger nextID = new AtomicInteger(0);
            AsynchronousChannelGroup group = AsynchronousChannelGroup.withFixedThreadPool(SERVER_THREADS,
                    r -> new Thread(r, "Server-" + nextID.getAndIncrement()));
            new Server(AsynchronousServerSocketChannel.open(group)
                    .bind(new InetSocketAddress(address, port), SERVER_THREADS));
        } catch (IOException e) {
            System.err.println("Failed to start server");
            e.printStackTrace();
        }
        // This thread can now be used for reading console commands and responding appropriately
    }

    private void queue(AsynchronousServerSocketChannel socket) {
        socket.accept(socket, this);
    }

    @Override
    public void completed(AsynchronousSocketChannel result, AsynchronousServerSocketChannel attachment) {
        System.out.println("Client connected");
        new ClientConnection(game, result);
        queue(attachment);
    }

    @Override
    public void failed(Throwable throwable, AsynchronousServerSocketChannel attachment) {
        System.err.println("Server socket failed to accept connection");
        throwable.printStackTrace();
    }

    @Override
    public void close() throws Exception {
        socket.close();
    }
}
