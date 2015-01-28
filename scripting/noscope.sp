
#include <sourcemod>
#include <sdktools>
#include <sdkhooks>

#define PLUGIN_VERSION "1.0"

new Handle:g_Cvar_Enabled = INVALID_HANDLE;

public Plugin:myinfo =
{
	name = "Noscope Mode",
	author = "kHAN",
	description = "Spawns everyone with Scouts and disables zooming",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};


public OnPluginStart() 
{
	g_Cvar_Enabled  = CreateConVar("sm_noscope", "0", "Enables Noscope mode",FCVAR_PLUGIN);
	HookEvent("player_spawn", OnPlayerSpawn);
	HookEvent("weapon_zoom", OnWeaponZoom, EventHookMode_Pre);
}

public OnPlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast){
	new client = GetClientOfUserId(GetEventInt(event, "userid"));
	if(GetConVarInt(g_Cvar_Enabled) > 0) {

		// remove weapons
		StripAllWeapons(client);

		// give weapon
		GivePlayerItem(client, "weapon_ssg08");
		
		//give knife
		new iKnifeEntity = GivePlayerItem(client, "weapon_bayonet");
		EquipPlayerWeapon(client, iKnifeEntity);
		FakeClientCommand(client, "use weapon_knife");
		

		SetEntityHealth(client, 60);
		
		// print mode
		PrintHintText(client, "Noscope mode! No zooming allowed!");
		PrintToChat(client, "\x01\x0B\x04Noscope mode!\x01 No zooming allowed!");
	}
}

public OnWeaponZoom(Handle:event, const String:name[], bool:dontBroadcast) {
	if(GetConVarInt(g_Cvar_Enabled) > 0) {
		new client = GetClientOfUserId(GetEventInt(event, "userid"));
		if( IsPlayerAlive(client) )
		{
			// remove weapons
			StripAllWeapons(client);

			// give weapon
			GivePlayerItem(client, "weapon_ssg08");
			
			//give knife
			new iKnifeEntity = GivePlayerItem(client, "weapon_knife_karambit");
			EquipPlayerWeapon(client, iKnifeEntity);
			FakeClientCommand(client, "use weapon_knife");
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