
#include <sourcemod>
#include <sdktools>
#include <sdkhooks>

#define PLUGIN_VERSION "1.0"

new warmupmode;

public Plugin:myinfo =
{
	name = "Warmup Randomizer",
	author = "kHAN",
	description = "Picks a mode for each warmup",
	version = PLUGIN_VERSION,
	url = "http://abm.servecounterstrike.com"
};

public OnPluginStart() 
{
	DisableAll();
	RegAdminCmd("sm_randommode", Command_Random_Mode, ADMFLAG_SLAY, "");
	RegAdminCmd("sm_disablemodes", Command_Disable_All, ADMFLAG_SLAY, "");
	RegAdminCmd("sm_loadfunplugins", Command_Load_Plugins, ADMFLAG_SLAY, "");
	RegAdminCmd("sm_unloadfunplugins", Command_Unload_Plugins, ADMFLAG_SLAY, "");
}

public OnMapStart()
{
	ServerCommand("exec prewarmup.cfg");
	CreateTimer(75.0, WarmupEnd, TIMER_FLAG_NO_MAPCHANGE);
}


public Action:WarmupEnd(Handle:timer)
{
	ServerCommand("exec postwarmup.cfg");
}


public Action:Command_Random_Mode(client, args){
	DisableAll();
	SetRandomMode();
}
public Action:Command_Disable_All(client, args){
	DisableAll();
}

public Action:Command_Load_Plugins(client, args){
	ServerCommand("sm plugins load dodgeball");
	ServerCommand("sm plugins load headhunter");
	ServerCommand("sm plugins load noscope");
	ServerCommand("sm plugins load scoutsknives");
	ServerCommand("sm plugins load zeus");
}

public Action:Command_Unload_Plugins(client, args){
	ServerCommand("sm plugins unload dodgeball");
	ServerCommand("sm plugins unload headhunter");
	ServerCommand("sm plugins unload noscope");
	ServerCommand("sm plugins unload scoutsknives");
	ServerCommand("sm plugins unload zeus");
}

public SetRandomMode() {
	SetRandomSeed(RoundFloat(GetEngineTime()));
	warmupmode = GetRandomInt(1,5);
	switch (warmupmode)
	{
		case 1:
		{
			ServerCommand("sm_flashDB 1");
		}
		case 2:
		{
			ServerCommand("sm_scoutzknivez 1");
			ServerCommand("sm_cvar sv_airaccelerate 200");
			ServerCommand("sv_gravity 300");
		}
		case 3:
		{
			ServerCommand("sm_zeus 1");
		}
		case 4:
		{
			ServerCommand("sm_headhunter 1");
		}
		case 5:
		{
			ServerCommand("sm_noscope 1");
			ServerCommand("sv_showimpacts 1");
			ServerCommand("sv_showimpacts_time .05");			
		}
	}
}

public DisableAll()
{
	
	if (warmupmode == 2){
		ServerCommand("sm_cvar sv_airaccelerate 12");
		ServerCommand("sv_gravity 800");
		//ServerCommand("sm_slay @all");
	}
	
	ServerCommand("sv_showimpacts 0");
	ServerCommand("sm_flashDB 0");
	ServerCommand("sm_scoutzknivez 0");
	ServerCommand("sm_zeus 0");
	ServerCommand("sm_headhunter 0");
	ServerCommand("sm_noscope 0");
	warmupmode = 0;
}
