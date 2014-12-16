#include <sourcemod>
#include <smlib>
#include "include/multi1v1.inc"

public Plugin:myinfo = {
    name = "CS:GO Multi1v1: knife round addon",
    author = "splewis",
    description = "Adds a knife-round option when both playes in an arena allow it",
    version = "1.0",
    url = "https://github.com/splewis/csgo-multi-1v1"
};

public Multi1v1_OnRoundTypesAdded() {
    Multi1v1_AddRoundType("Knife", "knife", KnifeHandler, Multi1v1_NullChoiceMenu, true, true, "");
}

public void KnifeHandler(int client) {
    Client_RemoveAllWeapons(client, "", true);
    Client_SetArmor(client, 100);
    GivePlayerItem(client, "weapon_knife");
}