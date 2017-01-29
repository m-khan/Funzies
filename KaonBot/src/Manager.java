import java.util.ArrayList;
import java.util.List;

import bwapi.Unit;


public interface Manager{
	public void handleNewUnit(Unit unit);
	public String getName();
	public ArrayList<Double> claimUnits(List<Unit> unitList);
	public void assignNewUnit(Unit unit);
	public ArrayList<Unit> assignNewUnitBehaviors();
	public String getPriorityStatus();
	public double usePriority(double multiplier);
	public void givePriority(double priorityIncrease);
	public List<Unit> runFrame();
}
