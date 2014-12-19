package khanbot.TPP;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jibble.pircbot.PircBot;

public class TPPBot extends PircBot {

	static final String TEAM_RED = " red";
	static final String TEAM_BLUE = " blue";
	static final int BET_ALLIN = 0;
	static final int BET_NORMAL = 1;
	static final int BET_MIN = 2;
	final String channel;
	final String fileName;
	boolean isBP;  //is Betting Period
	boolean wfb; //waiting for balance
	int tB; //total blue bets
	int tR; //total red bets
	int tBa; //total blue bets ignoring big bets
	int tRa; //total red bets ignoring big bets
	int balance; //current balance
	ArrayList<Integer> lbR; //Stores large bets for red
	ArrayList<Integer> lbB; //Stores large bets for blue
	ConcurrentHashMap<String, TPPUser> userList;
	Set<TPPUser> redTeam;
	Set<TPPUser> blueTeam;
	ExecutorService threads;
	
	public TPPBot(String name)
	{
		this.setName(name);
		channel = "#twitchplayspokemon";
		fileName = "tppbot1.ser";
		tB = 0;
		tR = 0;
		tBa = 0;
		tRa = 0;
		balance = 100;
		lbR = new ArrayList<Integer>();
		lbB = new ArrayList<Integer>();
		
		redTeam = new HashSet<TPPUser>();
		blueTeam = new HashSet<TPPUser>();
		
		threads = Executors.newFixedThreadPool(8);
		
		loadUserList();
		
	}
	
