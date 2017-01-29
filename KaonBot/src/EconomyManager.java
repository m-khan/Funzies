import java.util.ArrayList;
import java.util.List;

import bwapi.Unit;
import bwapi.UnitType;


public class EconomyManager extends AbstractManager{
	
	private ArrayList<Miner> minerList = new ArrayList<Miner>();
	private ArrayList<Unit> mineralFields = new ArrayList<Unit>();;
	
	public EconomyManager(double baselinePriority, double degradationCoefficient)
	{
		super(baselinePriority, degradationCoefficient);
		minerList = new ArrayList<Miner>();
	}
	
	public void handleNewUnit(Unit unit){
		if(unit.getType().isMineralField()){
			mineralFields.add(unit);
			System.out.println("Minerals found: " + unit.getPosition().getPoint());
		}
	}
	
	public List<Unit> runFrame(){
		ArrayList<Unit> freeUnits = new ArrayList<Unit>();
		for(Miner m : minerList){
			if(m.update()){
				freeUnits.add(m.getUnit());
			}
		}
		return freeUnits;
	}
	
	@Override
	public String getName(){
		return "Economy";
	}
	
	@Override
	public ArrayList<Double> claimUnits(List<Unit> unitList) {
		ArrayList<Double> claims = new ArrayList<Double>();
		
		for(Unit unit: unitList){
			if (unit.getType() == UnitType.Terran_SCV)
			{
				claims.add(this.usePriority());
				//claims.add(-1.0);
			}
			else{
				claims.add(-1.0);
			}
		}
		return claims;
	}

	@Override
	public ArrayList<Unit> assignNewUnitBehaviors() {
		ArrayList<Unit> returnList = new ArrayList<Unit>();
		for(Unit unit: newUnits){
			if(unit.getType() == UnitType.Terran_SCV)
			{
				minerList.add(new Miner(unit, KaonUtils.getClosest(unit.getPosition(), mineralFields))); //TODO replace null
			}
			else{
				returnList.add(unit);
			}
		}
		newUnits.clear();
		return returnList;
	}

	private class Miner extends Behavior{

		private Unit resource;
		private UnitType resourceType;
		
		public Miner(Unit miner, Unit resource){
			super(miner);
			this.resource = resource;
			this.resourceType = resource.getType();
			System.out.println("New miner: " + resourceType + " at " + resource.getPosition().getPoint());
			getUnit().gather(resource);
}
		
		public UnitType getResourceType(){
			return resourceType;
		}
		
		@Override
		public boolean update() {
			Unit miner = getUnit();
			if(miner.isGatheringGas()){
//				System.out.println("Miner " + miner.getID() + " gathering gas...");
			}
			else if(miner.isGatheringMinerals()){
//				System.out.println("Miner " + miner.getID() + " gathering minerals...");
			}
//			else if(miner.isCarryingGas()){
//				System.out.println("Miner " + miner.getID() + " carrying gas...");
//			}
//			else if(miner.isCarryingMinerals()){
//				System.out.println("Miner " + miner.getID() + " carrying minerals...");
//			}
			else{
				return true;
			}
			return false;
		}
		
		
		
	}
	
	
}
