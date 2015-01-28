/*  WRATH OF KHAN version 3.0

	This is a simplified version of WoK
	
	Version 3.0
		-Moved knife menu to it's own plugin (knifemenu.sp)
		-Changed trigger to be based off round start instead of individual player spawns.  
	
*/

#include <sourcemod>
#include <sdktools>

#define PLUGIN_VERSION "3.0"

new String:attackername[MAX_NAME_LENGTH + 1];
new String:victimname[MAX_NAME_LENGTH + 1];
new Handle:g_Cvar_Enabled = INVALID_HANDLE;
new Handle:g_Cvar_SpawnTime = INVALID_HANDLE;
new Handle:g_Cvar_Mode = INVALID_HANDLE;
new Handle:g_Cvar_Damage = INVALID_HANDLE;
new bool:isRoundStart;


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
	g_Cvar_SpawnTime = CreateConVar("sm_wrathofkhantime","14","Automatically punish any team attacker during this amount of seconds after round start");
	g_Cvar_Mode = CreateConVar("sm_wrathofkhanmode","1","0 = slay, 1 = slap (default)");
	g_Cvar_Damage = CreateConVar("sm_wrathofkhandamage" , "24" , "Sets the damage for slap mode");
	
	HookEvent("player_hurt", Event_PlayerHurt);
	HookEvent("round_start", Event_RoundStart);
	
	HookConVarChange(g_Cvar_Enabled, OnEnableChanged)

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

public Event_RoundStart(Handle:event, const String:name[], bool:dontBroadcast){
	isRoundStart = true;
	CreateTimer(GetConVarFloat(g_Cvar_SpawnTime), RoundStartTimer, TIMER_FLAG_NO_MAPCHANGE);
}

public Action:RoundStartTimer(Handle:timer)
{
	isRoundStart = false;
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
		if(attacker > 0 && IsClientInGame(attacker) && isRoundStart) 
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
