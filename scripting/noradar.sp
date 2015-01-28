#include <sourcemod> 
#include <sdktools>

#define HIDEHUD_RADAR 1 << 12
#define SHOWHUD_RADAR 1 >> 12

public OnPluginStart() 
{ 
	HookEvent("player_spawn", Event_PlayerSpawn);
	HookEvent("player_death", Event_PlayerDeath);
} 

public Event_PlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast)
{
	new client = GetClientOfUserId(GetEventInt(event, "userid"));
	CreateTimer(0.0, RemoveRadar, client);
}

public Event_PlayerDeath(Handle:event, const String:name[], bool:dontBroadcast)
{
	new client = GetClientOfUserId(GetEventInt(event, "userid"));
	CreateTimer(0.0, ShowRadar, client);
}

public Action:RemoveRadar(Handle:timer, any:client)
{
	SetEntProp(client, Prop_Send, "m_iHideHUD", HIDEHUD_RADAR);
}

public Action:ShowRadar(Handle:timer, any:client)
{
	SetEntProp(client, Prop_Send, "m_iHideHUD", SHOWHUD_RADAR);
}