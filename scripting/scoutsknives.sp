
#include <sourcemod>
#include <sdktools>
#include <sdkhooks>

#define PLUGIN_VERSION "1.0"

new Handle:g_Cvar_Enabled = INVALID_HANDLE;
new OffsetMovement;

public Plugin:myinfo =
{
	name = "Quake Mode",
	author = "kHAN",
	description = "Spawns everyone with shotguns, gives them accelerated movement",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};


public OnPluginStart() 
{
	g_Cvar_Enabled  = CreateConVar("sm_scoutzknivez", "0", "Enables Quake mode",FCVAR_PLUGIN);
	HookEvent("player_spawn", OnPlayerSpawn);
	OffsetMovement = FindSendPropOffs("CBasePlayer", "m_flLaggedMovementValue");
	if(OffsetMovement == -1)
    {
        SetFailState("FATAL ERROR OffsetMovement");
    }	

	
}

public OnPlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast){
	if(GetConVarInt(g_Cvar_Enabled) > 0) {
		new client = GetClientOfUserId(GetEventInt(event, "userid"));

		// remove weapons
		StripAllWeapons(client);
		
		// give weapon
		GivePlayerItem(client, "weapon_nova");		
		
		//give knife
		new iKnifeEntity = GivePlayerItem(client, "weapon_knife_tactical");
		EquipPlayerWeapon(client, iKnifeEntity);
		FakeClientCommand(client, "use weapon_knife");
		
		//Give super speed
		SetEntDataFloat(client, OffsetMovement, 1.8);
		
		// print mode
		PrintHintText(client, "Quake mode! M-M-M-MONSTER KILL!");
		PrintToChat(client, "\x01\x0B\x04Quake mode!\x01 M-M-M-MONSTER KILL!");
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