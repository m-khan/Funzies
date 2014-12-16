#include <sourcemod>
#include <sdktools>

#define PLUGIN_VERSION "1.0"

new String:Killstreaker[MAX_NAME_LENGTH + 1];
new Killstreak[MAXPLAYERS + 1] = {0, ...};
new Handle:g_Cvar_Enabled = INVALID_HANDLE;
new Handle:g_Cvar_Kill_Reward = INVALID_HANDLE;
new Handle:g_Cvar_Headshot_Bonus = INVALID_HANDLE;
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

public Event_PlayerDeath(Handle:event, const String:name[], bool:dontBroadcast)
{
    new attacker = GetClientOfUserId(GetEventInt(event, "attacker"));
    new victim = GetClientOfUserId(GetEventInt(event, "userid"));
    new hsBonus = GetEventBool(event, "headshot");

    Killstreak[victim] = 0;
	
	//CreateTimer(0.0, NormalizeSpeed, victim, TIMER_FLAG_NO_MAPCHANGE);

    if (IsValidKill(attacker, victim))
    {
        Killstreak[attacker]++;
        //PrintToChat(attacker, "Killstreak: %i", Killstreak[attacker]);
        if(hsBonus > 0)
		{
			CreateTimer(0.0, RewardHeadshot, attacker, TIMER_FLAG_NO_MAPCHANGE);
		}
		else
		{
			CreateTimer(0.0, RewardKill, attacker, TIMER_FLAG_NO_MAPCHANGE);
		}
		CreateTimer(0.0, RewardKillStreak, attacker, TIMER_FLAG_NO_MAPCHANGE);
	}
}

public Action:RestoreArmor(Handle:timer, any:client)
{
	SetEntData(client, g_Armor, 100, 4, true);
}


IsValidKill(attacker, victim)
{
    return victim != 0 && attacker != 0 && victim != attacker && GetClientTeam(victim) != GetClientTeam(attacker);
}

public Action:RewardKillStreak(Handle:timer, any:client)
{
	switch(Killstreak[client]%9)
	{
		case 3:
		{
			new grenade = CreateEntityByName("weapon_hegrenade");
			DispatchSpawn(grenade);
			EquipPlayerWeapon(client, grenade);
			PrintToChat(client, "[\x04Grenade\x01] 3 Kill Streak");
			if(Killstreak[client] > 3)
			{
				GetClientName(client, Killstreaker, sizeof(Killstreaker));
				PrintToChatAll("\x03%s\x01 Is on a %i Killstreak!", Killstreaker, Killstreak[client]);
			}
		}
		
		case 6:
		{
			GetClientName(client, Killstreaker, sizeof(Killstreaker));
			PrintToChatAll("\x03%s\x01 Is on a %i Killstreak!", Killstreaker, Killstreak[client]);
			new grenade = CreateEntityByName("weapon_smokegrenade");
			DispatchSpawn(grenade);
			EquipPlayerWeapon(client, grenade);
			PrintToChat(client, "[\x04Smoke\x01] 6 Kill Streak");
			
			/*
			SetEntDataFloat(client, OffsetMovement, 1.5);
			PrintToChat(client, "[\x04Speed Boost\x01] 6 Kill Streak");
			CreateTimer(10.0, NormalizeSpeed, client, TIMER_FLAG_NO_MAPCHANGE);
			*/
		}
		case 0:
		{
			if(Killstreak[client] != 0)
			{	
				GetClientName(client, Killstreaker, sizeof(Killstreaker));
				PrintToChatAll("\x03%s\x01 Is on a %i Killstreak!", Killstreaker, Killstreak[client]);
				new grenade = CreateEntityByName("weapon_molotov");
				DispatchSpawn(grenade);
				EquipPlayerWeapon(client, grenade);
				PrintToChat(client, "[\x04Molotov\x01] 9 Kill Streak");
			}
		}
	}
}

/*
public Action:NormalizeSpeed(Handle:timer, any:client)
{
	SetEntDataFloat(client, OffsetMovement, 1.0);
}
*/

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
		PrintToChat(client, "\x05%i:\x01[\x04+%i HP\x01] Headshot", Killstreak[client], reward);
	}
}

