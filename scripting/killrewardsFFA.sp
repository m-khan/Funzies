#include <sourcemod>
#include <sdktools>

#define PLUGIN_VERSION "1.1"
#define HIDEHUD_RADAR 1 << 12
#define SHOWHUD_RADAR 1 >> 12

new String:Killstreaker[MAX_NAME_LENGTH + 1];
new Killstreak[MAXPLAYERS + 1] = {0, ...};
new SpeedBoostBuffer[MAXPLAYERS + 1] = {0, ...};
new Handle:g_Cvar_Enabled = INVALID_HANDLE;
new Handle:g_Cvar_Kill_Reward = INVALID_HANDLE;
new Handle:g_Cvar_Headshot_Bonus = INVALID_HANDLE;
new Handle:g_Cvar_Assist_Reward = INVALID_HANDLE;
new Handle:g_Cvar_Awp_Reward = INVALID_HANDLE;
new Handle:g_Cvar_Knife_Reward = INVALID_HANDLE;
new g_Armor; //new g_iHealth
new OffsetMovement;
public Plugin:myinfo =
{
	name = "Kill Rewards",
	author = "kHAN",
	description = "Rewards kills with health and items",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};


public OnPluginStart() 
{
	
	CreateConVar("sm_killrewardsversion", PLUGIN_VERSION, "Version of Kill Rewards plugin", FCVAR_PLUGIN|FCVAR_SPONLY|FCVAR_REPLICATED|FCVAR_NOTIFY);
	g_Cvar_Enabled  = CreateConVar("sm_killrewards_enable", "1","Enables Kill Rewards plugin",FCVAR_PLUGIN);
	g_Cvar_Kill_Reward = CreateConVar("sm_killhealth", "10", "Health Reward for normal kill", FCVAR_PLUGIN);
	g_Cvar_Headshot_Bonus = CreateConVar("sm_headshotbonus", "15", "Health Reward for headshot kill", FCVAR_PLUGIN);	
	g_Cvar_Assist_Reward = CreateConVar("sm_assisthealth", "5", "Health Reward for kill assist", FCVAR_PLUGIN);
	g_Cvar_Awp_Reward = CreateConVar("sm_awphealth", "5", "Health Reward for awp kill", FCVAR_PLUGIN);
	g_Cvar_Knife_Reward = CreateConVar("sm_knifehealth", "100", "Health Reward for awp/auto kill", FCVAR_PLUGIN);
	
	HookEvent("player_spawn", Event_PlayerSpawn);
	HookEvent("player_death", Event_PlayerDeath);
	//HookEvent("weapon_reload", Event_WeaponReload);
	HookConVarChange(g_Cvar_Enabled, OnEnableChanged)
	OffsetMovement = FindSendPropOffs("CBasePlayer", "m_flLaggedMovementValue");
	if(OffsetMovement == -1)
    {
        SetFailState("FATAL ERROR OffsetMovement");
    }	

	g_Armor = FindSendPropOffs("CCSPlayer", "m_ArmorValue");
	if (g_Armor == -1)
	{
		SetFailState("[Headshot Only] Error - Unable to get offset for CSSPlayer::m_ArmorValue");
	}
	/*=============
	g_iHealth = FindSendPropOffs("CCSPlayer", "m_iHealth");
	if (g_iHealth == -1)
	{
		SetFailState("[Headshot Only] Error - Unable to get offset for CSSPlayer::m_iHealth");
	}
	=============*/

}

public OnMapEnd()
{
	for(new i = 0; i <= MAXPLAYERS; i++)
	{	
		Killstreak[i] = 0;
	}
}

public OnEnableChanged(Handle:cvar, const String:oldval[], const String:newval[]) 
{
	if (strcmp(oldval, newval) != 0) {
		if (strcmp(newval, "0") == 0)
			UnhookEvent("player_death", Event_PlayerDeath);
		else if (strcmp(newval, "1") == 0)
			HookEvent("player_death", Event_PlayerDeath);
	}
}

