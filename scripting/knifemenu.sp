/*  Knife Menu version 1.1

	This plugin provides a knife menu for clients with the sourcemod reserved slot flag. 
	
	By kHAN
	
	==========Changes==========
	Version 1.1: changed panels to menu
	Version 1.2: Equips current weapon after knife is given.
	Version 1.3: cvars for enable and re-equip
	
*/

#include <sourcemod>
#include <sdktools>

#define PLUGIN_VERSION "1.1"


new KnifeType[MAXPLAYERS + 1] = {-1, ...};
new String:weaponName[64];
new Handle:g_Cvar_Enabled = INVALID_HANDLE;
new Handle:g_Cvar_Reequip = INVALID_HANDLE;

public Plugin:myinfo =
{
	name = "KnifeMenu",
	author = "kHAN",
	description = "Provides knife menu to clients with reserved slots",
	version = PLUGIN_VERSION,
	url = "http://abmgaming.com"
};


public OnPluginStart() 
{
	
	CreateConVar("sm_knifemenu_version", PLUGIN_VERSION, "Version of WRATH OF KHAN plugin", FCVAR_PLUGIN|FCVAR_SPONLY|FCVAR_REPLICATED|FCVAR_NOTIFY);
	g_Cvar_Enabled = CreateConVar("sm_knifemenu_enabled", "1", "1 = Enabled, 0 = Disabled", FCVAR_PLUGIN);
	g_Cvar_Enabled = CreateConVar("sm_knifemenu_reequip_primary", "0", "Switches weapon back to currently selected after knife is equipped, use in dm servers", FCVAR_PLUGIN);

	HookEvent("player_spawn", Event_PlayerSpawn);

	RegConsoleCmd("nogoldknife", Command_nogoldknife, "Disables gold knife");
	RegConsoleCmd("knifemenu", Command_knifemenu, "Enables Knife Menu");
}


public OnClientConnected(client)
{
	CreateTimer(4.0, InitializeKnife, client, TIMER_FLAG_NO_MAPCHANGE);
	return true;
}


public Action:InitializeKnife(Handle:timer, any:client)
{
	// DEBUG PrintToChat(client, "Initializing connection: %i", GetClientTime(client));
	if(GetClientTime(client) < 1100000000) //Magic number that means you just connected (roughly) IDK I saw it on the internet.  
	{
		KnifeType[client] = -1;
	}
	
}



public Action:Command_nogoldknife(client, args)
{
	//PrintToChat(client, "Your \x03Special Knife\x01 is available. Type '!knifemenu' to select a knife.");
	KnifeType[client] = 0;
	EquipKnife(client, true);
}

public Action:Command_knifemenu(client, args)
{
	if(GetAdminFlag(GetUserAdmin(client), Admin_Reservation))
	{
		CreateKnifeMenu(client);
	}
	else
	{
		PrintToChat(client, "You must be a donator to use this command.");
	}

}

public CreateKnifeMenu(any:client)
{
	new Handle:menu = CreateMenu(KnifeMenuHandler);
	SetMenuTitle(menu, "Knife Menu:" );
	AddMenuItem(menu,"Default" , "Default");
	AddMenuItem(menu,"Huntsman","Huntsman");
	AddMenuItem(menu,"Bayonet","Bayonet");
	AddMenuItem(menu,"Flip","Flip");
	AddMenuItem(menu,"Gut","Gut");
	AddMenuItem(menu,"Karambit","Karambit");
	AddMenuItem(menu,"Butterfly","Butterfly");
	AddMenuItem(menu,"M9 Bayonet","M9 Bayonet");
	AddMenuItem(menu,"Gold", "Gold");
	DisplayMenu(menu, client, 10);
}

public KnifeMenuHandler(Handle:menu, MenuAction:action, param1, param2)
{
	if (action == MenuAction_Select)
	{	
		//PrintToChat(param1, "You selected %i" , param2); DEBUG
		KnifeType[param1] = param2;
		EquipKnife(param1, true);
		CloseHandle(menu);
	}
	else if (action == MenuAction_End)
	{	
		CloseHandle(menu);
	}
}

public Event_PlayerSpawn(Handle:event, const String:name[], bool:dontBroadcast){

	new client = GetClientOfUserId(GetEventInt(event, "userid"));
	
	if (GetAdminFlag(GetUserAdmin(client), Admin_Reservation)) 
	{
		if(GetClientTeam(client) == 2 || GetClientTeam(client) == 3)
		{
			if(KnifeType[client] == -1)
			{
				PrintToChat(client, "Your \x03Special Knife\x01 is available. Type '!knifemenu' to select a knife.");
				KnifeType[client] = 0;
			}
			else if (KnifeType[client] != 0)
			{
				CreateTimer(0.1, EquipKnifeSpawn, client, TIMER_FLAG_NO_MAPCHANGE);
			}
		}
	}
}

