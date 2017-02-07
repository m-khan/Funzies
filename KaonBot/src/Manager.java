import java.util.ArrayList;
import java.util.List;

import bwapi.Game;
import bwapi.Unit;

public interface Manager extends UnitCommander{
	public void init(Game game);
	public void handleNewUnit(Unit unit, boolean friendly);
	public void handleCompletedBuilding(Unit unit, boolean friendly);
	public String getName();
	public ArrayList<Double> claimUnits(List<Unit> unitList);
	public String getStatus();
	public double usePriority(double multiplier);
	public double incrementPriority(double priorityChange, boolean log);
	public void runFrame();
	public List<ProductionOrder> getProductionRequests();
}
