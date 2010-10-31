/**
 * 
 */
package dars;

import logger.Logger;
import dars.event.DARSEvent;
import dars.gui.GUI;

/**
 * @author Mike
 * 
 */
public class DARSMain {

  /**
   * @param args
   */
  public static void main(String[] args) {
    // Setup the logger to consume DARSEvents from both the input handler and
    // the output handler. From henceforth, all DARSEvents that pass through
    // the Input and Output handlers will be logged.
    InputHandler.addInputConsumer(Logger.getInstance());
    OutputHandler.addOutputConsumer(Logger.getInstance());

    // Instantiate the simulator engine
    SimEngine s = new SimEngine();

    // Name the simulator engine as an input consumer
    // TODO: Have the simulator engine class implement the InputConsumer
    // interface.
     InputHandler.addInputConsumer(s);

    // Instantiate the gui
    GUI g = new GUI();

    // Setup the node inpsector for the gui. This gives the gui a backdoor into the 
    // simulation, where it can view node attributes
    g.setNodeInspector(s.getNodeStore());
    
    
    // Name the GUI as an output consumer
    OutputHandler.addOutputConsumer(g);
    
    for(int i =0; i<30; i++)
      OutputHandler.dispatch(DARSEvent.outDebug("HOLY COW!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"));

  }

}