	public void tppConnect(String password)
	{
		try {
			this.connect("irc.twitch.tv", 6667, password);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		this.joinChannel("#twitchplayspokemon");
	}

	public void onMessage(String chan, String sender,
            String login, String hostname, String message)
	{
		threads.execute(new TPPMessageProcessor(this, sender, message));
	}
	
	private void sop(String p)
	{
		sop(p, 9);
		//LOGGER.log(Level.INFO, p);
	}
	
	private void sop(String p, int color)
	{
//		System.out.println("Hello \u001b[1;31mred\u001b[0m world!");
		System.out.println("\u001b[" + (color+30) + "m" + p);
	}
	
	private void sop(String p, int color, boolean bold)
	{
		if(bold)
		{
			System.out.println("\u001b[1;" + (color+30) + "m" + p);
		}
		else
		{
			System.out.println("\u001b[" + (color+30) + "m" + p);
		}
	}
	
	public void infoDump()
	{
		sop("======== INFODUMP ========");
		sop("tRa: " + tRa);
		sop("tBa: " + tBa);
		sop("tR: " + tR);
		sop("tB: " + tB);
		sop("lbR: " + lbR);
		sop("lbB: " + lbB);
		sop("balance: " + balance);
	}
	
	protected void processMessage(String sender, String message, String[] words)
	{
		if(sender.equalsIgnoreCase("tppinfobot"))
		{
			sop("========= INFO: " + message + " =========", 6, true);
			if(message.equalsIgnoreCase("A new match is about to begin!"))
			{
//				Signals the beginning of the betting period

				sop("========= NEW MATCH STARTED =========", 6, true);
				newMatch();
			}
			else if(message.equalsIgnoreCase("Betting closes in 10 seconds"))
			{
//				Signals the end of the betting period, this is where we bet.
				sop("========= BETTING PERIOD ENDING =========", 6, true);
				Timer timer = new Timer();
				timer.schedule(new FinishTimer(this), 8000);
			}
			else if("Team Blue won the match!".equalsIgnoreCase(message))
			{
				sop("========= RECORDING BLUE VICTORY =========", 4, true);				
				for(TPPUser user : blueTeam)
				{
					user.bets++;
					user.wins++;
					sop("Win recorded for: " + user);
				}
				for(TPPUser user : redTeam)
				{
					user.bets++;
					sop("Loss recorded for: " + user);
				}
				
			}
			else if("Team Red won the match!".equalsIgnoreCase(message))
			{
				sop("========= RECORDING RED VICTORY =========", 1, true);
				for(TPPUser user : redTeam)
				{
					user.bets++;
					user.wins++;
					sop("Win recorded for: " + user);
				}
				for(TPPUser user : blueTeam)
				{
					user.bets++;
					sop("Loss recorded for: " + user);
				}
			}
		}
		else if(sender.equalsIgnoreCase("tppbankbot"))
		{
			sop("BANK: " + message);
			String userName = words[0].substring(1);
			int userBalance = Integer.parseInt(words[words.length - 1].replace(",",""));
			
			if(userName.endsWith("khan___"))
			{
				sop("========= CURRENT BALANCE: " + userBalance + " =========", 3, true);
				balance = userBalance;
				return;
			}
			
			if(userList.containsKey(userName))
			{
				userList.get(userName).balance = userBalance;
				sop("Balance updated for " + userName + ": " + userBalance, 3);
			}
			else
			{
				sop("Recording new user: " + userName + ": " + userBalance, 4);
				TPPUser newUser = new TPPUser(userName);
				newUser.balance = userBalance;
				userList.put(userName, newUser);
			}
		}
		else if(isBP)
		{
			if(words.length == 3 && words[0].equalsIgnoreCase("!bet"))
			{
				sop(sender + ": " + message);

				if(userList.containsKey(sender))
				{
					TPPUser better = userList.get(sender);
					int bet = 0;
					try{
					bet = Integer.parseInt(words[1].replace(",",""));
					}catch(Exception e)
					{
						sop("Incorrect bet format", 1);
						return;
					}
					String team = words[2];
					
					if(bet <= better.balance && !blueTeam.contains(better) && !redTeam.contains(better))
					{
						processBet(better, bet, team);
					}
					else if(better.balance < bet)
					{
						sop("User '" + sender + "' bet " + bet + " when his balance was only " + better.balance, 1);
					}
					else
					{
						sop("User '" + sender + "' tried to place multiple bets");
					}
				}
				else
				{
					sop("User '" + sender + "' was not found in User List.", 1);
				}
			}
		}
	}

	private void printUserList(Map<String, TPPUser> users){
		for(String key : users.keySet())
		{
			sop(users.get(key)+"");
		}
	}
	
	@SuppressWarnings("unchecked")
	private void loadUserList()
	{
		sop("Loading user list...");
		
    	HashMap<String, TPPUser> loadMap = null;
		try{
			FileInputStream fileIn = new FileInputStream(fileName);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			loadMap = (HashMap<String, TPPUser>) in.readObject();
			in.close();
			fileIn.close();
		}catch(Exception e){
			e.printStackTrace();
			loadMap = null;
		}
		
		if(loadMap != null)
		{
			printUserList(loadMap);
			
			userList = new ConcurrentHashMap<String, TPPUser>(loadMap);
			sop("Loaded information for " + userList.size() + " users.", 2, true);
		}
		else
		{
			sop("WARNING: User list file " + fileName + " not found, creating new list", 1, true);
			userList = new ConcurrentHashMap<String, TPPUser>();
		}
		
	}
	
	private void saveUserList()
	{
        try{
        	HashMap<String, TPPUser> saveMap = new HashMap<String, TPPUser>(userList);
        	FileOutputStream fout = new FileOutputStream(fileName);
        	ObjectOutputStream out = new ObjectOutputStream(fout);
        	out.writeObject(saveMap);
        	out.close();
        	fout.close();
        	sop("User List saved in " + fileName, 2, true);
        }catch(IOException e){
        	e.printStackTrace();
        }

	}
	
	private void newMatch()
	{
		
		blueTeam.clear();
		redTeam.clear();

		saveUserList();
		
		isBP = true;
		tB = 0;
		tR = 0;
		tBa = 0;
		tRa = 0;
		lbR.clear();
		lbB.clear();
		
		try {
		    Thread.sleep((long)((Math.random() * 5000) + 20000));
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
		wfb = true;
		sendMessage(channel, "!balance");
	}
	
	public void startBet()
	{
		isBP = false;
		processOdds();
		infoDump();
	}
	
	private void sendMsg(String message)
	{
		sendMessage(channel, message);
		sop("MESSAGE SENT: " + message, 9, true);
	}
	
	private void processBet(TPPUser better, int bet, String team)
	{
		sop("Recording Bet: " + better + ", " + bet + ", " + team, 5);
		if(team.equalsIgnoreCase("red"))
		{
			redTeam.add(better);
			tR += bet;
			if(bet <= 1000 && bet > 0) 
			{
				tRa += bet;
			}
			else if (bet > 1000)
			{
				tRa += 1000;
				lbR.add(new Integer(bet));
			}					
		}
		if(team.equalsIgnoreCase("blue"))
		{
			blueTeam.add(better);
			tB += bet;
			if(bet <= 1000 && bet > 0) 
			{
				tBa += bet;
			}
			else if (bet > 1000)
			{
				tBa += 1000;
				lbB.add(new Integer(bet));
			}					
		}
	}
	
	private void processOdds()
	{
		double rOdds = (tRa * 1.0) / (tBa * 1.0);
		double bOdds = (tBa * 1.0) / (tRa * 1.0);
		
		if(!processOddsBasic(rOdds, bOdds))
		{
			processCloseOdds();
		}
	}
	
	private boolean processOddsBasic(double rOdds, double bOdds)
	{
		sop("Odds:");
		sop("RED: " + rOdds);
		sop("BLUE: " + bOdds);
		boolean betMade = false;
		if(rOdds > 2)
		{
			betMade = true;
			if(rOdds > 10)
				makeBet(TEAM_RED, BET_ALLIN);
			else
				makeBet(TEAM_RED, BET_NORMAL);
		}
		else if(bOdds > 2)
		{
			betMade = true;
			if(bOdds > 10)
				makeBet(TEAM_BLUE, BET_ALLIN);
			else
				makeBet(TEAM_BLUE, BET_NORMAL);
		}
		return betMade;
	}
	
	private void processCloseOdds()
	{
		boolean outR = false;
		for(Integer i : lbR)
		{
			if(i.intValue() > tRa)
				outR = true;
		}
		
		boolean outB = false;
		for(Integer i : lbB)
		{
			if(i.intValue() > tBa)
				outB = true;
		}
		
		if(outR && outB)
		{
//			Both sides have been stacked, probably should stay out
			sop("========= ODDS UNPREDICTABLE, NO BET MADE =========");
			this.sendMsg("!bet " + 50 + " red");
		}
		else if(outR)
		{
			makeBet(TEAM_BLUE, BET_MIN);
		}
		else if(outB)
		{
			makeBet(TEAM_RED, BET_MIN);			
		}
		else
		{
			balance = 500;
			double rOdds = (tR * 1.5) / (tB * 1.0);
			double bOdds = (tB * 1.5) / (tR * 1.0);
			if(!processOddsBasic(rOdds, bOdds))
			{
				sop("========= ODDS TOO CLOSE TO CALL =========");
				this.sendMsg("!bet " + 50 + " red");

			}
		}
	}
	
	private void makeBet(String team, int betFlag)
	{
		this.sendMsg("!bet " + 100 + team);
	}

	public static void main(String[] args)
	{
			joinTPP();

	}
	
	public static void joinTPP()
	{
		TPPBot bot = new TPPBot("Khan___");
		bot.tppConnect("oauth:8xqea4a2u4x4zzcfhrrpgh86q80l7ot");
		System.out.println("TPPBot Activated.");
	}


}

class FinishTimer extends TimerTask
{
	TPPBot master;
	
	public FinishTimer(TPPBot bot)
	{
		master = bot;
	}
	
	public void run()
	{
		master.startBet();
	}
}

