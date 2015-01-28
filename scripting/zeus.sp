
#include <sourcemod>
#include <sdktools>
#include <sdkhooks>

#define PLUGIN_VERSION "1.0"

new Handle:g_Cvar_Enabled = INVALID_HANDLE;

public Plugin:myinfo =
{
	name = "Zeus Mode",
	author = "kHAN",
	description = "Spawns everyone with tasers, rewards kills with new taser",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};


public OnPluginStart() 
{
	g_Cvar_Enabled  = CreateConVar("sm_zeus", "0", "Enables Zeus mode",FCVAR_PLUGIN);
	HookEvent("player_spawn", OnPlayerSpawn);
	HookEvent("player_death", OnPlayerDeath);

}

public OnPlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast){
	if(GetConVarInt(g_Cvar_Enabled) > 0) {
		new client = GetClientOfUserId(GetEventInt(event, "userid"));

		// remove weapons
		StripAllWeapons(client);
		
		// give weapon
		GivePlayerItem(client, "weapon_taser");

		//give knife
		new iKnifeEntity = GivePlayerItem(client, "weapon_knife_karambit");
		EquipPlayerWeapon(client, iKnifeEntity);
		FakeClientCommand(client, "use weapon_knife");
		
		// print mode
		PrintHintText(client, "Zeus mode! Get a new taser for every kill!");
		PrintToChat(client, "\x01\x0B\x04Zeus mode!\x01 Get a new taser for every kill!");
	}
}

public OnPlayerDeath(Handle:event, const String:name[], bool:dontBroadcast){
	if(GetConVarInt(g_Cvar_Enabled) > 0) {
		new killer = GetClientOfUserId(GetEventInt(event,"attacker"));
		if( killer > 0 && IsPlayerAlive(killer))
		{
			new taser = GivePlayerItem(killer, "weapon_taser");
			EquipPlayerWeapon(killer, taser);
			FakeClientCommand(killer, "use weapon_taser");
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