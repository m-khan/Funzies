/*  WRATH OF KHAN version 1.2.1	

	This plugin is intended to discourage players from attacking other 
	teammates at the start of each round.  Any player who does this will
	be immediatly slayed.  
	
	Version 1.1: Added health restoration for victims
			   : Fixed a crash associated with shotguns
			   
	Version 1.2: Added slap as an alternative punishment
*/

#include <sourcemod>
#include <sdktools>

#define PLUGIN_VERSION "1.2.1"

new SpawnTime[MAXPLAYERS + 1] = {0, ...};
new KnifeEnabled[MAXPLAYERS + 1] = {0, ...};
new String:attackername[MAX_NAME_LENGTH + 1];
new String:victimname[MAX_NAME_LENGTH + 1];
new Handle:g_Cvar_Enabled = INVALID_HANDLE;
new Handle:g_Cvar_SpawnTime = INVALID_HANDLE;
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
	g_Cvar_SpawnTime = CreateConVar("sm_wrathofkhantime","10","Automatically punish any team attacker during this amount of seconds after spawn");
	g_Cvar_Mode = CreateConVar("sm_wrathofkhanmode","0","0 = slay, 1 = slap");
	g_Cvar_Damage = CreateConVar("sm_wrathofkhandamage" , "25" , "Sets the damage for slap mode");
	g_Cvar_Knife_Enabled = CreateConVar("sm_goldknife" , "0", "Set to 1 to enable gold knives");
	
	HookEvent("player_hurt", Event_PlayerHurt);
	HookEvent("player_spawn", Event_PlayerSpawn);
	
	HookConVarChange(g_Cvar_Enabled, OnEnableChanged)

	RegConsoleCmd("nogoldknife", Command_nogoldknife, "Disables gold knife");
	RegConsoleCmd("goldknifeme", Command_goldknifeme, "Enables gold knife");
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

public Action:Command_nogoldknife(client, args)
{
	new id = GetClientUserId(client);
	KnifeEnabled[id] = 1;
	PrintToChat(client, "Your \x03gold knife\x01 has been disabled");
}

public Action:Command_goldknifeme(client, args)
{
	new id = GetClientUserId(client);
	KnifeEnabled[id] = 0;
	PrintToChat(client, "Your \x03gold knife\x01 has been enabled");
}

public Event_PlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast){

	new id = GetEventInt(event, "userid");
	new client = GetClientOfUserId(id);
	
	if(GetConVarInt(g_Cvar_Enabled) > 0) {
		if(client > 0 && IsClientInGame(client))
			SpawnTime[client] = GetTime();
	}
	
	if (GetConVarInt(g_Cvar_Knife_Enabled) > 0 && GetAdminFlag(GetUserAdmin(client), Admin_Reservation)) 
	{
		PrintToChat(client, "Thank you for supporting -abM-!");
		if((GetClientTeam(client) == 2 || GetClientTeam(client) == 3))
		{
			if(KnifeEnabled[id] != 1)
			{
				new wepIdx;
				if ((wepIdx = GetPlayerWeaponSlot(client, 2)) != -1)
				{
					RemovePlayerItem(client, wepIdx);
					AcceptEntityInput(wepIdx, "Kill");
				}
				new knife = CreateEntityByName("weapon_knifegg");
				DispatchSpawn(knife);
				EquipPlayerWeapon(client, knife);
				PrintToChat(client, "\x01Enjoy your \x03gold knife\x01!");
			}
			else
			{
				PrintToChat(client, "\x01Your \x03gold knife\x01 has been disabled. Type 'goldknifeme' to get it back");				
			}
		}
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
	if(GetConVarInt(g_Cvar_Enabled) > 0) 
	{
		new victim = GetClientOfUserId(GetEventInt(event,"userid"));
		new attacker = GetClientOfUserId(GetEventInt(event,"attacker"));
		if(attacker > 0 && IsClientInGame(attacker)) 
			if((GetTime() - SpawnTime[victim]) <= GetConVarInt(g_Cvar_SpawnTime))
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
