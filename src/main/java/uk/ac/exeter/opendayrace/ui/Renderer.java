package uk.ac.exeter.opendayrace.ui;

import javax.swing.*;
import java.awt.*;

public class Renderer {
    private JFrame frame;
    private Insets insets;

    public Renderer() {
        frame = new JFrame("Open Day Race");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIgnoreRepaint(true);
        frame.setUndecorated(true);
        frame.setFocusTraversalKeysEnabled(false);
        frame.setSize(640, 480);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.createBufferStrategy(2);

        System.out.println("Buffer capabilities");
        BufferCapabilities bc = frame.getBufferStrategy().getCapabilities();
        System.out.println("\tFlip contents: " + bc.getFlipContents());
        System.out.println("\tFull screen required: " + bc.isFullScreenRequired());
        System.out.println("\tPage flipping: " + bc.isPageFlipping());

        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(frame);

        insets = frame.getInsets();
    }
}
