package dars;

import java.awt.Container;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Utilities {

  public static void runSaveLogDialog(Container parent) {
    JFileChooser chooser = new JFileChooser();
    FileNameExtensionFilter filter = new FileNameExtensionFilter(       
        "Log Files", "log");        
        chooser.setFileFilter(filter);  
    int returnVal = chooser.showSaveDialog(parent);
    if (returnVal == JFileChooser.APPROVE_OPTION) {

      // Define the new files to be saved.
      File logFile = new File("darslog.tmp");
      File saveFile = new File(chooser.getSelectedFile().getPath()+".log");

      // Check to see if we will overwrite the file
      if (saveFile.exists()){
      int overwrite = JOptionPane.showConfirmDialog(null, "File already exists, do you want to overwrite?");
        if (overwrite == JOptionPane.CANCEL_OPTION ||
            overwrite == JOptionPane.CLOSED_OPTION ||
            overwrite == JOptionPane.NO_OPTION){
            return;
        }
      }
      
      // Initialize the file readers and writers
      FileReader in = null;
      FileWriter out = null;
       
      // Try to open each file
      try {
        int c;
        in = new FileReader(logFile);
        out = new FileWriter(saveFile);
        // Write each line of the first file to the file chosen.
        while ((c = in.read()) != -1) {
          out.write(c);
        }
        
        // Close both files.
        in.close();
        out.close();

      } catch (FileNotFoundException e1) {
        JOptionPane.showMessageDialog(parent,
            "Log file could not be saved at"
                + chooser.getSelectedFile().getPath());
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
  }
    
    
}
