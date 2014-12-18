package khanbot.TPP;

public class TPPMessageProcessor implements Runnable {

	TPPBot bot;
	String sender;
	String message;
	
	public TPPMessageProcessor(TPPBot bot, String sender, String message)
	{
		this.bot = bot;
		this.sender = sender;
		this.message = message;
	}
	
	@Override
	public void run() {
		String words[] = message.split(" "); 
		bot.processMessage(sender, message, words);
	}

}
