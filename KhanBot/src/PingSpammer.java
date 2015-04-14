
import java.awt.AWTException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PingSpammer
{
	
  public static void main(String args[]) 
  throws IOException, AWTException, InterruptedException
  {
	Thread.sleep(2000);
	  // create the ping command as a list of strings
    PingSpammer ping = new PingSpammer();
    List<String> commands = new ArrayList<String>();
    commands.add("ping");
    commands.add("-t");
    commands.add("8.8.8.8");
    ping.doCommand(commands);
    
  }

  public void doCommand(List<String> command) 
  throws IOException, AWTException
  {
	  
    Keyboard keyboard = new Keyboard();

    String s = null;

    ProcessBuilder pb = new ProcessBuilder(command);
    Process process = pb.start();

    BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
    BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

    int count = 0;
    
    // read the output from the command
    while ((s = stdInput.readLine()) != null)
    {
      keyboard.type(count++ + " - ");
      keyboard.type(s + "\n");
    }

    // read any errors from the attempted command
    while ((s = stdError.readLine()) != null)
    {
      keyboard.type(s + "\n");
    }
  }

}