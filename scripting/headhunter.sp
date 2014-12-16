
#include <sourcemod>
#include <sdktools>
#include <sdkhooks>
#include <string>

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
	
	g_Cvar_Enabled  = CreateConVar("sm_headhunter", "0", "Enables Headhunter mode",FCVAR_PLUGIN);
	HookEvent("player_spawn", OnPlayerSpawn);
	HookEvent("player_hurt", OnPlayerHurt);

}

public OnPlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast){
	if(GetConVarInt(g_Cvar_Enabled) > 0) {
		new client = GetClientOfUserId(GetEventInt(event, "userid"));
		
		// remove weapons
		StripAllWeapons(client);
		
		// give weapons
		new weapon = GetRandomInt(1,7);
		switch(weapon)
		{	
			case 1:
			{
				GivePlayerItem(client, "weapon_m4a1");
			}
			case 2:
			{
				GivePlayerItem(client, "weapon_ak47");
			}
			case 3:
			{
				GivePlayerItem(client, "weapon_aug");
			}
			case 4:
			{
				GivePlayerItem(client, "weapon_sg556");
			}
			case 5:
			{
				GivePlayerItem(client, "weapon_famas");
			}
			case 6:
			{
				GivePlayerItem(client, "weapon_galilar");
			}
			case 7:
			{
				GivePlayerItem(client, "weapon_m4a1_silencer");
			}
		}
		
		GivePlayerItem(client, "weapon_hkp2000");
		new iKnifeEntity = GivePlayerItem(client, "weapon_knife_flip");
		EquipPlayerWeapon(client, iKnifeEntity);
		FakeClientCommand(client, "use weapon_knife");
		GivePlayerItem(client, "item_kevlar");

		SetEntityHealth(client, 80);
		
		// print mode
		PrintHintText(client, "Headhunter mode! HEADSHOTS ONLY!");
		PrintToChat(client, "\x01\x0B\x04Headhunter mode!\x01 HEADSHOTS ONLY!");
	
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
			SetEntityHealth(victim, 80);
			PrintToChat(attacker, "\x01\x0B\x04Headhunter mode!\x01 HEADSHOTS ONLY!");
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