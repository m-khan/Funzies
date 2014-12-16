/*  Sourcemod Callouts version 1.0

	This is the callout mod, which allows players to talk to teammates for a short period after death.  
	
	Change notes:
	1.1: Fixed issue where muted players would be unmuted
*/

#include <sourcemod>
#include <sdktools>

#define PLUGIN_VERSION "1.0"
#define VOICE_NORMAL		0	/**< Allow the client to listen and speak normally. */
#define VOICE_MUTED			1	/**< Mutes the client from speaking to everyone. */
#define VOICE_SPEAKALL		2	/**< Allow the client to speak to everyone. */
#define VOICE_LISTENALL		4	/**< Allow the client to listen to everyone. */
#define VOICE_TEAM			8	/**< Allow the client to always speak to team, even when dead. */
#define VOICE_LISTENTEAM	16	/**< Allow the client to always hear teammates, including dead ones. */


new Handle:g_Cvar_Enabled = INVALID_HANDLE;
new Handle:g_Cvar_CalloutTime = INVALID_HANDLE;
new bool:isRoundStart;


public Plugin:myinfo =
{
	name = "Callouts",
	author = "kHAN",
	description = "Allows players a short callout period on death.",
	version = PLUGIN_VERSION,
	url = "http://abmgaming.com"
};


public OnPluginStart() 
{
	
	CreateConVar("sm_callout_version", PLUGIN_VERSION, "Version of Callout plugin", FCVAR_PLUGIN|FCVAR_SPONLY|FCVAR_REPLICATED|FCVAR_NOTIFY);
	g_Cvar_Enabled  = CreateConVar("sm_callout", "1","Enables Callout plugin",FCVAR_PLUGIN);
	g_Cvar_CalloutTime = CreateConVar("sm_callout_time","8","Players have this long to talk to teammates after death");
	
	HookEvent("round_start", Event_RoundStart);
	HookEvent("player_death", Event_PlayerDeath);
	
	HookConVarChange(g_Cvar_Enabled, OnEnableChanged)

}

public OnEnableChanged(Handle:cvar, const String:oldval[], const String:newval[]) 
{
	if (strcmp(oldval, newval) != 0) {
		if (strcmp(newval, "0") == 0)
			UnhookEvent("player_death", Event_PlayerDeath);
		else if (strcmp(newval, "1") == 0)
			HookEvent("player_death", Event_PlayerDeath);
	}
}

public Event_RoundStart(Handle:event, const String:name[], bool:dontBroadcast){
	isRoundStart = true;
	CreateTimer(GetConVarFloat(g_Cvar_CalloutTime), RoundStartTimer, TIMER_FLAG_NO_MAPCHANGE);
}

public Action:RoundStartTimer(Handle:timer)
{
	isRoundStart = false;
}

public Event_PlayerDeath(Handle:event, const String:name[], bool:dontBroadcast) {
	if(GetConVarInt(g_Cvar_Enabled) > 0) 
	{
		new victim = GetClientOfUserId(GetEventInt(event,"userid"));
		if(victim > 0 && IsClientInGame(victim) && !isRoundStart)
		{
			if(GetClientListeningFlags(victim) == VOICE_NORMAL)
			{
				SetClientListeningFlags(victim, VOICE_TEAM);
				CreateTimer(GetConVarFloat(g_Cvar_CalloutTime), CalloutTimer, victim, TIMER_FLAG_NO_MAPCHANGE);
			}
		}
	}
}

public Action:CalloutTimer(Handle:timer, any:client)
{
	SetClientListeningFlags(client, VOICE_NORMAL);
	if(!isRoundStart)
		PrintToChat(client, "Your callout time has expired. Alive teammates can no longer hear you.");
}
