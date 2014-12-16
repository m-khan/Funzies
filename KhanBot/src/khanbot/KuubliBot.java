package khanbot;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.collections4.CollectionUtils;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

public class KuubliBot extends PircBot
{
	private ArrayList<String> mods;
	private static final SimpleDateFormat dateF = new SimpleDateFormat("MM/dd HH:mm:ss");
	String homeChannel;
	int chatDelay;
	String oAuth;
	boolean canSpeak;
	HashMap<String, ArrayList<String>[]> tempLogs;
	HashMap<String, ArrayList<String>[]> fullLogs;
	String filename;
	int messageCount;
	static final String STRING_TERMINATE = "<<TERM>>";
	static final String STRING_STARTER = "<<START>>";
	static final int BEFORE = 0;
	static final int AFTER = 1;
	
	@SuppressWarnings("unchecked")
	public KuubliBot(String name, String channel)
	{
		this.setName(name);
		mods = new ArrayList<String>();
		homeChannel = channel;
		chatDelay = 3000;
		filename = "kuublifile.ser";
		messageCount = 0;
		canSpeak = false;
		tempLogs = new HashMap<String, ArrayList<String>[]>();
		fullLogs = readLogs();
		if (fullLogs == null)
		{
			fullLogs = new HashMap<String, ArrayList<String>[]>();
		}
		
	}
	
	public void twitchConnect(String password, String channel) throws Exception
	{
		this.connect("irc.twitch.tv", 6667, password);		
		homeChannel = "#" + channel;
		this.joinChannel(homeChannel);
		oAuth = password;
	}
	
