#include <sourcemod>
#include <sdktools>

#define PLUGIN_VERSION "1.0"
#define MAXTEAMS 10
#define TEAM_NONE 0
#define TEAM_SPEC 1
#define TEAM_T 2
#define TEAM_CT 3
#define SPREAD_AVERAGE 2
#define SPREAD_GOOD 1
#define SPREAD_BAD 3
#define SPREAD_ANY 0

new Scores[MAXTEAMS] = {0, ...};
new Winstreak = 0;
new Previous_Winner = 0;
new Phase = 0;
new String:SwitchedPlayer[MAX_NAME_LENGTH + 1];
new PistolRound = 0;

new Handle:g_Cvar_Balance_Enabled = INVALID_HANDLE;

public Plugin:myinfo =
{
	name = "Handicaps",
	author = "kHAN",
	description = "Gives a severely losing team a monetary handicap, balances teams when needed based on winstreak",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};


public OnPluginStart() 
{
	g_Cvar_Balance_Enabled  = CreateConVar("sm_khanbalance", "1", "Enables kHAN's Teambalance",FCVAR_PLUGIN);
	HookEvent("round_end", Event_RoundEnd);
	HookEvent("round_start", Event_RoundStart);
	HookEvent("announce_phase_end", Event_Halftime);
	HookEvent("round_poststart", OnRoundPostStart);
}

public OnMapEnd()
{
	Scores[TEAM_CT] = 0;
	Scores[TEAM_T] = 0;
	Winstreak = 0;
	Previous_Winner = 0;
	Phase = 0;
	ServerCommand("sm_disablemodes");
	ServerCommand("sm_cvar sv_airaccelerate 10");
	ServerCommand("sv_showimpacts 1");
}

public Event_Halftime(Handle:event, const String:name[], bool:dontBroadcast)
{
	Phase++;
	if(Phase == 1) // This triggers at normal halftime
	{
		ServerCommand("mp_overtime_enable 1");
		ServerCommand("mp_overtime_maxrounds 2");
		ServerCommand("mp_overtime_halftime_pausetimer 1");
		CreateTimer(30.0, ChangeHalftimeDuration);
		new Swapper = Scores[TEAM_CT];
		Scores[TEAM_CT] = Scores[TEAM_T];
		Scores[TEAM_T] = Swapper;
		PrintToChatAll("Teams have been switched for halftime");
		if (Previous_Winner == TEAM_CT)
		{
			Previous_Winner = TEAM_T;
		}
		else
		{
			Previous_Winner = TEAM_CT;
		}
		PistolRound = 1;
	}
	else if(Phase == 2) // this triggers at the end of the game
	{
		PrintToChatAll("CT Score is: %i", Scores[TEAM_CT]);
		PrintToChatAll("T Score is: %i", Scores[TEAM_T]);

		ServerCommand("mp_halftime_duration 15");
		if(Scores[TEAM_CT] == Scores[TEAM_T])
		{
			PrintToChatAll("OVERTIME VOTE");
			DoOvertimeVote();
		}
	}
	
	/*
	else if(Phase >= 3)
	{
		PrintToChatAll("ADDITIONAL PHASE TRIGGERED");
	}
	*/
}

public Action:ChangeHalftimeDuration(Handle:timer)
{
	ServerCommand("mp_halftime_duration 3");
}

