

# Adding a New Protocol #

DARS is designed to easily accept the addition of new routing protocols.  Adding a protocol can be divided into three major steps.

  * Updating the NodeFactory
  * Exteneding the Node Class
  * Creating a Protocol Dialog Box


## Updating the Node Factory ##

Updating the NodeFactory class indicates to DARS that there is an additional protocol.  This automatically adds the new protocol into the GUI as well as giving the simumlation engine access to nodes of the new protocol type.

Update dars/proto/NodeFactory.java

> Step 1: Add an import statement for the new protocol's class.
```
      import dars.proto.newproto.NewProto;
```
> Step 2: Add the protocol name to the NodeType enumeration.
```
      public enum NodeType { AODV, DSDV, NewProto };
```
> Step 3: Handle new node requests for the new protocol type.  Add a case to the node type switch statement.  The new case should call the constructor for the protocol.  The constructor will need to accept a single argument of NodeAttributes type.  This constructor is required as part of the Node abstract base class.
```
      case NewProtoName : return NewProto(na);
```



## Extending the Node Class ##

The Node Class requires that a new protocol implement six functions.  These functions provide interfaces rest of DARS to intereact with a node. The protocol implementation should not call any of these functions.

A list of the functions and a description follows, but more detailed information is included in the source of the Node class implementation file (Node.java).

  * messageToNetwork - This function is used to return a message off the transmit queue of a node and return it for the simulation engine to consume. Effectively this is used to simulate the transmittal of a message into the network.

  * messageToNode - This function is used to deliver a message to a node. The message will be placed into the nodes receive queue effectively the node is receiving the message.

  * newNarrativeMessage - Send a narrative message from one node to another.

  * clockTick - Process an iteration of this node. This will do all the processing for a node's time interval.

  * getNodeDialog - Return a JDialog that will be displayed by the GUI.

  * updateNodeDialog - Update the previously returned JDialog with the latest information for a node that will be showed to the GUI.



## Creating a Protocol Dialog Box ##

Each protocol must provide a dialog box that can be displayed to the user to show information about a given node to the user.  This dialog should extend the standard JDialog class.  The implementation and design of the Protocol Dialog box is left up to the developer that is implementing the protocol.




# Additional Topics #

In the process of impleneting a new protocol in DARS the developer will undoubtably require knowledge of two additional areas of DARS, the Message class and DARSEvents.

## The Message Class ##

The Message Class is the internal representation of a message.  It contains three fields message, destinationId, and originId.

The message field (Message.message) contains the payload of the message.  The payload is normally a pipe, '|', delimited string.  An example of this is the narrative message payload that is used by both AODV and DSDV implementation.  The simulation engine does not examine the message field except to display it to the user.

> NARR Message Format - TYPE|FLAGS|TTL|DESTID|ORIGID|TEXT

The destinationId field is the desired destination node ID string.  The message class also implents a special destination ID string that can be used for broadcast messages.  To send a broadcase message assign destiationId equal to Message.BCAST\_STRING.

The originId field is the ID of the node that is sending the message into the network.

It is important to remember that origin and destination of a given message object are often different than the origin and desired destination of a message payload.


## DARSEvents ##

DARSEvents are the way that DARS communicates from one area of the program to another.  For the puroposes of a protocol implementer a DARSEvent can be thought of as a way to notify the rest of the program of an event.  This will often end up in some form of output to the user.

> Some examples of calling a DARSEvent:
```
   OutputHandler.dispatch(DARSEvent.outDebug("Debug Message"));

   OutputHandler.dispatch(DARSEvent.outMsgRecieved(MsgOrigID, MsgDestID, MsgText));
```

For a protocol implementaion only a subset of the overall list of DARSEvents are relavent.  That subset is listed below with additional information.

  * DARSEvent.outDebug(String DebugText)

> Outputs DebugText into the simulation log.

  * DARSEvent.outError(String ErrorText)

> Outputs ErrorText into the simulation log.

  * DARSEvent.outControlMsgReceived(String NodeID, Message msg)

> Informs the GUI that node NodeID received the protocol control message msg.  This acction will also be logged.

> DARSEvent.outControlMsgReceived should be called anytime a node receives a message that is not a Narrative message.

  * DARSEvent.outControlMsgTransmitted(String NodeID, Message msg)

> Informs the GUI that node NodeID transmitted the protocol control message msg.  This acction will also be logged.

> DARSEvent.outControlMsgTransmitted should be called anytime a node transmits a message that is not a Narrative message.

  * DARSEvent.outNarrMsgReceived(String NodeID, Message msg)

> Informs the GUI that node NodeID received the narrative message msg.  This acction will also be logged.

> DARSEvent.outNarrMsgReceived should be called anytime a node receives a message that is a Narrative message.

  * DARSEvent.outNarrMsgTransmitted(String NodeID, Message msg)

> Informs the GUI that node NodeID transmitted the narrative message msg.  This acction will also be logged.

> DARSEvent.outNarrMsgTransmitted should be called anytime a node transmits a message that is not a Narrative message.

  * DARSEvent.outMsgRecieved(MsgOrigID, MsgDestID, MsgText)

> outMsgRecieved is a special DARSEvent that results in a popup box being displayed to the user informing them of a message being received.  This event is generally only used when a Narrative message successfully reaches its final destination.