public Event_PlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast)
{
	new client = GetClientOfUserId(GetEventInt(event, "userid"));
	CreateTimer(0.0, RemoveRadar, client);
}

public Action:RemoveRadar(Handle:timer, any:client)
{
	SetEntProp(client, Prop_Send, "m_iHideHUD", HIDEHUD_RADAR);
}

/*
public Action:ShowRadar(Handle:timer, any:client)
{
	SetEntProp(client, Prop_Send, "m_iHideHUD", SHOWHUD_RADAR);
}
*/

public Event_PlayerDeath(Handle:event, const String:name[], bool:dontBroadcast)
{
	new attacker = GetClientOfUserId(GetEventInt(event, "attacker"));
	new victim = GetClientOfUserId(GetEventInt(event, "userid"));
	new assister = GetClientOfUserId(GetEventInt(event, "assister"));
	new String:killweapon[MAX_NAME_LENGTH + 1]; 
	GetEventString(event, "weapon", killweapon, MAX_NAME_LENGTH + 1);
	new hsBonus = GetEventBool(event, "headshot");

	SpeedBoostBuffer [victim] = 0;
	Killstreak[victim] = 0;

	//CreateTimer(0.0, NormalizeSpeed, victim, TIMER_FLAG_NO_MAPCHANGE);

	if (IsValidKill(attacker, victim))
    {
		//PrintToChatAll("Assister: %i", assister);
		//PrintToChatAll(killweapon);

		Killstreak[attacker]++;
		//PrintToChat(attacker, "Killstreak: %i", Killstreak[attacker]);
		if(hsBonus > 0)
		{
			CreateTimer(0.0, RewardHeadshot, attacker, TIMER_FLAG_NO_MAPCHANGE);
		}
		else if (strcmp(killweapon, "awp") == 0 || strcmp(killweapon, "scar20") == 0 || strcmp(killweapon, "g3sg1") == 0)
		{
			CreateTimer(0.0, RewardAwpKill, attacker, TIMER_FLAG_NO_MAPCHANGE);
		}
		else if (strncmp(killweapon, "knife", 5) == 0)
		{
			CreateTimer(0.0, RewardKnifeKill, attacker, TIMER_FLAG_NO_MAPCHANGE);
		}
		else
		{
			CreateTimer(0.0, RewardKill, attacker, TIMER_FLAG_NO_MAPCHANGE);
		}
		CreateTimer(0.0, RewardKillStreak, attacker, TIMER_FLAG_NO_MAPCHANGE);
		
		if(assister != 0)
		{
			CreateTimer(0.0, RewardAssist, assister, TIMER_FLAG_NO_MAPCHANGE);
		}
	}
}

public Action:RestoreArmor(Handle:timer, any:client)
{
	SetEntData(client, g_Armor, 100, 4, true);
}


IsValidKill(attacker, victim)
{
    return victim != 0 && attacker != 0 && victim != attacker;
}

public Action:RewardKillStreak(Handle:timer, any:client)
{
	switch(Killstreak[client]%8)
	{
		case 2:
		{
			new grenade = CreateEntityByName("weapon_hegrenade");
			DispatchSpawn(grenade);
			EquipPlayerWeapon(client, grenade);
			PrintToChat(client, "[\x04Grenade\x01] 2 Kill Streak");
			if(Killstreak[client] > 3)
			{
				GetClientName(client, Killstreaker, sizeof(Killstreaker));
				PrintToChatAll("\x03%s\x01 Is on a %i Killstreak!", Killstreaker, Killstreak[client]);
			}
		}
		case 4:
		{
			GetClientName(client, Killstreaker, sizeof(Killstreaker));
			PrintToChatAll("\x03%s\x01 Is on a %i Killstreak!", Killstreaker, Killstreak[client]);
			GivePlayerItem(client, "weapon_flashbang");
			GivePlayerItem(client, "weapon_flashbang");
			PrintToChat(client, "[\x042 Flashbangs\x01] 4 Kill Streak");
			
		}
		case 6:
		{
			GetClientName(client, Killstreaker, sizeof(Killstreaker));
			PrintToChatAll("\x03%s\x01 Is on a %i Killstreak!", Killstreaker, Killstreak[client]);
			GivePlayerItem(client, "weapon_hegrenade");
			//CreateTimer(0.0, ShowRadar, client);
			PrintToChat(client, "[\x04Grenade\x01] 6 Kill Streak");
			
		}
		case 0:
		{
			if(Killstreak[client] != 0)
			{	
				GetClientName(client, Killstreaker, sizeof(Killstreaker));
				PrintToChatAll("\x03%s\x01 Is on a %i Killstreak!", Killstreaker, Killstreak[client]);
				GivePlayerItem(client, "weapon_molotov");

				PrintToChat(client, "[\x04Molotov\x01] 8 Kill Streak");
			}
		}
	}
}