	public void sendMsg(String channel, String message)
	{
		try {
		    Thread.sleep(chatDelay);
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
		System.out.println("kuubli: " + message);
		sendMessage(channel, message);

	}
	
	public void onMessage(String channel, String sender,
            String login, String hostname, String message)
	{
		String words[] = message.split(" "); 
		
		if(message.startsWith("!"))
		{
			processCommand(channel, sender, message, words);
		}
		else if(!message.contains("http://"))
		{
			//System.out.println(sender + ": " + message);
			logMessage(words);
			if(canSpeak && message.toLowerCase().contains("kuubli"))
			{
				updateLogs();
				System.out.println(sender + ": " + message);
				String response = "";
				if(words.length == 1)
				{
					response = getRandomResponse();
				}
				response = getResponse(words, words.length, words.length * 2);
				sendMsg(homeChannel, "@" + sender + ": " + response);
			}
		}
	}
	
	private String getRandomResponse()
	{
		List<String> keySet = new ArrayList<String>(fullLogs.keySet());
		Random r = new Random();
		for (int i = 0; i < 50; i++) {
			String key = keySet.get(r.nextInt(keySet.size()));
			ArrayList<String>[] lists = fullLogs.get(key);
			if(lists[0].contains(STRING_STARTER) && lists[1].contains(STRING_TERMINATE))
			{
				return key;
			}
		}
		return "HassanChop";
		
	}
	
	private String getResponse(String[] words, int beforeCount, int afterCount)
	{
		String seedWord = getUniqueWord(words);
		
		ArrayList<String> beforeList = fullLogs.get(seedWord)[BEFORE];
		ArrayList<String> afterList = fullLogs.get(seedWord)[AFTER];
		
		Collection<String> cross = CollectionUtils.intersection(beforeList, afterList);
		if(cross.size() > 0)
		{
			String[] crossWords = new String[cross.size()];
			crossWords = cross.toArray(crossWords);
			seedWord = getUniqueWord(crossWords);
		}
		System.out.println("SEED:" + seedWord);
		
		return respondHelpBefore(beforeList, beforeCount) + " " + seedWord + " "+ respondHelpAfter(afterList, afterCount);
	}
	
	private String respondHelpBefore(ArrayList<String> wordList, int tryCount)
	{
		Random r = new Random();
		String toReturn = wordList.get(r.nextInt(wordList.size()));
		while(tryCount-- > 0 && toReturn.compareTo(STRING_STARTER) == 0)
		{
			toReturn = wordList.get(r.nextInt(wordList.size()));
		}
		if(toReturn.compareTo(STRING_STARTER) == 0 || tryCount < 0)
		{
			return "";
		}
		else
		{
//			System.out.println(tryCount + ": Processing words before: " + toReturn);
			wordList = fullLogs.get(toReturn)[BEFORE];
			return respondHelpBefore(wordList, tryCount - 1) + " " + toReturn;
		}
		
	}
	
	private String respondHelpAfter(ArrayList<String> wordList, int tryCount)
	{
		Random r = new Random();
		String toReturn = wordList.get(r.nextInt(wordList.size()));
		while(tryCount-- > 0 && toReturn.compareTo(STRING_TERMINATE) == 0)
		{
			toReturn = wordList.get(r.nextInt(wordList.size()));
		}
		if(toReturn.compareTo(STRING_TERMINATE) == 0 || ( tryCount < 0 && fullLogs.get(toReturn)[1].contains(STRING_TERMINATE)))
		{
			return "";
		}
		else
		{
//			System.out.println(tryCount + ": Processing words after: " + toReturn);
			wordList = fullLogs.get(toReturn)[AFTER];
			return toReturn + " " + respondHelpAfter(wordList, tryCount - 1);
		}
	}
	
	private String getUniqueWord(String [] words)
	{
		String uniqueWord = null;
		int minWordRefCount = Integer.MAX_VALUE;
		for(String word : words)
		{
			if(!word.toLowerCase().contains("kuubli") && fullLogs.containsKey(word))
			{
				int refCount = fullLogs.get(word)[AFTER].size();
//				System.out.println(word + " has reference count: " + refCount);
				if(refCount < minWordRefCount)
				{
					minWordRefCount = refCount;
					uniqueWord = word;
				}
			}
		}
		return uniqueWord;
	}
	
	private void processCommand(String channel, String sender, String message, String[] words)
	{
		if(isAdmin(sender))
		{
			adminCommand(words);
		}
		
		if(isMod(sender))
		{
			modCommand(channel, message);
		}
		
		userCommand(channel, sender, message, words);
		
	}
	
	@SuppressWarnings("unchecked")
	protected void adminCommand(String[] words)
	{
		if(words[0].equals("!join"))
		{
			try {
				BotManager.activateSmartBot(words[1]);
				System.out.println(dateF.format(new Date()) + ": " + "Joining channel: " + words[1]);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		else if(words[0].equalsIgnoreCase("!savechatlogs"))
		{
			saveLogs();
		}
		else if(words[0].equalsIgnoreCase("!dumplog"))
		{	
			if(words.length > 1)
				printLogs(readLogs(), words[1]);
			else
			{
				printLogs(readLogs());
			}
		}
		else if(words[0].equalsIgnoreCase("!speak"))
		{
			canSpeak = true;
			sendMsg(homeChannel, "Hi!");
		}
		else if(words[0].equalsIgnoreCase("!quiet"))
		{
			canSpeak = false;
			sendMsg(homeChannel, "/me enters lurk mode");
		}


	}
	
	private void modCommand(String channel, String message)
	{
		if (message.equalsIgnoreCase("!modlist")) 
		{
			if(channel.equalsIgnoreCase(homeChannel))
				sendMsg(channel, mods.toString());
			else
				sendMsg(channel, "Command not available outside home channel");
		}
		else if (message.equalsIgnoreCase("!steal")) 
		{
			sendMsg(channel, "!songs list stealsong");
		}	
	}
	
	private void pyramidCommand(String channel, String sender, String[] words)
	{
		if(!canSpeak) return;
		String face = words[1];
		int size = Integer.parseInt(words[words.length - 1]);
		System.out.println(dateF.format(new Date()) + ": " + sender + " triggered pyramid of '" + face + "' of size " + size);
		if (size > 8)
			sendMsg(channel, "Wow " + sender + " that's like waaaay to big");
		else
		{
			sendMsg(channel, "Pyramid for " + sender + ":");
			for (int i = 1; i <= size; i++)
			{
				String sendFace = "";
				for (int j = 1; j <= i; j++)
				{
					sendFace = sendFace + face + " ";
				}
				sendMsg(channel, sendFace);
				try {
				    Thread.sleep(1);
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
			}
			for (int i = size - 1; i >= 1; i--)
			{
				String sendFace = "";
				for (int j = 1; j <= i; j++)
				{
					sendFace = sendFace + face + " ";
				}
				sendMsg(channel, sendFace);
			}					
		}
	}
	
	private void userCommand(String channel, String sender, String message, String[] words)
	{
		if (message.equalsIgnoreCase("!time")) 
		{
			String time = new java.util.Date().toString();
			sendMsg(channel, sender + ": The time is now " + time);
		}
		else if (message.equalsIgnoreCase("!userlist"))
		{
			if(channel.equalsIgnoreCase(homeChannel))
				sendMsg(channel, getUserList());
			else
				sendMsg(channel, "Command not available outside home channel");
		}
		else if (words[0].equalsIgnoreCase("!pyramid"))
		{
			pyramidCommand(channel, sender, words);
		}
	}

	@SuppressWarnings("unchecked")
	private void logMessage(String[] words)
	{
		messageCount++;
		if(words.length > 50 || words.length < 1)
		{
//			TODO:Handle copy past-uh message
			System.out.println(dateF.format(new Date()) + ": " + "Message too long or too short.");
			return;
		}

		if(tempLogs.containsKey(words[0]))
		{
			ArrayList<String>[] list = tempLogs.get(words[0]);
			list[BEFORE].add(STRING_STARTER);
			if(words.length > 1) list[AFTER].add(words[1]);
		}
		else
		{
			ArrayList<String>[] list = new ArrayList[2]; 
			list[BEFORE] = new ArrayList<String>();
			list[AFTER] = new ArrayList<String>();
			list[BEFORE].add(STRING_STARTER);
			if(words.length > 1) list[AFTER].add(words[1]);
			tempLogs.put(words[0], list);
		}

		
		for(int i = 1; i < words.length - 1; i++)
		{
			if(tempLogs.containsKey(words[i]))
			{
				tempLogs.get(words[i])[BEFORE].add(words[i-1]);
				tempLogs.get(words[i])[AFTER].add(words[i+1]);
			}
			else
			{
				ArrayList<String>[] list = new ArrayList[2]; 
				list[BEFORE] = new ArrayList<String>();
				list[AFTER] = new ArrayList<String>();
				list[BEFORE].add(words[i-1]);
				list[AFTER].add(words[i+1]);
				tempLogs.put(words[i], list);
			}
		}
		
		if(tempLogs.containsKey(words[words.length - 1]))
		{
			ArrayList<String>[] list = tempLogs.get(words[words.length - 1]);
			if(words.length > 1) list[BEFORE].add(words[words.length - 2]);
			list[AFTER].add(STRING_TERMINATE);
		}
		else
		{
			ArrayList<String>[] list = new ArrayList[2]; 
			list[BEFORE] = new ArrayList<String>();
			list[AFTER] = new ArrayList<String>();
			if(words.length > 1) list[BEFORE].add(words[words.length - 2]);
			list[AFTER].add(STRING_TERMINATE);
			tempLogs.put(words[words.length - 1], list);
		}
		
		if(messageCount > 100)
		{
			saveLogs();
			messageCount = 0;
		}
		
	}
	
	public void printLogs(HashMap<String, ArrayList<String>[]> log)
	{
		System.out.println(dateF.format(new Date()) + ": " + "Printing logs: ");
		for(String key : log.keySet())
		{
			System.out.println(key + " BEFORE: " + log.get(key)[BEFORE]);
			System.out.println(key + " AFTER: " + log.get(key)[AFTER]);
		}
	}

	public void printLogs(HashMap<String, ArrayList<String>[]> log, String key)
	{
		System.out.println(dateF.format(new Date()) + ": " + "Printing logs: " + log);
		System.out.println(key + " BEFORE: " + log.get(key)[BEFORE]);
		System.out.println(key + " AFTER: " + log.get(key)[AFTER]);
	}

	private void updateLogs()
	{
		for(String key : tempLogs.keySet())
		{
			if(fullLogs.containsKey(key))
			{
				//System.out.println(key);
				fullLogs.get(key)[BEFORE].addAll(tempLogs.get(key)[BEFORE]);
				fullLogs.get(key)[AFTER].addAll(tempLogs.get(key)[AFTER]);
			}
			else
			{
				fullLogs.put(key, tempLogs.get(key));
			}
		}

	}
	
	@SuppressWarnings("unchecked")
	public void saveLogs()
    {
		System.out.println(dateF.format(new Date()) + ": " + "Saving logs....");
		
		fullLogs = readLogs();
		if (fullLogs == null)
		{
			System.out.println("LOGS WERE NULL");
			fullLogs = new HashMap<String, ArrayList<String>[]>();
		}
		updateLogs();
		try
		{
	           FileOutputStream fos =
	              new FileOutputStream(filename, false);
	           ObjectOutputStream oos = new ObjectOutputStream(fos);
	           oos.writeObject(fullLogs);
	           oos.close();
	           fos.close();
	           System.out.println(dateF.format(new Date()) + ": " + "Chat data from '" + homeChannel + "' has been saved in " + filename);
	    }
		catch(IOException ioe)
	    {
			ioe.printStackTrace();
	    }
		tempLogs.clear();
		
    }	
	
	@SuppressWarnings({ "rawtypes" })
	public HashMap readLogs()
	{
		System.out.println(dateF.format(new Date()) + ": " + "Attempting to read " + filename);
		HashMap temp = null;
		try
		{
			FileInputStream fis = new FileInputStream(filename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			temp = (HashMap) ois.readObject();
			ois.close();
			fis.close();
		}
		catch(IOException ioe)
		{
			//ioe.printStackTrace();
			System.out.println("No log file found; A new one will be created.");
			
			return null;
		}
		catch(ClassNotFoundException c)
		{
			System.out.println("Class not found");
			c.printStackTrace();
			return null;
		}
		System.out.println(dateF.format(new Date()) + ": " + "File read into channel: " + homeChannel);
		return(temp);
	}
	
	@SuppressWarnings({ "unchecked", "unused" })
	private void mergeOldData(String oldFileName)
	{
//		TODO: Finish this? or delete it
		String temp = filename;
		filename = oldFileName;
		HashMap<String, ArrayList<String>> old = readLogs();
		filename = temp;
		
		for(String key : old.keySet())
		{
			if(tempLogs.containsKey(key))
			{
				//System.out.println(key);
				ArrayList<String> after = old.get(key);
				fullLogs.get(key)[AFTER].addAll(after);
				
				for(String word : after)
				{
					if(tempLogs.containsKey(word))
					{
						//System.out.println(key);
						tempLogs.get(key)[BEFORE].add(key);
					}
					else
					{
						ArrayList<String>[] list = new ArrayList[2]; 
						list[BEFORE] = new ArrayList<String>();
						list[AFTER] = new ArrayList<String>();
//						TODO: FIGURE OUT WHAT THE FUCK TO DO HERE OR FUCK IT AND DELETE
						tempLogs.put(word, list);
					}
				}
			}
			else
			{
				fullLogs.put(key, tempLogs.get(key));
			}
		}
	}
	
	public void onUserMode(String targetNick,
            String sourceNick,
            String sourceLogin,
            String sourceHostname,
            String mode)
	{
		if(mode.contains("+o"))
		{
			System.out.println(dateF.format(new Date()) + ": " + mode);
			String mod = mode.substring(homeChannel.length() + 4);
			mods.add(mod);
			if(mod.equals(super.getNick()))
			{
				System.out.println("Mod status detected, changing chat delay");
				chatDelay = 1000;
			}
			//sendMsg("#khan___", "Mod detected: " + mode.substring(12));
		}
	}
	
	private boolean isMod(String nick)
	{
		for ( String mod : mods )
		{
			if(nick.equals(mod))
			{
				return true;
			}
		}
		return false;
	}
	
	protected boolean isAdmin(String nick)
	{
		return nick.equalsIgnoreCase("khan___");
		
	}
	
	private String getUserList(){
		
	     String list = "";
	
	     User userList[] = this.getUsers(homeChannel);
	
	     for( User user : userList ){
	
               list = list + user.getNick() + ", ";
	     }
	
	     return list;
	}
}
