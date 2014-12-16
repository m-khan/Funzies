
#include <sourcemod>
#include <sdktools>
#include <sdkhooks>

#define PLUGIN_VERSION "1.0"

public Plugin:myinfo =
{
	name = "Silencers??!?!?!?",
	author = "kHAN",
	description = "Test plugin lol",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};


public OnPluginStart() 
{
	RegAdminCmd("sm_silencer", Command_Silencer, ADMFLAG_CHANGEMAP, "Silencer?");
}

public Action:Command_Silencer(client, args){
	new Handle:event = CreateEvent("silencer_on", true);
	
	SetEventInt(event, "userid", GetClientOfUserId(client));
	FireEvent(event,true);
	return Plugin_Handled;
}
