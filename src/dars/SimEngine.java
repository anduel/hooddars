package dars;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import dars.NodeStore;
import dars.event.DARSEvent;
import dars.proto.*;
import dars.proto.aodv.Aodv;
import java.lang.Thread;

/**
 * @author Kenny
 * 
 */
public class SimEngine implements InputConsumer {
  /**
   * Time to wait for an iteration.
   */
  private int              WAIT_TIME    = 10;
  private boolean          KILL_THREAD  = false;
  NodeStore                store        = new NodeStore();
  Queue<Message>           messageQueue = new LinkedList<Message>();        
  MessageRelay             thread       = new MessageRelay();
  static public Object     lock         = new Object();
  private volatile boolean paused;

  /**
   * Function that will start a simulation
   * 
   * This function will be the controlling function for all of the nodes
   * 
   * @author kennylmay
   * 
   * @param
   */
  void runSimulation() {
    if (thread.isAlive() == false) {
      thread.start();
    }
  }

  /**
   * Function that sets the timer speed
   * 
   * This method will allow the adjustment of the time interval(time between
   * clock ticks) in seconds.
   * 
   * 
   * @author kennylmay
   * 
   * @param speed
   *          (int) The time in seconds that a simulation should pause between
   *          ticks.
   */
  void setSimSpeed(int speed) {
    WAIT_TIME = speed;
  }

  /**
   * Function that will pause a simulation
   * 
   * This function will pause the simulation until the user decides to continue
   * 
   * @author kennylmay
   * 
   * @param
   */
  void pauseSimulation() {
      paused = true;
  }

  /**
   * Function that will resume a simulation
   * 
   * This function will resume the simulation when the user chooses to continue
   * 
   * @author kennylmay
   * 
   * @param
   */
  void resumeSimulation() {
      paused = false;
  }

  /**
   * Function that will stop a simulation
   * 
   * This function will stop a simulation indefinitely.
   * 
   * @author kennylmay
   * 
   * @param
   */
  void stopSimulation() {
    KILL_THREAD = true;
  }

  NodeStore getNodeStore() {
    return store;
  }

  /**
   * Function that returns the timer speed
   * 
   * This method will return the wait time.
   * 
   * @author kennylmay
   * 
   * @param
   */
  public int getSimSpeed() {
    return WAIT_TIME;
  }

  class MessageRelay extends Thread {
    int iterationCount = 0;

