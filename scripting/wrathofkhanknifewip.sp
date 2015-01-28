/*  WRATH OF KHAN version 2.1	

	This plugin is intended to discourage players from attacking other 
	teammates at the start of each round.  Any player who does this will
	be immediatly slayed.  
	
	Version 1.1  : Added health restoration for victims
				 : Fixed a crash associated with shotguns
			   
	Version 1.2  : Added slap as an alternative punishment
	Version 2.0  : Added Gold Knife for donors
			2.0.1: Added !nogoldknife command for Vtec (and others)
	Version 2.1  : Added Knife menu for donors
*/

#include <sourcemod>
#include <sdktools>
#include <sdkhooks>

#define PLUGIN_VERSION "2.1"

new SpawnTime[MAXPLAYERS + 1] = {0, ...};
new LastSpawnTime;
new KnifeType[MAXPLAYERS + 1] = {1, ...};
new String:attackername[MAX_NAME_LENGTH + 1];
new String:victimname[MAX_NAME_LENGTH + 1];
new String:newclassname[MAX_NAME_LENGTH +1];
new Handle:g_Cvar_Enabled = INVALID_HANDLE;
//new Handle:g_Cvar_SpawnTime = INVALID_HANDLE;
new Handle:g_Cvar_Mode = INVALID_HANDLE;
new Handle:g_Cvar_Damage = INVALID_HANDLE;
new Handle:g_Cvar_Knife_Enabled = INVALID_HANDLE;


public Plugin:myinfo =
{
	name = "WRATH OF KHAN",
	author = "kHAN",
	description = "Punishes players who attack teammates in spawn",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};


public OnPluginStart() 
{
	
	CreateConVar("sm_wrathofkhanversion", PLUGIN_VERSION, "Version of WRATH OF KHAN plugin", FCVAR_PLUGIN|FCVAR_SPONLY|FCVAR_REPLICATED|FCVAR_NOTIFY);
	g_Cvar_Enabled  = CreateConVar("sm_wrathofkhan", "1","Enables WRATH OF KHAN plugin",FCVAR_PLUGIN);
	//g_Cvar_SpawnTime = CreateConVar("sm_wrathofkhantime","10","Automatically punish any team attacker during this amount of seconds after spawn");
	// ^^BEING IGNORED FOR OPTIMIZATION FIXES... WILL REENABLE WHEN SERVER PERFORMANCE IS STABLE
	g_Cvar_Mode = CreateConVar("sm_wrathofkhanmode","0","0 = slay, 1 = slap");
	g_Cvar_Damage = CreateConVar("sm_wrathofkhandamage" , "25" , "Sets the damage for slap mode");
	g_Cvar_Knife_Enabled = CreateConVar("sm_goldknife" , "1", "Set to 1 to enable gold knives");
	
	HookEvent("player_hurt", Event_PlayerHurt);
	HookEvent("player_spawn", Event_PlayerSpawn);
	//HookEvent("round_freeze_end", Event_RoundFreezeEnd); === REMOVED SEE NOTE BELOW
	
	HookConVarChange(g_Cvar_Enabled, OnEnableChanged);

	RegConsoleCmd("nogoldknife", Command_nogoldknife, "Disables gold knife");
	RegConsoleCmd("knifemenu", Command_knifemenu, "Enables Knife Menu");
	// DEBUG RegConsoleCmd("gettime", Command_gettime, "gets connection time");
}

public OnEnableChanged(Handle:cvar, const String:oldval[], const String:newval[]) 
{
	if (strcmp(oldval, newval) != 0) {
		if (strcmp(newval, "0") == 0)
			UnhookEvent("player_hurt", Event_PlayerHurt);
		else if (strcmp(newval, "1") == 0)
			HookEvent("player_hurt", Event_PlayerHurt);
	}
}

/* DEBUG STUFF
public Action:Command_gettime(client, args)
{
	PrintToChat(client, "Connection time: %i", GetClientTime(client));
}
*/

public bool:OnClientConnect(client)
{
	CreateTimer(4.0, InitializeKnife, client, TIMER_FLAG_NO_MAPCHANGE);
	return true;
}


public Action:InitializeKnife(Handle:timer, any:client)
{
	// DEBUG PrintToChat(client, "Initializing connection: %i", GetClientTime(client));
	if(GetClientTime(client) < 1100000000) //Magic number that means you just connected (roughly)
	{
		KnifeType[client] = 1;
	}
	
}

public OnEntityCreated(entity, const String:classname[])
{
	PrintToChatAll("%s created", classname);
}

public Action:Command_nogoldknife(client, args)
{
	//PrintToChat(client, "Your \x03Special Knife\x01 has been disabled. Type '!knifemenu' to get it back.");
	KnifeType[client] = 0;
}

public Action:Command_knifemenu(client, args)
{
	if(GetAdminFlag(GetUserAdmin(client), Admin_Reservation))
	{
		/*
		PrintToChat(client, "Knife menu was broken by a recent update, sorry!");
		KnifeType[client] = 1;
		EquipKnife(client);
		*/
		
		new Handle:panel = CreatePanel();
		SetPanelTitle(panel, "Which knife would you like to use?" );
		
		DrawPanelItem(panel,"Gold");
		DrawPanelItem(panel,"Bayonet");
		DrawPanelItem(panel,"Flip");
		DrawPanelItem(panel,"Gut");
		DrawPanelItem(panel,"Karambit");
		DrawPanelItem(panel, "M9 Bayonet");
		SendPanelToClient(panel, client, KnifePanel, 5);
			
		CloseHandle(panel);
		
	}
	else
	{
		PrintToChat(client, "You must be a donator to use this command.");
	}
}

