import java.util.ArrayList;

import bwapi.Unit;

public abstract class AbstractManager implements Manager{
	private double priorityScore;
	private double baselinePriority;
	private double degradationCoefficient;
	protected ArrayList<Unit> newUnits = new ArrayList<Unit>();
	public AbstractManager(double baselinePriority, double degradationCoefficient) {
		this.baselinePriority = baselinePriority;
		priorityScore = baselinePriority;
		this.degradationCoefficient = degradationCoefficient;
	}
	public String getName(){
		return "UnnamedManager";
	}
	public void assignNewUnit(Unit unit){
		newUnits.add(unit);
	}
	public double usePriority(double multiplier) {
		if(baselinePriority > priorityScore){
			priorityScore = baselinePriority; // Should I keep this?
			return baselinePriority;
		}
		else
		{
			double degradation = priorityScore * degradationCoefficient * multiplier;
			double priority = priorityScore;
			priorityScore -= degradation;
			return priority;
		}
	}
	public double usePriority(){
		return this.usePriority(1.0);
	}
	
	public void givePriority(double priorityIncrease) {
		priorityScore += priorityIncrease;
	}

	public String getPriorityStatus() {
		return priorityScore + "/" + baselinePriority;
	}

	public abstract class Behavior{
		private Unit unit;
		public Behavior(Unit unit){
			this.unit = unit;
		}
		public abstract boolean update();
		public Unit getUnit(){
			return unit;
		}
	}
	
}
