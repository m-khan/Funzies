import java.util.ArrayList;
import java.util.List;

import bwapi.Color;
import bwapi.Game;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;


public class EconomyManager extends AbstractManager{
	
	private final double ENEMY_BASE = 1.0;
	
	private ArrayList<Miner> minerList = new ArrayList<Miner>();
	private ArrayList<Unit> mineralFields = new ArrayList<Unit>();
	private ArrayList<Unit> gasGeysers = new ArrayList<Unit>();
	private ArrayList<Base> bases = new ArrayList<Base>();
	
	public EconomyManager(double baselinePriority)
	{
		super(baselinePriority);
	}
	
	public void init(Game game){
		List<BaseLocation> baseLocations = BWTA.getBaseLocations();
		BaseLocation start = BWTA.getStartLocation(game.self());
		
		for(BaseLocation l: baseLocations){
			bases.add(new Base(l, start));
		}
	}
	
	public List<ProductionOrder> getProductionRequests(){
		ArrayList<ProductionOrder> list = new ArrayList<ProductionOrder>();
		Base nextExpand = null;
		double expandScore = Double.MIN_VALUE;
		
		for(Base b: bases){
			if(b.cc != null && b.cc.exists()){
				list.add(new UnitOrder(50, 0, this.usePriority(0.85), b.cc, UnitType.Terran_SCV));
			}
			else{
				double score = b.gdFromEnemy - b.gdFromStart;
//				System.out.println(b.location.getPosition() + ": " + b.gdFromEnemy + " - " +  b.gdFromStart + " = " + score);

				if(score > expandScore){
					expandScore = score;
					nextExpand = b;
				}
			}
		}
		
		
		if(nextExpand != null){
			list.add(new BuildingOrder(400, 0, this.usePriority(0.5), null, 
					UnitType.Terran_Command_Center, nextExpand.location.getTilePosition()));
		}
		return list;
	}
	
	protected class Base{
		BaseLocation location;
		double gdFromStart;
		double gdFromEnemy;
		private ArrayList<Unit> mins = new ArrayList<Unit>();
		private ArrayList<Integer> minerCounts = new ArrayList<Integer>(); 
		Unit gas;
		Unit extractor = null;
		Unit cc = null;
		boolean active = false;
		ArrayList<Miner> miners = new ArrayList<Miner>();
		
		protected Base(BaseLocation location, BaseLocation start)
		{
			this.location = location;
			gdFromStart = BWTA.getGroundDistance(location.getTilePosition(), start.getTilePosition());
			
			List<BaseLocation> baseLocations = BWTA.getStartLocations();
			double distance = 0;
			for(BaseLocation bL: baseLocations){
				if(!bL.getPoint().equals(start.getPoint())) {
					System.out.println("Possble Enemy at " + bL);
					distance += BWTA.getGroundDistance(bL.getTilePosition(), location.getTilePosition());
				}
			}
			
			gdFromEnemy = distance / baseLocations.size() - 1;
			System.out.println(location.getPosition() + ": " + gdFromStart);
		}
		
		protected void addMinerals(Unit unit){
			mins.add(unit);
			minerCounts.add(0);
		}
		
		protected boolean addMiner(Unit unit){
			if(mins.size() == 0){
				return false;
			}
			
			miners.add(new Miner(unit, mins.get(0))); //TODO implement mineral lock
			return true;
		}

		protected int requiredMiners(){
			return mins.size() * 2 - miners.size();
		}
		
		protected List<Unit> update(){
			ArrayList<Unit> freeUnits = new ArrayList<Unit>();
			
			if(cc == null || !cc.exists()){
				cc = null;
				for(Miner m: miners){
					if(m.getUnit().exists()){
						freeUnits.add(m.getUnit());
					}
				}
				miners.clear();
				return freeUnits;
			}
			
			ArrayList<Miner> toRemove = new ArrayList<Miner>();
			for(Miner m : miners){
				if(m.update()){
					if(m.getUnit().exists()){
						freeUnits.add(m.getUnit());
					}
					else{
						toRemove.add(m);
					}
				}
			}
			for(Miner m: toRemove){
				miners.remove(m);
			}
			return freeUnits;
		}
	}

	public void handleNewUnit(Unit unit, boolean friendly){
		if(unit.getType().isMineralField()){
			mineralFields.add(unit);
			
			for(Base b: bases){
				double distance = b.location.getDistance(unit.getPosition());
				if(distance < 500){
					b.addMinerals(unit);
				}
			}
		}
		else if(unit.getType() == UnitType.Resource_Vespene_Geyser){
			gasGeysers.add(unit);
			
			for(Base b: bases){
				double distance = b.location.getDistance(unit.getPosition());
				if(distance < 500){
					b.gas = unit;
				}
			}
		}
		else if(unit.getType().isResourceDepot()){
			if (friendly && unit.isCompleted()){
				for(Base b: bases){
					double distance = b.location.getDistance(unit.getPosition());
					if(distance < 300)
						b.cc = unit;
				}
			}
			else{
				//TODO ignore first base
				incrementPriority(ENEMY_BASE, false);
			}
		}
	}

	@Override
	public void handleCompletedBuilding(Unit unit, boolean friendly) {
		if(friendly && unit.getType().isResourceDepot()){
			for(Base b: bases){
				double distance = b.location.getDistance(unit.getPosition());
				if(distance < 300)
					b.cc = unit;
			}
		}
	}

	public List<Unit> runFrame(){
		ArrayList<Unit> toReturn = new ArrayList<Unit>();
		for(Base b: bases){
			toReturn.addAll(b.update());
		}
		return toReturn;
		
	}
	
	@Override
	public String getName(){
		return "ECONOMY";
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
			if(unit.getType().isWorker())
			{
				int max = 0;
				Base minBase = null;
				for(Base b: bases){
					if(b.cc != null && b.requiredMiners() > max){
						max = b.requiredMiners();
						minBase = b;
					}
				}
				if(minBase != null){
					if(!minBase.addMiner(unit)){
						returnList.add(unit);
					}
				}
				else{
					// TODO: what do we do if there are no bases?
				}
				
			}
			else{
				if(unit.exists()){
					returnList.add(unit);
				}
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
			getUnit().gather(resource);
}
		
		public UnitType getResourceType(){
			return resourceType;
		}
		
		@Override
		public boolean update() {
			return !resource.exists() && !getUnit().exists();
		}
	}

	@Override
	public void displayDebugGraphics(Game game) {
		for(Base b: bases){
			if(b.cc != null){
				game.drawTextMap(b.cc.getPosition(), "Patches: " + b.mins.size() + "\nWorkers: " + b.miners.size());
				
				for(Unit m: b.mins){
					game.drawLineMap(b.cc.getPosition(), m.getPosition(), new Color(100, 100, 200));
				}
				game.drawLineMap(b.cc.getPosition(), b.gas.getPosition(), new Color(100, 200, 100));
			}
		}
	}

}
