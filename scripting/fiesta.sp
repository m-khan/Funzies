
#include <sourcemod>
#include <sdktools>
#include <sdkhooks>

#define PLUGIN_VERSION "1.0"

new Handle:g_Cvar_Enabled = INVALID_HANDLE;

public Plugin:myinfo =
{
	name = "Headhunter Mode",
	author = "kHAN",
	description = "Headshot only",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};

public OnPluginStart() 
{
	
	g_Cvar_Enabled  = CreateConVar("sm_fiesta", "0", "Enables Fiesta mode",FCVAR_PLUGIN);
	HookEvent("player_spawn", OnPlayerSpawn);

}

public OnPlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast){
	if(GetConVarInt(g_Cvar_Enabled) > 0) {
		new client = GetClientOfUserId(GetEventInt(event, "userid"));

		// remove weapons
		StripAllWeapons(client);
		
		// give weapons
		new weapon = GetRandomInt(1,4);
		new deagle = 0;
		switch(weapon)	
		{
			case 1:
			{
				deagle = CreateEntityByName("weapon_ump45");
			}
			case 2:
			{
				deagle = CreateEntityByName("weapon_ak47");
			}
			case 3:
			{
				deagle = CreateEntityByName("weapon_g3sg1");
			}
			case 4:
			{
				deagle = CreateEntityByName("weapon_famas");
			}
			
		}
		
		new knife = CreateEntityByName("weapon_knife");
		DispatchSpawn(deagle);
		DispatchSpawn(knife);
		EquipPlayerWeapon(client, deagle);		
		EquipPlayerWeapon(client, knife);
		
		// print mode
		PrintHintText(client, "Fiesta mode! Random weapons!");
		PrintToChat(client, "\x01\x0B\x04Fiesta mode!\x01 Random weapons!");

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