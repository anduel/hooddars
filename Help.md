

# DARS (Dynamic Ad-Hoc Routing Simulator v 1.0) #

## 1.0 Setting Up A Simulation ##

  * Create a new simulation by selecting Simulation->New->AODV. This will should unlock the canvas and other buttons such as Play. From here you may begin by adding and moving nodes around the canvas.

  * See section 2.0 for adding and moving nodes.


## 2.0 Node Management ##

> ### 2.1 Adding Nodes ###

  * Nodes can be added to a canvas by right clicking on part of the blank canvas and clicking "Add Node". This will add a node at the coordinates of the mouse pointer with default parameters.

  * Nodes can also be adding be selecting Simulation->Randomize. This will as you for the number of nodes you would like to add.  This will then populate the canvas with these nodes with random but valid parameters.

  * This Operation can be performed before and during a simulation.

> ### 2.2 Deleting Nodes ###

  * Nodes can be deleted by right clicking on the desired node and selecting "Delete Node".

  * If you wish to clear all the nodes from the simulation you may select Simulation->Clear.

  * his Operation can be performed before and during a simulation.


> ### 2.3 Moving Nodes ###

  * Nodes can be moved by left clicking on the desired node and dragging it to the new location.

  * Nodes can be moved to a specific X,Y coordinate by left clicking on the desired node and manually changing the value in the bottom right hand corner's node attribute window.

  * This Operation can be performed before and during a simulation.


> ### 2.4 Node Attributes ###

  * A nodes detailed attributes can be viewed by left clicking on the desired node and then left clicking the attributes bottom in the bottom left hand corer of the screen.

  * The node attributes window can be viewed simultaneously with the simulation and other attributes windows.

  * This Operation can be performed before and during a simulation.


## 3.0 Playing A Simulation ##

> ### 3.1 Playing A New Simulation ###

  * After adjusting the nodes to their desired states click play on the
> > menu bar to start the simulation

  * The simulation will continue to run until it is paused or stopped.


> ### 3.2 Playing Previous Simulation ###

  * If you have a saved simulation you may click Simulation->Import->Replay to load a previously run setup.  After the setup has been loaded you will be prompted that the load is complete and you may click play to continue.

  * The simulation can be played as usual. If untouched the simulation will continue to run just as it had previously. If the simulation is edited it will attempt to follow the previous replay as closely as possible with the new changes taken into account.

## 4.0 Saving A Simulation ##

  * A simulation can be saved at any point before, during, or after the simulation has been run.  This can be accomplished by selecting Simulation->Save.  This will ask you for the file location in which to save the file and if it should be overwritten or not.

## 5.0 Loading A Simulation ##

> ### 5.1 Loading a Replay ###

  * If you have a saved simulation you may click Simulation->Import->Replay to load a previously run setup.  After the setup has been loaded you will be prompted that the load is complete and you may click play to continue.

  * The simulation can be played as usual. If untouched the simulation will continue to run just as it had previously. If the simulation is edited it will attempt to follow the previous replay as closely as possible with the new changes taken into account.

  * A setup can be loaded from any previously saved simulation.

  * The simulation will continue to run until it is paused or stopped.


> ### 5.2 Loading a Setup ###

  * If you have a saved simulation you may click Simulation->Import->Setup to load a previously run setup.

  * This action will load the previous simulation setup only. Any actions taken after the play button was pressed in the setup file will not be used.

  * A setup can be loaded from any previously saved simulation.

  * The simulation will continue to run until it is paused or stopped.


## 6.0 Sending a Message ##

  * A narritave message can be sent from one node to another by right clicking on a node and clicking send message. This will display a popup box asking for the destination node and a text area with the message to send.

  * The simulator will attempt delivery of the message based on the routing protocol selected.

  * A message can be added to a node before or during a simulation.

## 7.0 AODV Notes ##

## 8.0 DSDV Notes ##

> [Resources](http://code.google.com/p/hooddars/wiki/DSDV)

## 9.0 Programmer Notes ##

> [Adding A New Protocol](http://code.google.com/p/hooddars/wiki/AddProto)
