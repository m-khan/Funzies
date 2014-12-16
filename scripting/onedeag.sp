
#include <sourcemod>
#include <sdktools>
#include <sdkhooks>

#define PLUGIN_VERSION "1.0"

new Handle:g_Cvar_Enabled = INVALID_HANDLE;

public Plugin:myinfo =
{
	name = "1Deag Mode",
	author = "kHAN",
	description = "Spawns everyone with deagles, headshot only",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};

public OnPluginStart() 
{
	
	g_Cvar_Enabled  = CreateConVar("sm_onedeag", "0", "Enables 1Deag mode",FCVAR_PLUGIN);
	HookEvent("player_spawn", OnPlayerSpawn);
	HookEvent("player_hurt", OnPlayerHurt);

}

public OnPlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast){
	if(GetConVarInt(g_Cvar_Enabled) > 0) {
		new client = GetClientOfUserId(GetEventInt(event, "userid"));

		// remove weapons
		StripAllWeapons(client);
		
		// give weapons
		new deagle = CreateEntityByName("weapon_deagle");
		new knife = CreateEntityByName("weapon_knife");
		DispatchSpawn(deagle);
		DispatchSpawn(knife);
		EquipPlayerWeapon(client, deagle);		
		EquipPlayerWeapon(client, knife);
		
		// print mode
		PrintHintText(client, "1Deag mode! HEADSHOTS ONLY!");
		PrintToChat(client, "\x01\x0B\x041Deag mode!\x01 HEADSHOTS ONLY!");

	}
}

public OnPlayerHurt(Handle:event, const String:name[], bool:dontBroadcast)
{
	if(GetConVarInt(g_Cvar_Enabled) > 0) {
		new victim = GetClientOfUserId(GetEventInt(event,"userid"));
		new attacker = GetClientOfUserId(GetEventInt(event, "attacker"));
		new hitbox = GetEventInt(event, "hitgroup");
		if(hitbox != 1)
		{
			SetEntityHealth(victim, 100);
			PrintToChat(attacker, "\x01\x0B\x041Deag mode!\x01 HEADSHOTS ONLY!");
		}	
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