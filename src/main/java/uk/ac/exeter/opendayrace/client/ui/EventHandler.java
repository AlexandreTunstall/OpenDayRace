package uk.ac.exeter.opendayrace.client.ui;

import uk.ac.exeter.opendayrace.client.GameState;
import uk.ac.exeter.opendayrace.common.world.Node;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class EventHandler implements MouseListener, ComponentListener {

    Renderer renderer;
    GameState game;

    public EventHandler(Renderer renderer) {
        this.renderer = renderer;
        this.game = this.renderer.getGame();
        this.renderer.getFrame().addMouseListener(this);
        this.renderer.getFrame().addComponentListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {

    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        if (this.game.getState() == GameState.State.AWAITING_SELECTION) {
            // find closest node and toggle selection
            for (Node node : game.getWorld().getNodes()) {
                double nodeXY[] = this.renderer.mapCoordinates(node.getX1(), node.getY1());
                if (Math.pow(mouseEvent.getX() - nodeXY[0], 2) + Math.pow(mouseEvent.getY() - nodeXY[1], 2) < Math.pow(Renderer.NODE_RADIUS, 2)) {
                    // if adding nodes make sure that only a node connected to a previous node can be selected
                    if (!node.isSelected()) {
                        Node last = this.game.getLastNodeInPath();
                        boolean valid = false;
                        if (last == null) {
                            valid = node.startingNode;
                        } else {
                            for (Node n : last.getConnectedNodes()) {
                                if (n == node) valid = true;
                            }
                        }
                        if (valid) {
                            node.setSelected(true);
                            this.game.addToPath(node);
                            if (node.endingNode) {
                                this.game.setWorldPath(this.game.calculatePath());
                                this.game.setWaitingForPlayers();
                            }
                        }
                    } else {
                        if (node !=  this.game.getLastNodeInPath()) return;
                        node.setSelected(false);
                        this.game.popFromPath();
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    @Override
    public void componentResized(ComponentEvent componentEvent) {

    }

    @Override
    public void componentMoved(ComponentEvent componentEvent) {

    }

    @Override
    public void componentShown(ComponentEvent componentEvent) {

    }

    @Override
    public void componentHidden(ComponentEvent componentEvent) {

    }
}
