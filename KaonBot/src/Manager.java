import java.util.ArrayList;
import java.util.List;

import bwapi.Game;
import bwapi.Unit;

public interface Manager{
	public void init(Game game);
	public void handleNewUnit(Unit unit, boolean friendly);
	public void handleCompletedBuilding(Unit unit, boolean friendly);
	public String getName();
	public ArrayList<Double> claimUnits(List<Unit> unitList);
	public void assignNewUnit(Claim claim);
	public ArrayList<Unit> assignNewUnitBehaviors();
	public String getStatus();
	public double usePriority(double multiplier);
	public double incrementPriority(double priorityChange, boolean log);
	public List<Unit> runFrame();
	public List<ProductionOrder> getProductionRequests();
	public void displayDebugGraphics(Game game);
	public List<Claim> getAllClaims();
}
