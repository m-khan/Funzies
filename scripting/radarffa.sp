
#include <sourcemod>
#include <sdktools>
#include <sdkhooks>

#define PLUGIN_VERSION "1.0"
#define HIDEHUD_RADAR 1 << 12
#define SHOWHUD_RADAR 1 >> 12

new Handle:g_Cvar_RadarOffTime = INVALID_HANDLE;
new Handle:g_Cvar_RadarOnTime = INVALID_HANDLE;
new TimerStarted[MAXPLAYERS + 1] = {0, ...};

public Plugin:myinfo =
{
	name = "RadarFFA",
	author = "kHAN",
	description = "Radar 'pulse' mode for FFA",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};

public OnPluginStart()
{
	g_Cvar_RadarOffTime  = CreateConVar("sm_radar_pulse_off", "2.0","Time for radar off",FCVAR_PLUGIN);
	g_Cvar_RadarOnTime  = CreateConVar("sm_radar_pulse_on", "1.0","Time for radar on",FCVAR_PLUGIN);	
	HookEvent("player_spawn", Event_PlayerSpawn);
}

public Event_PlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast)
{
	new client = GetClientOfUserId(GetEventInt(event, "userid"));
	if(TimerStarted[client] == 0)
	{
		CreateTimer(0.0, HideRadar, client);
		TimerStarted[client] = 1;
	}
}

public Action:HideRadar(Handle:timer, any:client)
{
	if(IsClientConnected(client))
	{
		SetEntProp(client, Prop_Send, "m_iHideHUD", HIDEHUD_RADAR);
		CreateTimer(GetConVarFloat(g_Cvar_RadarOffTime), ShowRadar, client, TIMER_FLAG_NO_MAPCHANGE);
	}
	else
	{
		TimerStarted[client] = 0;
	}
}

public Action:ShowRadar(Handle:timer, any:client)
{
	SetEntProp(client, Prop_Send, "m_iHideHUD", SHOWHUD_RADAR);
	CreateTimer(GetConVarFloat(g_Cvar_RadarOnTime), HideRadar, client, TIMER_FLAG_NO_MAPCHANGE);
}