    public void run() {
      // Make sure the kill switch hasn't been thrown.
      while (KILL_THREAD == false) {

        iterationCount++;
        // Only attempt to enter the critical area every 100th try
        if (iterationCount == 100) {
          // Reset the iterationCount after the 100th try
          iterationCount = 0;
          if (paused == false) {
            // Enter the critical area for the simulation
            // ////////////////////////////////////////////////////
            synchronized (lock) {
              MainLoop();
            }
          }
        }
        try {
          Thread.sleep(WAIT_TIME);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  public void MainLoop() {
    Node node = null;
    Message message = null;
    Iterator<Node> i;
    
    // If there are messages in the messageQueue try to attempt delivery.
    while (messageQueue.isEmpty() == false) {
      message = messageQueue.poll();

      // If the message is a broadcast then try to send to everyone
      if (message.destinationId == Message.BCAST_STRING) {
        i = store.getNodes();
        while(i.hasNext()) {
          node = i.next();
          if (node == null)
            continue;
          // Only allow the nodes in range to hear the broadcast.
          if (canCommunicate(message.originId, node.getAttributes().id) && message.originId != node.getAttributes().id) {
            node.messageToNode(message);
          }
        }
        // Else if the messageQueue is not a broadcast try to send it to the
        // destination id.
      } else {
        if (canCommunicate(message.originId, message.destinationId)) {
          node.messageToNode(message);
        }
      }
    }

    // Issue a clock tick to each node so that they can make algorithmic
    // decisions.
    i = store.getNodes();
    while(i.hasNext()) {
      // / Issue a clock tick to each node
      node = i.next();
      if (node == null)
        continue;
      node.clockTick();
    }

    // Check each node for messages waiting to be sent and gather them up
    // to be stored in our message queue.
    i = store.getNodes();
    while(i.hasNext()) {
      node = i.next();
      // Gather all the messages from each node.
      while ((message = node.messageToNetwork()) != null) {
        messageQueue.add(message);
      }
    }
  }

  /**
   * This function will provide a way to determine the type of even that is
   * issued and make a decision as to what to do with the event.
   * 
   * @ author kennylmay
   * 
   * @param DARSEvnt
   * 
   */
  @Override
  public void consumeInput(DARSEvent e) {
    Node n;
    
    // Enter critical area
    synchronized (lock) {
      switch(e.eventType){
      case IN_START_SIM :
        runSimulation();
        OutputHandler.dispatch(DARSEvent.outStartSim());
        break;
      
      case IN_STOP_SIM:
        stopSimulation();
        OutputHandler.dispatch(DARSEvent.outStopSim());
        break;
        
      case IN_PAUSE_SIM:
        pauseSimulation();
        OutputHandler.dispatch(DARSEvent.outPauseSim());
        break;
        
      case IN_RESUME_SIM:
        resumeSimulation();
        OutputHandler.dispatch(DARSEvent.outResumeSim());
        break;
        
      case IN_SIM_SPEED:
        WAIT_TIME = e.newSimSpeed;
        OutputHandler.dispatch(DARSEvent.outSimSpeed(WAIT_TIME));
        break;
        
      case IN_ADD_NODE:
     // Get the node attributes for this input event
        NodeAttributes ni = e.getNodeAttributes();

        // Assign an ID to the node
        ni.id = assignNodeId();

        // Make a new network node with these attributes
        n = makeNetworkNode(ni);

        // Add it to the node store
        store.addNode(n);

        // Dispatch an output event indicating a new node has entered
        // the network.
        OutputHandler.dispatch(DARSEvent.outAddNode(ni));
      	break;
      	
      case IN_DEL_NODE:
        store.deleteNode(e.nodeId);
        OutputHandler.dispatch(DARSEvent.outDeleteNode(e.nodeId));
        break;
        
      case IN_SET_NODE_RANGE:
        // Get the node
        n = store.getNode(e.nodeId);
        
        // Set the new range
        n.setRange(e.nodeRange);
        OutputHandler.dispatch(DARSEvent.outSetNodeRange(e.nodeId, e.nodeRange));
        break;
        
      case IN_MOVE_NODE:
        // Get the node
        n = store.getNode(e.nodeId);
        
        // Set the new coords
        n.setXY(e.nodeX, e.nodeY);
        
        // Dispatch the moved event
        OutputHandler.dispatch(DARSEvent.outMoveNode(e.nodeId, e.nodeX, e.nodeY));
        break;
        
      }
    } // / Exit critical area
  }

  public enum NodeType {
    AODV, DSDV
  };

  private NodeType nodeType = NodeType.AODV;

  public NodeType getNodeType() {
    return nodeType;
  }

  public void setNodeType(NodeType nt) {
    nodeType = nt;
  }

  public Node makeNetworkNode(NodeAttributes na) {
    // Make the network node based on what type of node is set
    Node n = null;
    switch (getNodeType()) {
    case AODV:
      n = new Aodv();
      break;

    case DSDV:
      // TODO implement DSDV
      n = null;
      break;
    }

    assert (n != null);

    // Set the node attributes
    n.setAttributes(na);

    return n;
  }

  /**
   * assignNodeId method.
   * 
   * Assigns a new node id. It uses the private variable currId to keep track of
   * the next id. The assignment sequence is as follows:
   * 
   * A......Z AA....AZ BA....BZ ........ AAA..AAZ ABA..ABZ ........
   * 
   * The algorithm used is a modified version of the convert decimal to hex
   * algorithm (or any other digit). It cheats a bit because there is no "zero"
   * digit in the ID assigning scheme (Just A-Z).
   * 
   * @ author Mike
   * 
   */
  private String assignNodeId() {
    // Assign a three character ID from A-Z
    //
    int charA = (int) 'A';
    int totalChars = 26;
    String ret = "";

    int remainder;
    int quotient = currId;
    int count = 0;
    while (quotient != 0) {

      // Divide the digit by our alphabet size. The remainder is the digit for
      // this place.
      remainder = quotient % totalChars;
      quotient = quotient / totalChars;

      // Convert the digit to its representation (A-Z)

      // If count is > 0, Cheat and decrement it by one.
      char c;
      if (count > 0) {
        c = (char) (remainder + charA - 1);
      } else {
        c = (char) (remainder + charA);
      }

      count++;
      // Prepend the return string
      ret = c + ret;
    }

    // increment the id
    currId++;

    // if ret is zero len, this must be the first assignment. Set it to "A".
    if (ret.length() == 0) {
      return "A";
    }

    return ret;
  }

  private int currId = 0;

  /**
   * This method is used for determining if a can send a message to antoher node
   * 
   * @param node1
   * @param node2
   * 
   * @return boolean
   */
  private boolean canCommunicate(String Id1, String Id2) {
    NodeAttributes att1 = store.getNodeAttributes(Id1);
    NodeAttributes att2 = store.getNodeAttributes(Id2);
    Point point1 = new Point(att1.x, att1.y);
    
    double distance = point1.distance(att2.x, att2.y);
    if (distance > att1.range || distance > att2.range) {
      return false;
    } else
      return true;
  }
}