public Action:CheckTeams(Handle:timer)
{
	if(GetConVarInt(g_Cvar_Balance_Enabled) > 0) {
		new ctCount = GetTeamClientCount(TEAM_CT);
		//PrintToChatAll("CT has %i Players", ctCount);
		new tCount = GetTeamClientCount(TEAM_T);
		//PrintToChatAll("T has %i Players", tCount);
		
		while(ctCount - tCount > 1)
        {
			//PrintToChatAll("CTS HAVE AT LEAST 2 MORE THAN TERRORISTS");
			new client = GetPlayerToSwitch(TEAM_CT)
			if(client != -1)
			{
				GetClientName(client, SwitchedPlayer, sizeof(SwitchedPlayer));
				ServerCommand("sm_dropc4");
				ServerCommand("sm_team %s %i", SwitchedPlayer, TEAM_T);
				//PrintToChatAll("\x04%s\x01 has been moved to T to balance the teams", SwitchedPlayer);
				ctCount--; tCount++;
			}
		}
		
		while(tCount - ctCount > 1)
        {
            //PrintToChatAll("TERRORISTS HAVE AT LEAST 2 MORE THAN CTS");
			new client = GetPlayerToSwitch(TEAM_T)
			if (client != -1)
			{
				GetClientName(client, SwitchedPlayer, sizeof(SwitchedPlayer));
				ServerCommand("sm_dropc4");
				ServerCommand("sm_team %s %i", SwitchedPlayer, TEAM_CT);
				//PrintToChatAll("\x04%s\x01 has been moved to CT to balance the teams", SwitchedPlayer);
				tCount--; ctCount++;
			}
		}
    }
}

public OnRoundPostStart(Handle:event, const String:name[], bool:dontBroadcast){
	CreateTimer(0.5, CheckTeams);
}



public GetPlayerToSwitch(team)
{
	new playerType;
	if(Winstreak <=3)  // TEAMS ARE BALANCED, SWITCH A NORMAL PLAYER
	{
		playerType = SPREAD_AVERAGE;
	}
	else if(Previous_Winner == team) //CTs ARE DOMINATING, SWITCH A GOOD PLAYER
	{
		playerType = SPREAD_GOOD;
	}
	else //CTs ARE LOSING, SWITCH A BAD PLAYER
	{
		playerType = SPREAD_BAD;
	}
	
	new client;
	new checkcount = 0;
	
	while(true)
	{
		client = GetRandomInt(1 , MaxClients);
		if(IsClientInGame(client) && GetClientTeam(client) == team)
		{	
			if(playerType == SPREAD_ANY)
			{
				return client;
			}
			else
			{
				new Float:kdratio = GetClientFrags(client) / (GetClientDeaths(client) + 0.1);
				//PrintToChatAll("client %i has kdr %f", client, kdratio);
				if(kdratio > 1.8)
				{
					if(playerType == SPREAD_GOOD)
					{
						return client;
					}
				}
				else if(kdratio > 0.8)
				{
					if(playerType == SPREAD_AVERAGE)
					{
						return client;
					}
				}
				else
				{
					if(playerType == SPREAD_BAD)
					{
						return client;
					}
				}
			}
		}
		checkcount++;
		if(checkcount > 200)
		{
			playerType = SPREAD_ANY;
		}
		else if(checkcount > 300)
		{
			PrintToChatAll("ERROR: Autoteambalance failed to find a player to switch");
			return -1;
		}
	}
}

public Event_RoundEnd(Handle:event, const String:name[], bool:dontBroadcast)
{
	new winner = GetEventInt(event, "winner");
	
	Scores[winner]++;
	/*
	PrintToChatAll("CT Score is: %i", Scores[TEAM_CT]);
	PrintToChatAll("T Score is: %i", Scores[TEAM_T]);
	*/
	if (winner == 0 || winner == 1)
	{
		return;
	}
	
	if (winner == Previous_Winner)
	{
		Winstreak++;
	}
	else
	{
		Winstreak = 1;
		Previous_Winner = winner;
	}
}

