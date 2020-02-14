/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package uk.ac.exeter.opendayrace.client;

import uk.ac.exeter.opendayrace.client.ui.Renderer;

import java.io.IOException;

public class OpenDayRace {
    public static void main(String[] args) {
        // Enable hardware acceleration
        System.setProperty("sun.java2d.opengl", "true");
        Renderer r = null;
        try {
            r = new Renderer();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        while (true) {
            r.run();
        }
    }
}
