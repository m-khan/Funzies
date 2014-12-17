package khanbot;

import khanbot.TPP.TPPBot;


public class BotManager {

	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		if(args.length > 0)
		{
			for(String s : args)
			{
				activateSmartBot(s);
			}
		}
		else
		{
			//activateSmartBot("khan___");
			//joinTPP();
	/*		
			activateSmartBot("shifty_time");
			activateSmartBot("mistermonopoli");
			activateSmartBot("goatrope");
			activateSmartBot("hockeyfan48");
			activateSmartBot("willzorss");
			activateSmartBot("somefilthycasuals");
			activateSmartBot("slidingghost");
			activateSmartBot("vetroxity");
			activateSmartBot("sub_whistle");
			activateSmartBot("hiipfire");
			activateSmartBot("pikminion");
			activateSmartBot("gamesswag");
			activateSmartBot("stuffedcrustftw");
			activateSmartBot("darkdevastat10n");
			activateSmartBot("desksol");
			activateSmartBot("skurty_");
			activateSmartBot("hidingtonight");
			activateSmartBot("direonyx");
			activateSmartBot("ruudyt");
			activateSmartBot("azurespirit");
	//*/
		}
		//joinDefaultChannel();
		
/*		if(args.length > 0)
		{
			joinChannel(args[0]);
		}
		else
		{
			joinDefaultChannel();
		}
*/
	}

	public static void activateSmartBot(String channel) throws Exception
	{
//		KublaiBot kublai = new KublaiBot("kublai_");
//		kublai.twitchConnect("oauth:bjsskbw5anwo2nsxpbuufhnrwc55lq6", channel);

		KuubliBot kuubli = new KuubliBot("kuubli", channel);
		kuubli.twitchConnect("oauth:3gkg7hhtz0xb6rus7pvxkpcvhqgx5xe", channel);
//		kublai.sendMessage(channel, "Smart Bot Active");
	}
	

	public static void joinDefaultChannel() throws Exception
	{
		String channel = "khan___";
		Bot khanbot = new Bot("khan___");
		khanbot.twitchConnect("oauth:8xqea4a2u4x4zzcfhrrpgh86q80l7ot", channel);
		khanbot.sendMessage("#" + channel, "Autoresponces Activated");
	
	}
	
	
	
	public static void joinChannel(String channel) throws Exception
	{
		//Bot kublai = new Bot("kublai_");
		//kublai.twitchConnect("oauth:190rhda671yp5cvdrvy9rqd19nhz6vx", channel);
		
		Bot khanbot = new Bot("khan_bot");
		khanbot.twitchConnect("oauth:o6ubqz9z8pb32j1vks5ezuboylrvwuy", channel);
		khanbot.sendMessage("#" + channel, "Hi!");
		
	}

}