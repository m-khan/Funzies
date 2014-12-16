#pragma semicolon 1
#include <sourcemod>
#include <sdktools>

new String:KillerList[MAXPLAYERS+1][MAX_NAME_LENGTH+1];
new String:VictimList[MAXPLAYERS+1][MAX_NAME_LENGTH+1];
new KilledBy[MAXPLAYERS+1];
new DelayedPunishment[MAXPLAYERS+1];
new DelayedPunishmentVictim[MAXPLAYERS+1];
new ACCOUNT_OFFSET;

#define PLUGIN_VERSION "1.1"
#define DO_NOT_PUNISH -1

public Plugin:myinfo =
{
	name = "Simple CS:GO TK Menu: eXtended",
	author = "Sheepdude, kHAN",
	description = "Displays a TK Menu with various punishments",
	version = PLUGIN_VERSION,
	url = "http://www.clan-psycho.com || http://abm.servecounterstrike.com"
};

public OnPluginStart()
{
	ACCOUNT_OFFSET = FindSendPropOffs("CCSPlayer", "m_iAccount");
	HookEvent("player_death", PlayerDeathEvent);
	HookEvent("player_spawned", PlayerSpawnedEvent);
	for (new id = 1; id <= MAXPLAYERS ; id++)
	{
		DelayedPunishment[id] = DO_NOT_PUNISH;
	}
	
}

public PlayerDeathEvent(Handle:event, const String:name[], bool:dontBroadcast)
{
	new victim = GetClientOfUserId(GetEventInt(event,"userid"));
	new killer = GetClientOfUserId(GetEventInt(event,"attacker"));
	GetClientName(killer, KillerList[victim], sizeof(KillerList[]));
	GetClientName(victim, VictimList[victim], sizeof(VictimList[]));
	if(victim > 0 && victim <= MaxClients && killer > 0 && killer <= MaxClients && GetClientTeam(victim) == GetClientTeam(killer) && victim != killer && !IsFakeClient(victim))
	{
		KilledBy[victim] = killer;
		doTKMenu(victim);
	}
}

public PlayerSpawnedEvent(Handle:event, const String:name[], bool:dontBroadcast)
{
	new client = GetClientOfUserId(GetEventInt(event, "userid"));
	if(DelayedPunishment[client] != DO_NOT_PUNISH)
		punish(DelayedPunishmentVictim[client], DelayedPunishment[client]);
}


public doTKMenu(victim)
{
	decl String:menuTitle[64];
	Format(menuTitle, sizeof(menuTitle), "You were killed by %s, choose an action:", KillerList[victim]);
	new Handle:menu = CreateMenu(handleTKVoteMenu);
	SetMenuTitle(menu, menuTitle);
	AddMenuItem(menu, "0", "Forgive");
	AddMenuItem(menu, "1", "Steal 50% Cash");
	AddMenuItem(menu, "2", "Slay");
	// Options Below added by kHAN
	AddMenuItem(menu, "3", "Beacon");
	AddMenuItem(menu, "4", "Burn");
	AddMenuItem(menu, "5", "Slap to 10 HP");
	AddMenuItem(menu, "6", "Freeze for 1 min");
	AddMenuItem(menu, "7", "Firebomb");
	AddMenuItem(menu, "8", "Timebomb");
	DisplayMenu(menu, victim, MENU_TIME_FOREVER);
}

public handleTKVoteMenu(Handle:menu, MenuAction:action, param1, param2) 
{
	if (action == MenuAction_End) 
	{
		CloseHandle(menu);
	}
	else if (action == MenuAction_Select)
	{
		if(IsClientInGame(KilledBy[param1]))
		{
			if(IsPlayerAlive(KilledBy[param1]))
			{
				punish(param1, param2);
			}
			else
			{
				DelayedPunishment[KilledBy[param1]] = param2;
				DelayedPunishmentVictim[KilledBy[param1]] = param1; 
				PrintToChat(param1, "Punishment will be delayed until the next round");
			}
		}
	}
}

public punish(param1, param2)
{
	if(param2 == 0)
		PrintToChatAll("\x01\x0B\x04[TKMenu]\x01 %s has forgiven %s for team killing.", VictimList[param1], KillerList[param1]);
	else if(param2 == 1 && IsClientInGame(param1) && IsClientInGame(KilledBy[param1]))
	{
		new KillerMoney = GetEntData(KilledBy[param1], ACCOUNT_OFFSET, 4);
		new VictimMoney = GetEntData(param1, ACCOUNT_OFFSET, 4);
		new difference = RoundToFloor(KillerMoney * 0.5);
		SetEntData(KilledBy[param1], ACCOUNT_OFFSET, KillerMoney - difference, 4, true);
		if(VictimMoney + difference > 16000)
			SetEntData(param1, ACCOUNT_OFFSET, 16000, 4, true);
		else
			SetEntData(param1, ACCOUNT_OFFSET, VictimMoney + difference, 4, true);
		PrintToChatAll("\x01\x0B\x04[TKMenu]\x01 %s stole \x05$%d\x01 from %s for team killing.", VictimList[param1], difference, KillerList[param1]);
	}
	else if(param2 == 2 && IsClientInGame(KilledBy[param1]))
	{
		ForcePlayerSuicide(KilledBy[param1]);
		PrintToChatAll("\x01\x0B\x04[TKMenu]\x01 %s was slain for team killing %s.", KillerList[param1], VictimList[param1]);
	}
	else if(param2 == 3 && IsClientInGame(KilledBy[param1]))
	{
		ServerCommand("sm_beacon \"%s\"", KillerList[param1]);
		PrintToChatAll("\x01\x0B\x04[TKMenu]\x01 %s has been turned into a beacon for team killing %s.", KillerList[param1], VictimList[param1]);			
	}
	else if(param2 == 4 && IsClientInGame(KilledBy[param1]))
	{
		ServerCommand("sm_burn \"%s\"", KillerList[param1]);
		PrintToChatAll("\x01\x0B\x04[TKMenu]\x01 %s was burned for team killing %s.", KillerList[param1], VictimList[param1]);			
	}
	else if(param2 == 5 && IsClientInGame(KilledBy[param1]))
	{
		SetEntityHealth(KilledBy[param1], 10);
		PrintToChatAll("\x01\x0B\x04[TKMenu]\x01 %s has been slapped to 10 HP for team killing %s.", KillerList[param1], VictimList[param1]);			
	}
	else if(param2 == 6 && IsClientInGame(KilledBy[param1]))
	{
		ServerCommand("sm_freeze \"%s \" 60", KillerList[param1]);
		PrintToChatAll("\x01\x0B\x04[TKMenu]\x01 %s has been frozen for team killing %s.", KillerList[param1], VictimList[param1]);			
	}
	else if(param2 == 7 && IsClientInGame(KilledBy[param1]))
	{
		ServerCommand("sm_firebomb \"%s\"", KillerList[param1]);
		PrintToChatAll("\x01\x0B\x04[TKMenu]\x01 %s has been turned into a firebomb for team killing %s.", KillerList[param1], VictimList[param1]);			
	}
	else if(param2 == 8 && IsClientInGame(KilledBy[param1]))
	{
		ServerCommand("sm_timebomb \"%s\"", KillerList[param1]);
		PrintToChatAll("\x01\x0B\x04[TKMenu]\x01 %s has been turned into a timebomb for team killing %s.", KillerList[param1], VictimList[param1]);			
	}
}
