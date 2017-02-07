import java.util.List;

import bwapi.Game;

public interface UnitCommander {
	public void assignNewUnit(Claim claim);
	public void assignNewUnitBehaviors();
	public List<Claim> getAllClaims();
	public void freeUnits();
	public String getName();
	public void displayDebugGraphics(Game game);
}