public Action:RewardAssist(Handle:timer, any:client)
{
	if(IsPlayerAlive(client))
	{	new health = GetClientHealth(client);
		new reward = GetConVarInt(g_Cvar_Assist_Reward);
		if(health + reward > 100)
		{
			SetEntityHealth(client, 100);
		}
		else
		{
			SetEntityHealth(client, (health + reward));
		}
		PrintToChat(client, "\x05%i:\x01[\x04+%i HP\x01] Assist", Killstreak[client], reward);
	}
}

public Action:RewardKill(Handle:timer, any:client)
{
	if(IsPlayerAlive(client))
	{	new health = GetClientHealth(client);
		new reward = GetConVarInt(g_Cvar_Kill_Reward);
		if(health + reward > 100)
		{
			SetEntityHealth(client, 100);
		}
		else
		{
			SetEntityHealth(client, (health + reward));
		}
		PrintToChat(client, "\x05%i:\x01[\x04+%i HP\x01] Kill", Killstreak[client], reward);
	}
}

public Action:RewardAwpKill(Handle:timer, any:client)
{
	if(IsPlayerAlive(client))
	{	new health = GetClientHealth(client);
		new reward = GetConVarInt(g_Cvar_Awp_Reward);
		if(health + reward > 100)
		{
			SetEntityHealth(client, 100);
		}
		else
		{
			SetEntityHealth(client, (health + reward));
		}
		PrintToChat(client, "\x05%i:\x01[\x04+%i HP\x01] Awp/Auto Kill", Killstreak[client], reward);
	}
}

public Action:RewardKnifeKill(Handle:timer, any:client)
{
	if(IsPlayerAlive(client))
	{	new health = GetClientHealth(client);
		new reward = GetConVarInt(g_Cvar_Knife_Reward);
		if(health + reward > 100)
		{
			SetEntityHealth(client, 100);
		}
		else
		{
			SetEntityHealth(client, (health + reward));
		}
		SetEntDataFloat(client, OffsetMovement, 1.5);
		PrintToChat(client, "[\x04Speed Boost\x01] Knife Kill");
		CreateTimer(10.0, NormalizeSpeed, client, TIMER_FLAG_NO_MAPCHANGE);
		SpeedBoostBuffer[client]++;
		PrintToChat(client, "\x05%i:\x01[\x04+%i HP\x01] Knife Kill", Killstreak[client], reward);
	}
}

public Action:NormalizeSpeed(Handle:timer, any:client)
{
	if(--SpeedBoostBuffer[client] < 1)
	{
		SpeedBoostBuffer[client] = 0;
		SetEntDataFloat(client, OffsetMovement, 1.0);
	}
}

public Action:RewardHeadshot(Handle:timer, any:client)
{
	if(IsPlayerAlive(client))
	{	new health = GetClientHealth(client);
		new reward = GetConVarInt(g_Cvar_Headshot_Bonus) + GetConVarInt(g_Cvar_Kill_Reward);
		if(health + reward > 100)
		{
			SetEntityHealth(client, 100);
		}
		else
		{
			SetEntityHealth(client, (health + reward));
		}
		PrintToChat(client, "\x05%i:\x01[\x04+%i HP & Ammo\x01] Headshot", Killstreak[client], reward);
	}
}

