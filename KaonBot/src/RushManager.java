import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import bwapi.Game;
import bwapi.Order;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;


public class RushManager extends AbstractManager {

	List<Unit> raxList = new ArrayList<Unit>();
	List<Unit> targetList = new ArrayList<Unit>();
	List<Position> targetPositions = new ArrayList<Position>();
	List<Rusher> rushers = new ArrayList<Rusher>();
	private final double RAX_WEIGHT = 0.6;
	TilePosition nextRax = null;
	int frameCount = 0;
	final int FRAME_LOCK = 50;
	private Random r = new Random();
	
	public RushManager(double baselinePriority, double volitilityScore) {
		super(baselinePriority, volitilityScore);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName(){
		return "RUSH " + targetList.size();
	}
	
	@Override
	public void init(Game game) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleNewUnit(Unit unit, boolean friendly, boolean enemy) {
		if(friendly){
		} else if(enemy){
			if(unit.getType().isBuilding()){
				targetList.add(unit);
				targetPositions.add(unit.getPosition());
				KaonBot.print(unit.getType() + " added to target list");
			}
		}
	}

	@Override
	public void handleCompletedBuilding(Unit unit, boolean friendly) {
		if(friendly && unit.getType() == UnitType.Terran_Barracks){
			raxList.add(unit);
		}
	}

	@Override
	public ArrayList<Double> claimUnits(List<Unit> unitList) {
		ArrayList<Double> toReturn = new ArrayList<Double>(unitList.size());
		
		for(Unit unit: unitList){
			UnitType type = unit.getType();
			if(!type.isWorker() && !type.isBuilding()) {
				toReturn.add(usePriority());
			}
			else {
				toReturn.add(DO_NOT_WANT);
			}
		}
		return toReturn;
	}

	@Override
	public void runFrame() {
		for (Iterator<Rusher> iterator = rushers.iterator(); iterator.hasNext();) {
			Rusher r = iterator.next();
			if(r.update()){
				iterator.remove();
				Claim toFree = this.claimList.get(r.getUnit().getID());
				if(toFree != null) toFree.free();
			}
		}
		
		if(frameCount < FRAME_LOCK){
			frameCount++;
			return;
		}
		updateNextRax();
		// TODO make this smarter (like not retarded)
		incrementPriority(getVolitility() * targetList.size(), false);
		frameCount = 0;
	}

	private void updateNextRax(){
		Unit builder = BuildingPlacer.getInstance().getSuitableBuilder(KaonBot.getStartPosition().getTilePosition(), 
				getRaxPriority(), this);
		if(builder != null){
			nextRax = BuildingPlacer.getInstance().getBuildTile(builder, UnitType.Terran_Supply_Depot, 
					KaonBot.getStartPosition().getTilePosition());
		}

	}
	
	private double getRaxPriority(){
		int raxCount = raxList.size() + 1;
		
		for(BuildingOrder b: ProductionQueue.getActiveOrders()){
			if(b.getUnitType() == UnitType.Terran_Barracks){
				raxCount++;
			}
		}
		
		return (this.usePriority() * RAX_WEIGHT) / (raxCount);
	}
	
	@Override
	public List<ProductionOrder> getProductionRequests() {
		List<ProductionOrder> prodList = new LinkedList<ProductionOrder>();
		
		for(Unit rax: raxList){
			prodList.add(new UnitOrder(50, 0, this.usePriority(), rax, UnitType.Terran_Marine));
		}

		// return now if we don't have a barracks location
		if(nextRax == null){
			return prodList;
		}

		double raxPriority = getRaxPriority();
		prodList.add(new BuildingOrder(150, 0, raxPriority, null, UnitType.Terran_Barracks, nextRax));
		
		return prodList;
	}

	public void updateTargetList(){
		Set<Integer> enemies = KaonBot.discoveredEnemies().keySet();
		
		Iterator<Unit> tIt = targetList.iterator();
		Iterator<Position> pIt = targetPositions.iterator();
		
		while(tIt.hasNext() && pIt.hasNext()) {
			Unit u = tIt.next();
			pIt.next();
			
			if(enemies.contains(u.getID())){
				tIt.remove();
				pIt.remove();
				KaonBot.print(u.getType() + " removed from target list");
			}
		}
	}
	
	@Override
	public void assignNewUnitBehaviors() {
		updateTargetList();

		for(Unit u: newUnits){
			if(targetList.size() == 0){
				List<BaseLocation> starts = BWTA.getStartLocations();
				Position p = starts.get(r.nextInt(starts.size())).getPosition();
				rushers.add(new Rusher(u, null, p));
			}
			else{
				rushers.add(new Rusher(u, targetList.get(0), targetPositions.get(0)));
			}
		}
	}

	@Override
	protected void addCommandeerCleanup(Claim claim) {
		for (Iterator<Rusher> iterator = rushers.iterator(); iterator.hasNext();) {
			Rusher r = iterator.next();
			if(r.getUnit() == claim.unit){
				iterator.remove();
			}
		}
	}
	
	@Override
	public void displayDebugGraphics(Game game){
		if(nextRax != null){
			game.drawCircleMap(nextRax.toPosition(), frameCount, debugColor);
		}
	}

	
	class Rusher extends Behavior{
		
		Unit target;
		Position targetPosition;
		private final int MICRO_LOCK = 50;
		private int microCount;
		
		public Rusher(Unit unit, Unit target, Position targetPosition) {
			super(unit);
			this.target = target;
			this.targetPosition = targetPosition;
		}

		@Override
		public boolean update() {
			if(microCount < MICRO_LOCK){
				microCount++;
				return false;
			}
			if(!getUnit().exists())
			{
				return true;
			}
			if(target != null && target.exists()){
				targetPosition = target.getPosition();
			}
			else if((getUnit().getDistance(targetPosition) < getUnit().getType().sightRange()))
			{
				return true;
			}
			else if(getUnit().getOrder() == Order.AttackMove){
				microCount = 0;
				return false;
			}
			
			// TODO: better micro
			getUnit().attack(targetPosition);
			microCount = 0;
			return false;
		}
	}
}
