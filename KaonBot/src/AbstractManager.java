import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bwapi.Color;
import bwapi.Game;
import bwapi.Unit;

public abstract class AbstractManager implements Manager{
	private double priorityScore;
	private double baselinePriority;
	private double volitilityScore;
	protected ArrayList<Unit> newUnits = new ArrayList<Unit>();
	protected Map<Integer, Claim> claimList = new HashMap<Integer, Claim>();
	private Color debugColor;
	
	public AbstractManager(double baselinePriority, double volitilityScore) {
		this.baselinePriority = baselinePriority;
		priorityScore = baselinePriority;
		this.volitilityScore = volitilityScore;
		debugColor = KaonUtils.getRandomColor();
	}
	
	@Override
	public String getName(){
		return "Manager " + this.toString();
	}
	
	@Override
	public void assignNewUnit(Claim claim){
		claimList.put(claim.unit.getID(), claim);
		newUnits.add(claim.unit);
		
		// This makes sure the claim is removed if the unit is commandeered by another manager
		claim.addOnCommandeer(claim.new CommandeerRunnable(){
			@Override
			public void run() {
				System.out.println(getName() + " releasing " + claim.unit.getID());
				removeClaim(claim);
			}
		});
		addCommandeerCleanup(claim);
	}
	
	protected abstract void addCommandeerCleanup(Claim claim);
	
	private void removeClaim(Claim claim){
		claimList.remove(claim);
	}
	
	@Override
	public double usePriority(double multiplier) {
		if(multiplier > 1.0)
			multiplier = 1.0; // Managers cannot request more than their current priority
		
		if(baselinePriority > priorityScore){
			priorityScore = baselinePriority; // Should I keep this?
		}
		
		return priorityScore * multiplier;
	}
	
	public double getVolitility(){
		return volitilityScore;
	}
	
	public double usePriority(){
		return this.usePriority(1.0);
	}

	@Override
	public double incrementPriority(double priorityChange, boolean log) {
		priorityScore += priorityChange;
		return priorityScore;
	}

	@Override
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
	
	public Claim getClaim(Integer unitID){
		return claimList.get(unitID);
	}
	
	@Override
	public List<Claim> getAllClaims(){
		ArrayList<Claim> toReturn = new ArrayList<Claim>();
		for(Integer key: claimList.keySet()){
			toReturn.add(claimList.get(key));
		}
		return toReturn;
	}
	
	@Override
	public void freeUnits() {
		for(Claim c: getAllClaims()){
			c.free();
		}
	}

	@Override
	public void displayDebugGraphics(Game game){
		for(Claim c: getAllClaims()){
			game.drawCircleMap(c.unit.getPosition(), 5, debugColor);
		}
	}

}
