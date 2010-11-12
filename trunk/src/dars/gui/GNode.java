package dars.gui;

import java.awt.*;

import javax.swing.*;

import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Vector;

public class GNode extends JPanel {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static String      newline          = System
                                                 .getProperty("line.separator");

  static public GNode       SelectedNode;

  // /Constructor
  public GNode(String id, int x, int y, int range, JLayeredPane layeredPane) {
    // Copy in the id, coordinates, range
    id_ = id;
    this.layeredPane = layeredPane;
    this.range = range;

    // Setup the default graphic, state
    img_ = ImageFactory.getNodeImg();

    setOpaque(false);

    // Setup the bounds given by x and y, and the size of the node
    setSize(new Dimension(img_.getWidth(null), img_.getHeight(null)));
    setLocation(new Point(x, y));

    // Add the internal mouse listeners
    addMouseListener(new GNodeMouseListener());
    addMouseMotionListener(new GNodeMouseMotionListener());

  }


  public void cleanup() {

  }
 
  private Rectangle r = new Rectangle();
  @Override
  public void paintComponent(Graphics g) {

    // Draw the graphic
    g.drawImage(img_, 0, 0, null);

    // Draw the node id onto the graphic
    r.setRect(9, 4, 17, 12);
    ImageFactory.drawNodeID(g, id_, r);

  }

  // /Functions
  public void addListener(GNodeListener l) {
    this.listeners.add(l);
  }

  public void removeListener(GNodeListener l) {
    this.listeners.remove(l);
  }


  // Select
  public void select() {
    // unselect the currently selected node
    GNode tmp = GNode.SelectedNode;
    if (GNode.SelectedNode != null) {
      GNode.SelectedNode.unselect();
    }

    isSelected = true;
    GNode.SelectedNode = this;
    // System.out.println("selecting a node..");
    this.img_ = ImageFactory.getSelectedNodeImg();

    if (tmp != null) {
      tmp.repaint();
    }

    this.repaint();
    // show the range indicator
    // rangeIndicator.setFill(true);

    // layeredPane.repaint();
  }

  // Unselect
  public void unselect() {
    // System.out.println("unselecting a node..");
    isSelected = false;
    GNode.SelectedNode = null;
    this.img_ = ImageFactory.getNodeImg();

    // hide the range indicator
    // rangeIndicator.setFill(false);
    // layeredPane.repaint();
    this.repaint();

  }

  public void setXY(int x, int y) {
    // Set the new location of the canvas
    setLocation(new Point(x, y));
  }

  public int getRange() {
    return range;
  }
  public void setRange(int range) {
    this.range = range;
  }

  public String getId() {
    return this.id_;
  }

  public Point getCenter() {
    return new Point(getX() + img_.getWidth() / 2, getY() + img_.getHeight()
        / 2);
  }

  private void setEntered(boolean entered) {
    // If the node is selected, don't draw a hover image
    if (isSelected) {
      isEntered = entered;
      return;
    }

    if (entered) {
      isEntered = true;

      // Set entered status
      isEntered = true;

      // Set image to hover image
      img_ = ImageFactory.getHoveredNodeImg();
    }
    // unset hover image
    else {
      img_ = ImageFactory.getNodeImg();
    }
    this.repaint();
  }

  // ID of the node
  private String                      id_;

  private boolean                     isEntered, isSelected;

  // DraggedNode boolean. Will be set true if the node is currently being
  // dragged
  boolean                             isDragged;

  private JLayeredPane                layeredPane;

  private DraggedGNode                draggedGNode;

  private BufferedImage               img_      = null;

  private final Vector<GNodeListener> listeners = new Vector<GNodeListener>();

  private boolean                     isClicked = false;

  private class GNodeMouseListener extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      // Invalidate any other drag notions initially
      isClicked = false;

      // If this is a popup event, notify the handlers and return
      if (e.isPopupTrigger()) {
        // System.out.println("Popup event");
        for (GNodeListener l : listeners) {
          l.nodePopupEvent((GNode) e.getSource(), e.getX(), e.getY());

        }
        return;
      }

      // If it's a non popup event right click, just return out
      if (e.getButton() == MouseEvent.BUTTON3) {
        return;
      }

      // Mark this node as selected
      select();

      // Notify the handlers
      for (GNodeListener l : listeners) {
        l.nodeSelected((GNode) e.getSource());
      }

      isClicked = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (e.isPopupTrigger()) {
        // System.out.println("Popup event");
        for (GNodeListener l : listeners) {
          l.nodePopupEvent((GNode) e.getSource(), e.getX(), e.getY());
        }

        return;
      }

      // If its a right click, return
      if (e.getButton() == MouseEvent.BUTTON3) {
        return;
      }

      // If no node was being dragged, no work to do.
      if (draggedGNode == null) {
        return;
      }

      // Notify the handlers of the node movement
      for (GNodeListener l : listeners) {
        l.nodeMoved((GNode) e.getSource(), draggedGNode.getX(),
            draggedGNode.getY());
      }

      System.out.println("Moving node.");
      // Remove the dragged node.
      draggedGNode.cleanup();
      draggedGNode = null;
      isClicked = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      // Set state to entered
      setEntered(true);

      // invalidate the parent container

      // Notify the handlers
      for (GNodeListener l : listeners) {
        l.nodeEntered((GNode) e.getSource());
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      // Set state to not entered
      setEntered(false);

      // Notify the handler
      for (GNodeListener l : listeners) {
        l.nodeExited((GNode) e.getSource());
      }
    }
  }

  private class GNodeMouseMotionListener extends MouseMotionAdapter {
    @Override
    public void mouseDragged(MouseEvent e) {

      // If its a right click, return
      if (!isClicked) {
        return;
      }

      // System.out.println("Node dragged");
      // If this is the first time through, create a new dragged node.
      if (draggedGNode == null) {
        draggedGNode = new DraggedGNode((GNode) e.getSource());
        // System.out.println("Adding new Dragged Node");
      }

      // System.out.printf("Mouse X: %d Y: %d", e.getX(), e.getY());
      // Update the dragged node's position.
      draggedGNode.moveXYOffset(e.getX(), e.getY());

    }
  }

  private class DraggedGNode extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    DraggedGNode(GNode parent) {
      // Copy in attributes
      parent_ = parent;

      // Setup the ghosted node graphic
      img_ = ImageFactory.getGhostedNodeImg();

      // Set the initial coordinates
      moveXYOffset(parent.getX(), parent.getY());
      setSize(new Dimension(img_.getWidth(null), img_.getHeight(null)));

      // Add this canvas to the parental container at the popup level
      parent.layeredPane.add(this, JLayeredPane.POPUP_LAYER);

      setOpaque(false);
      // System.out.println("Creating new drag node..");
    }

    @Override
    public void paintComponent(Graphics g) {
      // Draw the graphic
      g.drawImage(img_, 0, 0, null);

    }

    public void moveXYOffset(int x, int y) {
      int xOffset = parent_.getX() + x - img_.getWidth(null) / 2;
      int yOffset = parent_.getY() + y - img_.getHeight(null) / 2;
      setLocation(xOffset, yOffset);
    }

    public void cleanup() {
      // Remove this canvas from the parent container
      layeredPane.remove(this);
      layeredPane.repaint();

    }

    private BufferedImage img_;

    private GNode         parent_;

  }


  private int range;

}