public Action:EquipKnifeSpawn(Handle:timer, any:client)
{
	EquipKnife(client, false);
	
}

public EquipKnife(any:client, bool:fromMenu)
{
	// DEBUG PrintToChatAll("Equiping %i with %i", client, KnifeType[client]);
	g_Cvar_Enabled = FindConVar("sm_knifemenu_enabled");
	// DEBUGPrintToChat(client, "%i", GetConVarInt(g_Cvar_Enabled));
	if(KnifeType[client] >= 0 && IsPlayerAlive(client) && GetConVarInt(g_Cvar_Enabled) > 0)
	{
		GetClientWeapon(client, weaponName, sizeof(weaponName));
		new wepIdx;
		if ((wepIdx = GetPlayerWeaponSlot(client, 2)) != -1)
		{
			RemovePlayerItem(client, wepIdx);
			AcceptEntityInput(wepIdx, "Kill");
		}
		switch(KnifeType[client])
		{	
			case 0:
			{
				GivePlayerItem(client, "weapon_knife");
				if(fromMenu == true)
					PrintToChat(client, "Type '!knifemenu' for more options");
			}
			case 1:
			{
				new iKnifeEntity = GivePlayerItem(client, "weapon_knife_tactical");
				EquipPlayerWeapon(client, iKnifeEntity);
				FakeClientCommand(client, "use weapon_knife");
				if(fromMenu == true)
					PrintToChat(client, "\x01Enjoy your \x03Huntsman Knife\x01!");
			}
			case 2:
			{

				new iKnifeEntity = GivePlayerItem(client, "weapon_bayonet");
				EquipPlayerWeapon(client, iKnifeEntity);
				FakeClientCommand(client, "use weapon_knife");
				if(fromMenu == true)
					PrintToChat(client, "\x01Enjoy your \x03Bayonet\x01!");
			}
			case 3:
			{

				new iKnifeEntity = GivePlayerItem(client, "weapon_knife_flip");
				EquipPlayerWeapon(client, iKnifeEntity);
				FakeClientCommand(client, "use weapon_knife");
				if(fromMenu == true)
					PrintToChat(client, "\x01Enjoy your \x03Flip Knife\x01!");
			}
			case 4:
			{
				new iKnifeEntity = GivePlayerItem(client, "weapon_knife_gut");
				EquipPlayerWeapon(client, iKnifeEntity);
				FakeClientCommand(client, "use weapon_knife");
				if(fromMenu == true)
					PrintToChat(client, "\x01Enjoy your \x03Gut Knife\x01!");
			}
			case 5:
			{

				new iKnifeEntity = GivePlayerItem(client, "weapon_knife_karambit");
				EquipPlayerWeapon(client, iKnifeEntity);
				
				FakeClientCommand(client, "use weapon_knife");
				if(fromMenu == true)
					PrintToChat(client, "\x01Enjoy your \x03Karambit\x01!");
			}
			case 6:
			{
				new iKnifeEntity = GivePlayerItem(client, "weapon_knife_butterfly");
				EquipPlayerWeapon(client, iKnifeEntity);
				FakeClientCommand(client, "use weapon_knife");
				if(fromMenu == true)
					PrintToChat(client, "\x01Enjoy your \x03Butterfly Knife\x01!");
			}
			case 7:
			{
				new iKnifeEntity = GivePlayerItem(client, "weapon_knife_m9_bayonet");
				EquipPlayerWeapon(client, iKnifeEntity);
				FakeClientCommand(client, "use weapon_knife");
				if(fromMenu == true)
					PrintToChat(client, "\x01Enjoy your \x03Butterfly Knife\x01!");
			}
			case 8:
			{
				GivePlayerItem(client, "weapon_knifegg");
				if(fromMenu == true)
					PrintToChat(client, "\x01Enjoy your \x03Butterfly Knife\x01!");
			}
		}
		g_Cvar_Reequip = FindConVar("sm_knifemenu_reequip_primary");

		if(fromMenu == false && GetConVarInt(g_Cvar_Reequip) > 0)
		{
			//PrintToChat(client, "%s", weaponName);
			FakeClientCommand(client, "use %s", weaponName);
		}
	}
	else
	{
		PrintToChat(client, "\x01Your \x03Special Knife\x01 will be equipped next spawn.");				
	}
}

