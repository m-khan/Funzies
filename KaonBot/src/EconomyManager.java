import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bwapi.Color;
import bwapi.Game;
import bwapi.Order;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;


public class EconomyManager extends AbstractManager{
	
	private final double ENEMY_BASE = 1.0;
	private final double SCV_MULT = 0.85;
	private final double EXPO_MULT = 0.5;
	private final int NUM_BASES_TO_QUEUE = 3;
	
	private ArrayList<Base> bases = new ArrayList<Base>();
	
	public EconomyManager(double baselinePriority, double volatilityScore)
	{
		super(baselinePriority, volatilityScore);
	}
	
	@Override
	public String getName(){
		return "ECONOMY";
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
		double[] expandScores = new double[bases.size()];
		Base init = bases.get(0);
		double highScore = init.gdFromEnemy - init.gdFromStart;
		double lowScore = highScore;
		
		// normalize and use all expand scores
		int i = 0;
		for(Base b: bases){
			if(b.cc != null && b.cc.exists()){
				list.add(new UnitOrder(50, 0, this.usePriority(SCV_MULT), b.cc, UnitType.Terran_SCV));
			}
			double score = b.gdFromEnemy - b.gdFromStart;
			expandScores[i] = score;
			if(score > highScore){
				highScore = score;
			}
			else if(score < lowScore){
				lowScore = score;
			}
			i++;
		}

		int queued = 0;
		for(i = 0; i < bases.size() && queued < NUM_BASES_TO_QUEUE; i++){
			Base b = bases.get(i);
			double nScore = expandScores[i];
			nScore = nScore - lowScore;
			nScore = nScore / (highScore - lowScore);
			if(b.cc == null) {
				list.add(new BuildingOrder(400, 0, this.usePriority(EXPO_MULT * nScore), null, 
						UnitType.Terran_Command_Center, b.location.getTilePosition()));
				queued++;
			}
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
					distance += BWTA.getGroundDistance(bL.getTilePosition(), location.getTilePosition());
				}
			}
			
			gdFromEnemy = distance / baseLocations.size() - 1;
		}
		
		protected void addMinerals(Unit unit){
			mins.add(unit);
			minerCounts.add(0);
		}
		
		protected boolean addMiner(Unit unit){
			if(mins.size() == 0){
				return false;
			}
			
			miners.add(new Miner(unit, mins.get((miners.size() + 1) % mins.size()))); //TODO implement mineral lock
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

	public void handleNewUnit(Unit unit, boolean friendly, boolean enemy){
		if(unit.getType().isMineralField()){
			for(Base b: bases){
				double distance = b.location.getDistance(unit.getPosition());
				if(distance < 300){
					b.addMinerals(unit);
				}
			}
		}
		else if(unit.getType() == UnitType.Resource_Vespene_Geyser){
			for(Base b: bases){
				double distance = b.location.getDistance(unit.getPosition());
				if(distance < 300){
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
			else if(!friendly){
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

	@Override
	public void runFrame(){
		for(Base b: bases){
			b.update();
		}
		
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
				claims.add(DO_NOT_WANT);
			}
		}
		return claims;
	}

	public void removeMiner(Claim cl){
		for(Base b: bases){
			Iterator<Miner> it = b.miners.iterator();
			while(it.hasNext()){
				if(it.next().getUnit() == cl.unit){
					it.remove();
				}
			}
		}
	}

	@Override
	protected void addCommandeerCleanup(Claim cl){
		cl.addOnCommandeer(cl.new CommandeerRunnable(cl) {
			@Override
			public void run() {
				removeMiner((Claim) arg);
			}
		});
	}
	
	@Override
	public void assignNewUnitBehaviors() {
		for(Unit unit: newUnits){
			if(unit.exists() && unit.getType().isWorker())
			{
				// check close base to see if it needs the worker
				Base newBase = null;
				for(Base b: bases){
					double distance = b.location.getDistance(unit.getPosition());
					if(distance < 300){
						newBase = b;
						break;
					}
				}
				if(newBase == null || newBase.requiredMiners() > 0){
					// check all bases and see which needs the worker the most
					int max = -1;
					newBase = null;
					for(Base b: bases){
						if(b.cc != null && b.requiredMiners() > max){
							max = b.requiredMiners();
							newBase = b;
						}
					}
				}
				if(newBase != null) newBase.addMiner(unit);
			}
		}
		newUnits.clear();
	}

	private class Miner extends Behavior{

		private Unit resource;
		private UnitType resourceType;
		private boolean returning = false;
		private final int MICRO_LOCK = 2; //num frames to skip between micro actions
		private int microCount = 0; 
		
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
			if(microCount < MICRO_LOCK || getUnit().getOrder() == Order.MiningMinerals 
					|| getUnit().isCarryingMinerals() || getUnit().getOrder() == Order.WaitForMinerals)
			{
				microCount++;
				return false;
			}
			microCount = 0;
			
			return !resource.exists() || !getUnit().exists();
		}
	}

	@Override
	public void displayDebugGraphics(Game game) {
		super.displayDebugGraphics(game);
		for(Base b: bases){
			if(b.cc != null){
				game.drawTextMap(b.cc.getPosition(), "Patches: " + b.mins.size() + 
													 "\nWorkers: " + b.miners.size() + 
													 "\nNeed: " + b.requiredMiners());
				
				for(Miner m: b.miners){
					game.drawLineMap(m.resource.getPosition(), m.getUnit().getPosition(), new Color(100, 100, 200));
				}
				if(b.gas != null) game.drawLineMap(b.cc.getPosition(), b.gas.getPosition(), new Color(100, 200, 100));
			}
		}
	}

	@Override
	public void handleUnitDestroy(Unit u, boolean friendly, boolean enemy) {
		// TODO Auto-generated method stub
		
	}
}
