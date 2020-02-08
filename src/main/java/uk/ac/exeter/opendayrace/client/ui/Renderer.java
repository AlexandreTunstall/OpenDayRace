package uk.ac.exeter.opendayrace.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

public class Renderer implements Runnable {
    private JFrame frame;
    private Insets insets;

    // For use exclusively by the renderer thread
    private Graphics2D g;
    private int fw, fh;
    private long[] lastTime = new long[60];
    private int timeIndex;

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

    @Override
    public void run() {
        long time = System.nanoTime();
        BufferStrategy bs = frame.getBufferStrategy();
        Graphics rawGraphics = null;
        try {
            rawGraphics = bs.getDrawGraphics();
            Container fcp = frame.getContentPane();
            fw = fcp.getWidth();
            fh = fcp.getHeight();
            Graphics2D g = (Graphics2D) rawGraphics.create(insets.left, insets.top, fw, fh);
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            this.g = g;
            drawBackground();
            drawFPS(time);
        } finally {
            if (rawGraphics != null) {
                rawGraphics.dispose();
            }
        }
        bs.show();
    }

    private void drawBackground() {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, fw, fh);
    }

    private void drawFPS(long time) {
        double fps = lastTime.length * 1000000000D / (time - lastTime[timeIndex]);
        lastTime[timeIndex] = time;
        timeIndex = (timeIndex + 1) % lastTime.length;
        g.setColor(Color.RED);
        drawAlignedString("FPS " + (int) fps, fw * .99D, fh * .99D, fh * .02D, BOTTOM_RIGHT);
    }

    private void drawImage(Image img, double x, double y, double w, double h) {
        AffineTransform at = new AffineTransform(w / img.getWidth(null), 0, 0, h / img.getHeight(null),
                x, y);
        g.drawImage(img, at, null);
    }

    private void drawBoundedString(String string, double x, double y, double w, double h) {
        TextLayout layout = new TextLayout(string, g.getFont(), g.getFontRenderContext());
        Rectangle2D bounds = layout.getOutline(null).getBounds2D();
        double bw = bounds.getWidth(), bh = bounds.getHeight();
        double scale = Math.min(w / bw, h / bh);
        double tx = x + (w - bw * scale) / 2D, ty = y + (h - bh * scale) / 2D;
        AffineTransform transform = new AffineTransform(scale, 0D, 0D, scale,
                tx - scale * bounds.getX(), ty - scale * bounds.getY());
        AffineTransform original = g.getTransform();
        g.transform(transform);
        layout.draw(g, 0F, 0F);
        g.setTransform(original);
    }

    private void drawAlignedString(String string, double x, double y, double h, int alignment) {
        TextLayout layout = new TextLayout(string, g.getFont(), g.getFontRenderContext());
        Rectangle2D bounds = layout.getOutline(null).getBounds2D();
        double bw = bounds.getWidth(), bh = bounds.getHeight();
        double scale = h / bh;
        double tx = x - scale * bounds.getX(), ty = y - scale * bounds.getY();
        if ((alignment & TOP_RIGHT) > 0) {
            tx -= scale * bw;
        }
        if ((alignment & BOTTOM_LEFT) > 0) {
            ty -= scale * bh;
        }
        AffineTransform transform = new AffineTransform(scale, 0D, 0D, scale, tx, ty);
        AffineTransform original = g.getTransform();
        g.transform(transform);
        layout.draw(g, 0F, 0F);
        g.setTransform(original);
    }

    protected static final int TOP_LEFT = 0;
    protected static final int TOP_RIGHT = 1;
    protected static final int BOTTOM_LEFT = 2;
    protected static final int BOTTOM_RIGHT = 3;
}
