package dars;

import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.JDialog;

import dars.event.DARSEvent;
import dars.proto.Node;
import dars.proto.NodeFactory;
import dars.proto.NodeFactory.NodeType;

/**
 * @author Kenny
 * 
 */
public class SimEngine implements InputConsumer, SimulationTimeKeeper, NodeInspector {
  /**
   * Time to wait for an iteration.
   */
  private int                 WAIT_TIME    = 10;
  private boolean             KILL_THREAD  = false;
  NodeStore                   store        = new NodeStore();
  Queue<Message>              messageQueue = new LinkedList<Message>();
  Queue<Message>              newMessages  = new LinkedList<Message>();
  MessageRelay                thread       = new MessageRelay();
  static public Object        lock         = new Object();
  private volatile boolean    paused,throwPause;
  private volatile long       simTime      = 0;

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
      KILL_THREAD = false;
      thread = new MessageRelay();
      simTime = 0;
      thread.start();
      
    }
  }
  
  @Override
  public long getTime() {
    return simTime;
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
      throwPause = true;
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
        
        if(throwPause) {
          OutputHandler.dispatch(DARSEvent.outPauseSim());
          throwPause = false;
          paused = true;
        }
        // Only attempt to enter the critical area every 100th try
        if (iterationCount == 100) {
          // Reset the iterationCount after the 100th try
          iterationCount = 0;

          if (paused == false) {
            
            //Increment sim time
            simTime++;
            
            //Begin a new quantum
            OutputHandler.dispatch(DARSEvent.outQuantumElapsed());
            
            //Check if the kill switch was thrown as a result of the event
            if(KILL_THREAD) {
              break;
            }
            
            // Enter the critical area for the simulation
            //////////////////////////////////////////////////////
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
    Iterator<Message> mi;
   
    // If there are any messages in the newMessage Q, introduce them
    // into the network.
    mi = newMessages.iterator();
    while(mi.hasNext()) {
      //Get the ref to the message
      Message m = mi.next();
      
      //Delete it from the Q
      mi.remove();
      
      //Get the node
      Node n = store.getNode(m.originId);
      if(n == null) {
        continue;
        
      }
      //Introduce the message into the network
      n.newNarrativeMessage(m.originId, m.destinationId, m.message);
    }
    
    
    // If there are messages in the messageQueue try to attempt delivery.
    while (messageQueue.isEmpty() == false) {
      message = messageQueue.poll();

      // If the message is a broadcast then try to send to everyone
      if (message.destinationId == Message.BCAST_STRING) {  
        
        i = store.getNodes();
        while(i.hasNext()) {
          node = i.next();
          //if the node is null, it was deleted, continue
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
          // SAK - Send the message to the destination node.
          store.getNode(message.destinationId).messageToNode(message);
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
        //Set the speed
        setSimSpeed(e.newSimSpeed);
        runSimulation();
        OutputHandler.dispatch(DARSEvent.outStartSim(e.newSimSpeed));
        break;
      
      case IN_STOP_SIM:
        stopSimulation();
        OutputHandler.dispatch(DARSEvent.outStopSim());
        break;
        
      case IN_PAUSE_SIM:
        pauseSimulation();
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
        String id = assignNodeId();

        // Make a new network node with these attributes
        ni = new NodeAttributes(id, ni.x, ni.y, ni.range, ni.isPromiscuous);
        n = NodeFactory.makeNewNode(getNodeType(), ni);

        // Add it to the node store
        store.addNode(n);

        // Dispatch an output event indicating a new node has entered
        // the network.
        OutputHandler.dispatch(DARSEvent.outAddNode(ni));
      	break;
      	
      case IN_DEL_NODE:
        if(store.deleteNode(e.nodeId)) { 
          OutputHandler.dispatch(DARSEvent.outDeleteNode(e.nodeId));
        }
        else {
          OutputHandler.dispatch(DARSEvent.outError("Could not delete node " + e.nodeId + ", node does not exist"));
        }
        break;
        
      case IN_SET_NODE_RANGE:
        // Get the node
        n = store.getNode(e.nodeId);
        if(n==null) {
          OutputHandler.dispatch(DARSEvent.outError("Could not set range for node " + e.nodeId + ", node does not exist"));
          return;
        }
        
        // Set the new range
        n.setRange(e.nodeRange);
        OutputHandler.dispatch(DARSEvent.outSetNodeRange(e.nodeId, e.nodeRange));
        break;
        
      case IN_SET_NODE_PROMISCUITY: 
        //Get the node
        n = store.getNode(e.nodeId);
        if(n == null) {
          OutputHandler.dispatch(DARSEvent.outError("Could not set promsicuity for node " + e.nodeId + ", node does not exist"));
          return;
        }
        // Set the new promiscuity level
        n.setPromiscuity(e.isPromiscuous);
        OutputHandler.dispatch(DARSEvent.outSetNodePromiscuity(e.nodeId, e.isPromiscuous));
        break;
        
      case IN_CLEAR_SIM:
        //Clear the simulation
        clearSim();
        
        //Indicate to output consumers that the simulation
        //has been cleared.
        OutputHandler.dispatch(DARSEvent.outClearSim());
        break;
        
      case IN_NEW_SIM:
        //Clear the simulation
        clearSim();
        
        //Reset the current quantum
        simTime = 0;
        
        //Set the sim type
        setNodeType(e.nodeType);
        
        
        //Indicate to output consumers that 
        //a new sim has begun
        OutputHandler.dispatch(
            DARSEvent.outNewSim(e.nodeType));
        break;
        
      case IN_MOVE_NODE:
        // Get the node
        n = store.getNode(e.nodeId);
        
        if(n == null) {
          OutputHandler.dispatch(DARSEvent.outError("Could not move node " + e.nodeId + ", node does not exist"));
          return;
        }
        
        // Set the new coords
        n.setXY(e.nodeX, e.nodeY);
        
        // Dispatch the moved event
        OutputHandler.dispatch(DARSEvent.outMoveNode(e.nodeId, e.nodeX, e.nodeY));
        break;
        
      case IN_INSERT_MESSAGE:
        // Check if the source node exists
        if(store.getNode(e.sourceId) == null) {
          OutputHandler.dispatch(DARSEvent.outError("Could not insert a new message into the network, originating node " + e.nodeId + " does not exist"));
        }
        
        // Add the message to the newMessages Q
        Message m = new Message(e.destinationId,e.sourceId, e.transmittedMessage);
        newMessages.add(m);
        
        // Dispatch the insert message event
        OutputHandler.dispatch(DARSEvent.outInsertMessage(e.sourceId, e.destinationId, e.transmittedMessage));
        
        break;
      }
    } // / Exit critical area
  }

  private NodeType nodeType = null;

  public NodeType getNodeType() {
    return nodeType;
  }

  public void setNodeType(NodeType nt) {
    nodeType = nt;
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
    
    // increment the id
    currId++;
    
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

      char c;
      c = (char) (remainder + charA);

      count++;
      // Prepend the return string
      ret = c + ret;
    }
    
    //decrement the first char of the ID
    char f = ret.charAt(0);
    f--;
    ret = f + ret.substring(1);
    


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
    private Point point1 = new Point();
    private boolean canCommunicate(String OriginID, String DestinationID) {
    NodeAttributes originAtt = store.getNodeAttributes(OriginID);
    NodeAttributes destinationAtt = store.getNodeAttributes(DestinationID);
    
    //Nodes that don't exist can't communicate; return false.
    if(originAtt == null || destinationAtt == null ) {
      return false;
    }
    point1.x = originAtt.x;
    point1.y = originAtt.y;
    
    double distance = point1.distance(destinationAtt.x, destinationAtt.y);
    if (distance > originAtt.range || distance > destinationAtt.range) {
      return false;
    } else
      return true;
  }
  
  private void clearSim() {
    // remove all nodes from the node Store
    store.clear(); 
    
    //remove all messages from the queues
    messageQueue.clear();
    newMessages.clear();
    
    //Reset the node ID assigning sequence
    this.currId = 0;
  }
  
  // Fulfills the "Node Inspector" contract.
  public JDialog getNodeDialog(String nodeId) {
    synchronized (lock) {
      Node node = store.getNode(nodeId);
      if (node == null) {
        return null;
      }
      return node.getNodeDialog();
    }
  }
  // Fulfills the "Node Inspector" contract.
  public void updateNodeDialog(String nodeId, JDialog dialog) {
    synchronized (lock) {

      Node node = store.getNode(nodeId);
      if (node == null) {
        return;
      }
      node.updateNodeDialog(dialog);
    }
  }

  // Fulfills the "Node Inspector" contract.
  public NodeAttributes getNodeAttributes(String nodeId) {
    synchronized (lock) {
      Node node = store.getNode(nodeId);

      if (node == null) {
        return null;
      }
      return node.getAttributes();
    }
  }

}