public KnifePanel(Handle:menu, MenuAction:action, param1, param2)
{
	if (action == MenuAction_Select)
	{	
		KnifeType[param1] = param2;
		EquipKnife(param1);
	}
}

/*  ===== REMOVED BECAUSE PEOPLE GET STUCK IN EACH OTHER
public Event_RoundFreezeEnd(Handle:event, const String:name[], bool:dontBroadcast) {
	ServerCommand("mp_solid_teammates 0");
	PrintToChatAll("Team Collision Disabled");
	CreateTimer(5.0, EnableCollision, TIMER_FLAG_NO_MAPCHANGE);
}

public Action:EnableCollision(Handle:timer) {
	ServerCommand("mp_solid_teammates 1");
	PrintToChatAll("Team Collision Enabled");
}
*/
public Event_PlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast){

	new client = GetClientOfUserId(GetEventInt(event, "userid"));
	
	if(GetConVarInt(g_Cvar_Enabled) > 0) {
		if(client > 0 && IsClientInGame(client))
		{
			SpawnTime[client] = GetTime();
			LastSpawnTime = GetTime();
		}
	}
	
	// DEBUG PrintToChatAll("player spawn detected");
	
	if (GetConVarInt(g_Cvar_Knife_Enabled) > 0 && GetAdminFlag(GetUserAdmin(client), Admin_Reservation)) 
	{
		if(GetClientTeam(client) == 2 || GetClientTeam(client) == 3)
		{
			PrintToChat(client, "Thank you for supporting -abM-!");
			EquipKnife(client);
		}
	}
}

public EquipKnife(any:client)
{
	// DEBUG PrintToChatAll("Equiping %i with %i", client, KnifeType[client]);
	if(KnifeType[client] > 0 && IsPlayerAlive(client))
	{
		new wepIdx;
		if ((wepIdx = GetPlayerWeaponSlot(client, 2)) != -1)
		{
			RemovePlayerItem(client, wepIdx);
			GetEntityClassname(wepIdx, newclassname, sizeof(newclassname));
			AcceptEntityInput(wepIdx, "Kill");
			PrintToChat(client, "%s removed.", newclassname);
		}
		switch(KnifeType[client])
		{
			case 1:
			{
				GivePlayerItem(client,"weapon_knifegg");
				PrintToChat(client, "\x01Enjoy your \x03Gold Knife\x01!");
			}
			case 2:
			{
				new weapon = GivePlayerItem(client, "weapon_bayonet");
				PrintToChat(client, "\x01Enjoy your \x03Bayonet\x01!");
			}
			case 3:
			{
				GivePlayerItem(client,"weapon_knife_flip");
				PrintToChat(client, "\x01Enjoy your \x03Flip Knife\x01!");
			}
			case 4:
			{
				GivePlayerItem(client,"weapon_knife_gut");
				PrintToChat(client, "\x01Enjoy your \x03Gut Knife\x01!");
			}
			case 5:
			{
				GivePlayerItem(client,"weapon_knife_karambit");
				PrintToChat(client, "\x01Enjoy your \x03Karambit\x01!");
			}
			case 6:
			{
				GivePlayerItem(client,"weapon_knife_m9_bayonet");
				PrintToChat(client, "\x01Enjoy your \x03M9 Bayonet\x01!");
			}
		}
	}
	else
	{
		PrintToChat(client, "\x01Type '!knifemenu' to enable your \x03Special Knife");				
	}
}


public Action:RestoreHealth(Handle:timer, any:victim)
{
	if(IsPlayerAlive(victim))
	{
		SetEntityHealth(victim, 100);
		PrintToChat(victim, "[Wrath of kHAN] has restored your health to 100");
	}

}

public Action:Slay(Handle:timer, any:client)
{
	if(IsPlayerAlive(client))
	{
		ForcePlayerSuicide(client);
		PrintToChatAll("[Wrath of kHAN] has slain \x03%s\x01 for attacking \x03%s\x01 in spawn!", attackername, victimname);
	}
}

public Action:Slap(Handle:timer, any:client)
{
	if(IsPlayerAlive(client))
	{
		SlapPlayer(client, GetConVarInt(g_Cvar_Damage), true);
		PrintToChatAll("[Wrath of kHAN] slapped \x03%s\x01 for attacking \x03%s\x01 in spawn!", attackername, victimname);
	}
}

public Event_PlayerHurt(Handle:event, const String:name[], bool:dontBroadcast) {
	if(GetConVarInt(g_Cvar_Enabled) > 0 && (GetTime() - LastSpawnTime) <= 14) 
	{
		new victim = GetClientOfUserId(GetEventInt(event,"userid"));
		new attacker = GetClientOfUserId(GetEventInt(event,"attacker"));
		if(attacker > 0 && IsClientInGame(attacker)) 
			if((GetTime() - SpawnTime[victim]) <= 14)
				if(IsPlayerAlive(attacker) && GetClientTeam(attacker) == GetClientTeam(victim) && victim != attacker) 
				{	
					GetClientName(attacker, attackername, sizeof(attackername));
					GetClientName(victim, victimname, sizeof(victimname));
					CreateTimer(0.0, RestoreHealth, victim, TIMER_FLAG_NO_MAPCHANGE);
					if(GetConVarInt(g_Cvar_Mode) == 0)
						CreateTimer(0.0, Slay, attacker, TIMER_FLAG_NO_MAPCHANGE);
					if(GetConVarInt(g_Cvar_Mode) == 1)
						CreateTimer(0.0, Slap, attacker, TIMER_FLAG_NO_MAPCHANGE);
				}
	}
}

