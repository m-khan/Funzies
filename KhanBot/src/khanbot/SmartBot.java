/**
 * 
 */
package khanbot;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author kHAN_
 *
 */
public class SmartBot extends Bot {

	/**
	 * @param name
	 */
	
	HashMap<String, ArrayList<String>> fullLogs;
	HashMap<String, ArrayList<String>> logs;
	String filename;
	Timer saveTimer;
	static final String STRING_TERMINATE = "9873621687621";
	static final String STRING_STARTER = "1645173426567";
	boolean canSpeak;
	
	
	public SmartBot(String name) {
		super(name);
		
		canSpeak = false;
		filename  = "baselogs.ser";
		logs = new HashMap<String, ArrayList<String>>();
		
		if(!readLogs())
		{
			createNewFile();
		}
		
	}
	
	private void createNewFile()
	{
		fullLogs = new HashMap<String, ArrayList<String>>();
		fullLogs.put(STRING_STARTER, new ArrayList<String>());
		saveLogs();
		System.out.println("Chat logs not found: one has been created.");
		
	}
	
	public void twitchConnect(String password, String channel) throws Exception
	{
		super.twitchConnect(password, channel);
		setFileName(channel);
		saveTimer = new Timer(true);
		saveTimer.schedule(new SaveTimer(this), 120000, 120000);

	}
	
	public void onMessage(String channel, String sender,
            String login, String hostname, String message)
	{
		System.out.println(channel + ": " + sender + ": " + message);
		String words[] = message.split(" "); 
		
		if(message.startsWith("!"))
			processCommand(sender, message, words);
		else
		{
			if(canSpeak)
			{
				respond(channel, words);
			}
			logMessage(words);
		}
	}
	
	private void processCommand(String sender, String message, String[] words)
	{
		if(isAdmin(sender))
		{
			adminCommand(words);
		}
	}
	
	protected void adminCommand(String[] words)
	{
		if(words[0].equals("!join"))
		{
			try
			{
				BotManager.activateSmartBot(words[1]);
			}
			catch (Exception e)
			{
				
			}
		}
		else if(words[0].equalsIgnoreCase("!savechatlogs"))
		{
			saveLogs();
		}
		else if(words[0].equalsIgnoreCase("!stopsaves"))
		{
			saveTimer.cancel();
		}
		else if(words[0].equalsIgnoreCase("!speak"))
		{
			canSpeak = true;
			super.sendMsg(homeChannel, "Hi!");
		}
		else if(words[0].equalsIgnoreCase("!quiet"))
		{
			canSpeak = false;
			super.sendMsg(homeChannel, "/me enters lurk mode");
		}
		else if(words[0].equalsIgnoreCase("!dumplog"))
		{
			printLogs(logs);
		}
		else if(words[0].equalsIgnoreCase("!dumplogs"))
		{
			printLogs(fullLogs);
		}
		else if(words[0].equalsIgnoreCase("!filename"))
		{
			setFileName(words[1]);
		}
	}
	
	public void setFileName(String name)
	{
		saveLogs();
		fullLogs.clear();
		filename = name + ".ser";
		if(!readLogs())
		{
			createNewFile();
		}
	}
	
	private void respond(String channel, String[] words)
	{
		
	}
	private void logMessage(String[] words)
	{
		if(words.length > 30 || words.length < 1)
		{
//			Handle copy past-uh message
			return;
		}
		
		logs.get(STRING_STARTER).add(words[0]);
		for(int i = 0; i < words.length - 1; i++)
		{
			if(logs.containsKey(words[i]))
			{
				ArrayList<String> list = logs.get(words[i]);
				list.add(words[i+1]);
			}
			else
			{
				ArrayList<String> list = new ArrayList<String>();
				list.add(words[i+1]);
				logs.put(words[i], list);
			}
		}
		if(logs.containsKey(words[words.length - 1]))
		{
			ArrayList<String> list = logs.get(words[words.length - 1]);
			list.add(STRING_TERMINATE);
		}
		else
		{
			ArrayList<String> list = new ArrayList<String>();
			list.add(STRING_TERMINATE);
			logs.put(words[words.length - 1], list);
		}
	}
	
	public void printLogs(HashMap<String, ArrayList<String>> log)
	{
		System.out.println("Printing logs: " + log);
		for(String key : log.keySet())
		{
			System.out.println(key + ": " + log.get(key));
		}
	}
	
	public void saveLogs()
    {
		System.out.println("Saving logs....");
		for(String key : logs.keySet())
		{
			if(fullLogs.containsKey(key))
			{
				fullLogs.get(key).addAll(logs.get(key));
			}
			else
			{
				fullLogs.put(key, logs.get(key));
			}
		}
		try
		{
	           FileOutputStream fos =
	              new FileOutputStream(filename, false);
	           ObjectOutputStream oos = new ObjectOutputStream(fos);
	           oos.writeObject(fullLogs);
	           oos.close();
	           fos.close();
	           System.out.println("Serialized chat log data is saved in " + filename);
	    }
		catch(IOException ioe)
	    {
			ioe.printStackTrace();
	    }
		logs.clear();
		
    }	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean readLogs()
	{
	      try
	      {
	         FileInputStream fis = new FileInputStream(filename);
	         ObjectInputStream ois = new ObjectInputStream(fis);
	         fullLogs = (HashMap) ois.readObject();
	         ois.close();
	         fis.close();
	      }catch(IOException ioe)
	      {
	         //ioe.printStackTrace();
	         return false;
	      }catch(ClassNotFoundException c)
	      {
	         System.out.println("Class not found");
	         c.printStackTrace();
	         return false;
	      }
	      return(logs != null);
	}
}


class SaveTimer extends TimerTask
{
	SmartBot master;
	
	public SaveTimer(SmartBot bot)
	{
		master = bot;
	}
	
	public void run()
	{
		master.saveLogs();;
	}
}


