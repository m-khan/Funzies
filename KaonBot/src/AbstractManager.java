import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bwapi.Unit;

public abstract class AbstractManager implements Manager{
	private double priorityScore;
	private double baselinePriority;
	protected ArrayList<Unit> newUnits = new ArrayList<Unit>();
	protected Map<Integer, Claim> claimList = new HashMap<Integer, Claim>();
	
	public AbstractManager(double baselinePriority) {
		this.baselinePriority = baselinePriority;
		priorityScore = baselinePriority;
	}
	
	public String getName(){
		return "Manager " + this.toString();
	}
	
	public void assignNewUnit(Claim claim){
		claimList.put(claim.unit.getID(), claim);
		newUnits.add(claim.unit);
		
		// This makes sure the claim is removed if the unit is commandeered by another manager
		claim.addOnCommandeer(claim.new CommandeerRunnable(claim){
			@Override
			public void run() {
				removeClaim((Claim) arg);
			}
		});
	}
	
	private void removeClaim(Claim claim){
		claimList.remove(claim);
	}
	
	public double usePriority(double multiplier) {
		if(multiplier > 1.0)
			multiplier = 1.0; // Managers cannot request more than their current priority
		
		if(baselinePriority > priorityScore){
			priorityScore = baselinePriority; // Should I keep this?
		}
		
		return priorityScore * multiplier;
	}
	
	public double usePriority(){
		return this.usePriority(1.0);
	}

	public double incrementPriority(double priorityChange, boolean log) {
		priorityScore += priorityChange;
		return priorityScore;
	}

	public String getStatus() {
		return "PRIORITY=" + priorityScore + "/" + baselinePriority + "\nCLAIMS=" + claimList.size() + "\n";
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
	
	public List<Claim> getAllClaims(){
		ArrayList<Claim> toReturn = new ArrayList<Claim>();
		for(Integer key: claimList.keySet()){
			toReturn.add(claimList.get(key));
		}
		return toReturn;
	}
}
