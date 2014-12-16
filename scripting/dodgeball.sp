
#include <sourcemod>
#include <sdktools>
#include <sdkhooks>
#include <cstrike>

#define PLUGIN_VERSION "1.0"

new Handle:g_Cvar_Dodgeball = INVALID_HANDLE;
new Handle:g_Cvar_Dodgeball_Spawntime = INVALID_HANDLE;

public Plugin:myinfo =
{
	name = "Flashbang Dodgeball",
	author = "kHAN",
	description = "Spawns everyone with 1 hp and infinite flashbangs for dodgeball funtimes",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};


public OnPluginStart() 
{
	
	CreateConVar("sm_dodgeball", PLUGIN_VERSION, "Version of Flashbang Dodgeball plugin", FCVAR_PLUGIN|FCVAR_SPONLY|FCVAR_REPLICATED|FCVAR_NOTIFY);
	g_Cvar_Dodgeball  = CreateConVar("sm_flashDB", "0","Enables Dodgeball Mode",FCVAR_PLUGIN);
	g_Cvar_Dodgeball_Spawntime = CreateConVar("sm_DBFlashtime", "1.4", "Travel time before flashbang despawns", FCVAR_PLUGIN);
	
	HookEvent("player_spawn", OnPlayerSpawn);
	HookEvent("player_death", OnPlayerDeath);

}

public OnPlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast){
	if(GetConVarInt(g_Cvar_Dodgeball) > 0) {
		new client = GetClientOfUserId(GetEventInt(event, "userid"));

		// Set hp to 1
		SetEntityHealth(client, 1);
		
		// remove weapons
		StripAllWeapons(client);
				
		// give flashbangs

		//GivePlayerItem(client, "weapon_flashbang");
		GivePlayerItem(client, "weapon_flashbang");
		GivePlayerItem(client, "weapon_decoy");
		
		/*
		new iKnifeEntity = GivePlayerItem(client, "weapon_knife_flip");
		EquipPlayerWeapon(client, iKnifeEntity);
		FakeClientCommand(client, "use weapon_knife");
		*/
		
		PrintHintText(client, "Dodgeball mode! Kill your enemies by hitting them with flashbangs!");
		PrintToChat(client, "\x01\x0B\x04Dodgeball mode!\x01 Kill your enemies by hitting them with flashbangs!");
	}
}

public OnPlayerDeath(Handle:event, const String:name[], bool:dontBroadcast){
	if(GetConVarInt(g_Cvar_Dodgeball) > 0) {
		new killer = GetClientOfUserId(GetEventInt(event,"attacker"));
		if( killer > 0  && IsPlayerAlive(killer))
		{
			GivePlayerItem(killer, "weapon_decoy");
		}
	}
}

public OnEntityCreated(entity, const String:classname[])
{
	if (StrEqual(classname, "flashbang_projectile") && GetConVarInt(g_Cvar_Dodgeball))
	{
		SDKHook(entity, SDKHook_Spawn, OnEntitySpawned);
	}
}

public OnEntitySpawned(entity)
{
	CreateTimer(0.0, Timer_RemoveThinkTick, entity, TIMER_FLAG_NO_MAPCHANGE);
}

public Action:Timer_RemoveThinkTick(Handle:timer, any:entity)
{
	SetEntProp(entity, Prop_Data, "m_nNextThinkTick", -1);
	CreateTimer(GetConVarFloat(g_Cvar_Dodgeball_Spawntime), Timer_RemoveFlashbang, entity, TIMER_FLAG_NO_MAPCHANGE);
}

public Action:Timer_RemoveFlashbang(Handle:timer, any:entity)
{
	if (IsValidEntity(entity))
	{
		new client = GetEntPropEnt(entity, Prop_Data, "m_hOwnerEntity");
		AcceptEntityInput(entity, "Kill");
		
		if ((client != -1) && IsClientInGame(client) && IsPlayerAlive(client))
		{
			GivePlayerItem(client, "weapon_flashbang");
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