public Event_RoundStart(Handle:event, const String:name[], bool:dontBroadcast)
{
	if(PistolRound == 1)
	{
		PistolRound = 0;
	}
	else if(Winstreak >= 4)
	{	
		if(Winstreak <= 6)
		{
			if(Previous_Winner == TEAM_T)
			{
				ServerCommand("sm_adv_silent 1");
				ServerCommand("sm_cash @ct +%i", (Winstreak - 3) * 500);
				ServerCommand("sm_adv_silent 0");
				PrintToChatAll("Terrorists have won %i rounds in a row", Winstreak);
				PrintToChatAll("CT team has been given an extra $%i", (Winstreak -3) * 500);
			}
			if(Previous_Winner == TEAM_CT)
			{
				ServerCommand("sm_adv_silent 1");
				ServerCommand("sm_cash @t +%i", (Winstreak - 3) * 500);
				ServerCommand("sm_adv_silent 0");
				PrintToChatAll("Counter-Terrorists have won %i rounds in a row", Winstreak);
				PrintToChatAll("Terrorist team has been given an extra $%i", (Winstreak -3) * 500);
			}
		}
		else
		{
			if(Previous_Winner == TEAM_T)
			{
				ServerCommand("sm_adv_silent 1");
				ServerCommand("sm_cash @ct +%i", 2000);
				ServerCommand("sm_adv_silent 0");
				PrintToChatAll("Terrorists have won %i rounds in a row", Winstreak);
				PrintToChatAll("CT team has been given an extra $%i", 2000);
			}
			if(Previous_Winner == TEAM_CT)
			{
				ServerCommand("sm_adv_silent 1");
				ServerCommand("sm_cash @t +%i", 2000);
				ServerCommand("sm_adv_silent 0");
				PrintToChatAll("Counter-Terrorists have won %i rounds in a row", Winstreak);
				PrintToChatAll("Terrorist team has been given an extra $%i", 2000);
			}
		}
	}


}

public DoOvertimeVote()
{
	Winstreak = 0;
	ServerCommand("sm_disablemodes");
	ServerCommand("sm_loadfunplugins");
	ServerCommand("sm_goldknife 0");
	if(IsVoteInProgress())
	{ CancelVote(); }
	new Handle:menu = CreateMenu(Handle_VoteMenu);
	SetMenuTitle(menu, "Overtime Vote!")
	AddMenuItem(menu, "Dodgeball", "Dodgeball");
	AddMenuItem(menu, "Quake", "Quake");
	AddMenuItem(menu, "Zeus Mode", "Zeus Mode");
	AddMenuItem(menu, "Headhunter", "Headhunter");	
	AddMenuItem(menu, "Noscope", "Noscope");
	AddMenuItem(menu, "$1800 Eco", "$1800 Eco");
	SetMenuExitButton(menu, false);
	VoteMenuToAll(menu,12);
	ServerCommand("mp_maxmoney 0");
	ServerCommand("mp_timelimit 1");
}

public Handle_VoteMenu(Handle:menu, MenuAction:action, param1, param2)
{
	if (action == MenuAction_End)
	{
		CloseHandle(menu);
	}
	else if(action == MenuAction_VoteEnd)
	{
		ServerCommand("mp_halftime_pausetimer 0");
		PrintToChatAll("The voting is finished, the winner is:");
		switch(param1)
		{
			case 0:
			{
				ServerCommand("sm_flashDB 1");
				ServerCommand("sv_ignoregrenaderadio 1");
				PrintToChatAll("Dodgeball Mode!");
			}
			case 1:
			{
				ServerCommand("sm_scoutzknivez 1");
				ServerCommand("sm_cvar sv_airaccelerate 200");
				ServerCommand("sv_gravity 300");
				PrintToChatAll("Quake Mode!");
			}
			case 2:
			{
				ServerCommand("sm_zeus 1");
				PrintToChatAll("Zeus Mode!");
			}
			case 3:
			{
				ServerCommand("sm_headhunter 1");
				PrintToChatAll("Headhunter Mode!");
				ServerCommand("mp_ct_default_secondary weapon_p250");
				ServerCommand("mp_t_default_secondary weapon_p250");
			}
			case 4:
			{
				ServerCommand("sm_noscope 1");
				ServerCommand("sv_showimpacts 1");
				ServerCommand("sv_showimpacts_time .05");			
				PrintToChatAll("NoScope Mode!");
			}
			case 5:
			{
				ServerCommand("mp_maxmoney 16000");
				ServerCommand("mp_overtime_startmoney 1800");
				PrintToChatAll("1 round, $1800, Eco Weapons!");
			}
		}
	}
}

