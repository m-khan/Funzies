
#include <sourcemod>
#include <sdktools>
#include <sdkhooks>

#define PLUGIN_VERSION "1.0"

public Plugin:myinfo =
{
	name = "Hide Radar",
	author = "kHAN",
	description = "See if this works",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};

public OnClientPutInServer(client) {
  SDKHook(client, SDKHook_ThinkPost, Hook_ThinkPost);
}

public Hook_ThinkPost(entity) {
  SetEntProp(entity, Prop_Send, "m_bSpotted", 0);
}  
