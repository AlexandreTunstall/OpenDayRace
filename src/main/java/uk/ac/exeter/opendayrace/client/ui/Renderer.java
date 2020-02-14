package uk.ac.exeter.opendayrace.client.ui;

import uk.ac.exeter.opendayrace.client.GameState;
import uk.ac.exeter.opendayrace.common.world.Node;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

public class Renderer implements Runnable {
    private final GameState game;

    private JFrame frame;
    private Insets insets;

    // For use exclusively by the renderer thread
    private Graphics2D g;
    private int fw, fh;
    private long[] lastTime = new long[60];
    private int timeIndex;

    private BasicStroke thiccboi = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10);

    private BufferedImage background;

    public Renderer(GameState game) throws IOException {
        this.game = game;
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

        background = ImageIO.read(Renderer.class.getResourceAsStream("/uk/ac/exeter/opendayrace/client/background.png"));
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
            drawPopup();
            drawFPS(time);
        } finally {
            if (rawGraphics != null) {
                rawGraphics.dispose();
            }
        }
        bs.show();
    }

    private void drawBackground() {
        int bw = background.getWidth(), bh = background.getHeight();
        double scalefactor = Math.min(fw / bw, fh / bh);
        double transform = scalefactor - 1;
        double dx = transform * fw / 2, dy = transform * fh / 2, dw = scalefactor * fw, dh = scalefactor * fh;
        drawImage(background, dx, dy, dw, dh);
        for (Node n : game.getWorld().getNodes()) {
            drawNode(n, dx, dy, dw / bw, dh / bh);
        }
    }

    private void drawPopup() {
        String popupText = null;
        switch (game.getState()) {
            case CONNECTING:
                popupText = "Connecting...";
                break;
            case AWAITING_SELECTION:
                popupText = "Hello there.";
                return;
            case WAITING_FOR_PLAYERS:
                popupText = "Waiting for other players...";
                break;
            case DISPLAYING_TIME:
                popupText = "The path you chose took you " + game.getTravelTime() + " minutes";
                break;
        }
        g.setColor(new Color(0, 0, 0, 191));
        g.fill(new Rectangle2D.Double(0, 0, fw, fh));
        g.setColor(new Color(0xb2, 0xc8, 0xd5));
        drawBoundedString(popupText, fw * 3d / 10d, fh * 3d / 10d, fw * 2d / 5d, fh * 2d / 5d);
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

    private void drawNode(Node node, double dx, double dy, double dw, double dh) {
        int radius = 30;
        Shape circle = new Ellipse2D.Double(node.getX1() * dw - radius + dx, node.getY1() * dh - radius + dy, 2.0 * radius, 2.0 * radius);
        Shape circle2 = new Ellipse2D.Double(node.getX2() * dw - radius + dx, node.getY2() * dh - radius + dy, 2.0 * radius, 2.0 * radius);
        g.setColor(Color.WHITE);
        g.fill(circle);
        g.fill(circle2);
        g.setColor(Color.GRAY);
        g.setStroke(thiccboi);
        g.draw(circle);
        g.draw(circle2);
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
