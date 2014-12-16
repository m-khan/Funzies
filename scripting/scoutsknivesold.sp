
#include <sourcemod>
#include <sdktools>
#include <sdkhooks>

#define PLUGIN_VERSION "1.0"

new Handle:g_Cvar_Enabled = INVALID_HANDLE;
new Handle:g_Cvar_Gravity = INVALID_HANDLE;

public Plugin:myinfo =
{
	name = "Scoutzknivez Mode",
	author = "kHAN",
	description = "Spawns everyone with a scout and low gravity",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};


public OnPluginStart() 
{
	g_Cvar_Enabled  = CreateConVar("sm_scoutzknivez", "0", "Enables Scoutzknivez mode",FCVAR_PLUGIN);
	g_Cvar_Gravity = CreateConVar("sm_scoutzknivez_gravity", "0.25", "Gravity for scoutzknives mode", FCVAR_PLUGIN);
	HookEvent("player_spawn", OnPlayerSpawn);
}

public OnPlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast){
	new client = GetClientOfUserId(GetEventInt(event, "userid"));
	if(GetConVarInt(g_Cvar_Enabled) > 0) {

		// Set low gravity
		SetEntityGravity(client, GetConVarFloat(g_Cvar_Gravity));
		
		// remove weapons
		StripAllWeapons(client);
		
		// give weapons
		new scout = CreateEntityByName("weapon_ssg08");
		new knife = CreateEntityByName("weapon_knife");
		DispatchSpawn(scout);
		DispatchSpawn(knife);
		EquipPlayerWeapon(client, scout);		
		EquipPlayerWeapon(client, knife);
		
		// print mode
		PrintHintText(client, "Scoutzknivez mode! Scouts, Knives, and Low Gravity!");
		PrintToChat(client, "\x01\x0B\x04Scoutzknivez mode!\x01 Scouts, Knives, and Low Gravity!");
	}
	else {
		SetEntityGravity(client, 1.0);
	}
}

public StripAllWeapons(client)
{
	new wepIdx;
	for (new i; i < 4; i++)
	{
		if ((wepIdx = GetPlayerWeaponSlot(client, i)) != -1)
		{
			RemovePlayerItem(client, wepIdx);
			AcceptEntityInput(wepIdx, "Kill");
		}
	}
}