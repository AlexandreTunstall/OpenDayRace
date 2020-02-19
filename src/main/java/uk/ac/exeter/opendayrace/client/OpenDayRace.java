/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package uk.ac.exeter.opendayrace.client;

import uk.ac.exeter.opendayrace.client.io.ServerConnection;
import uk.ac.exeter.opendayrace.client.ui.Renderer;

import java.io.IOException;
import java.net.*;

public class OpenDayRace {

    public static void main(String[] args) {
        // Enable hardware acceleration
        System.setProperty("sun.java2d.opengl", "true");
        GameState game = new GameState();
        Renderer r = null;
        try {
            r = new Renderer(game);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        InetAddress address;
        try {
            address = Inet4Address.getByName("144.173.65.27");
        } catch (UnknownHostException e) {
            System.err.println("Fatal exception: could not resolve server address");
            e.printStackTrace();
            return;
        }
        new Thread(() -> runNetwork(game, new InetSocketAddress(address, 53892)), "Network").start();
        while (true) {
            r.run();
        }
    }

    private static void runNetwork(GameState game, SocketAddress address) {
        try {
            new ServerConnection(game, address, e -> {
                System.err.println("IO exception in Network thread");
                e.printStackTrace();
            }).run();
        } catch (IOException e) {
            System.err.println("IO exception when setting up the initial connection; is the server running?");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
