package kaonbot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import bwapi.Color;
import bwapi.Game;
import bwapi.Order;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Chokepoint;

public class DefenseManager extends AbstractManager {

	List<Unit> raxList = new ArrayList<Unit>();
	List<Unit> targetList = new ArrayList<Unit>();
	List<Position> targetPositions = new ArrayList<Position>();
	List<Position> defencePoints = new ArrayList<Position>(); 
	List<Rusher> rushers = new ArrayList<Rusher>();
	private final double RAX_WEIGHT = 0.6;
	TilePosition nextRax = null;
	TilePosition raxBase;
	int frameCount = 0;
	final int FRAME_LOCK = 51;
	final int DEFENSE_RADIUS = 100;
	final double BUILDING_KILL_MULTIPLIER = 5.0;
	final double NO_TARGET = -0.1;
	private Random r = new Random();
	private int targetListUpdateFrame = 0;
	
	public DefenseManager(double baselinePriority, double volitilityScore) {
		super(baselinePriority, volitilityScore);
		raxBase = KaonBot.getStartPosition().getTilePosition();
	}

	@Override
	public String getName(){
		return "DEFENSE " + targetList.size() + "|" + rushers.size();
	}
	
	@Override
	public void init(Game game) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleNewUnit(Unit unit, boolean friendly, boolean enemy) {
		if(enemy){
			List<BaseLocation> bases = KaonBot.econManager.getBases();
			
			for(BaseLocation b: bases){
				if(BWTA.getRegion(unit.getPosition()) == b.getRegion()){
					targetList.add(unit);
					targetPositions.add(unit.getPosition());
					KaonBot.print(unit.getType() + " added to target list");
					incrementPriority(getVolitility(), false);
				}
			}
		}
		else if(friendly){
			if(unit.getType().isResourceDepot()){
				updateDefencePoints();
			}
		}
	}

	private void updateDefencePoints(){
		defencePoints.clear();
		
		List<BaseLocation> bases = KaonBot.econManager.getBases();
		
		Set<Chokepoint> chokes = new HashSet<Chokepoint>();		
		List<Chokepoint> duplicates = new ArrayList<Chokepoint>();
		Set<BaseLocation> toAdd = new HashSet<BaseLocation>();
		
		for(BaseLocation b: bases){
			//defencePoints.add(b.getPosition());
			List<Chokepoint> newChokes = b.getRegion().getChokepoints();
			for(Chokepoint choke : newChokes){
				if(choke.getCenter().getDistance(b.getPosition()) > 500){
					toAdd.add(b);
				} else if(!chokes.add(choke)){
					duplicates.add(choke);
				}
			}
		}
		for(Chokepoint choke : duplicates){
			chokes.remove(choke);
		}
		for(Chokepoint choke : chokes){
			defencePoints.add(choke.getCenter());
		}
		for(BaseLocation b: toAdd){
			defencePoints.add(b.getPosition());
		}
		
		if(defencePoints.size() == 0){
			defencePoints.add(KaonBot.mainPosition.getPosition());
		}
		
	}
	
	@Override
	public void handleCompletedBuilding(Unit unit, boolean friendly) {
		if(friendly && unit.getType() == UnitType.Terran_Barracks){
			raxList.add(unit);
		}
	}

	@Override
	public void handleUnitDestroy(Unit u, boolean friendly, boolean enemy) {
		double price = u.getType().mineralPrice() + u.getType().gasPrice();
		
		if(enemy){
			if(u.getType().isBuilding()){
				price = price * BUILDING_KILL_MULTIPLIER;
			}
			incrementPriority(getVolitility() * price / -100, false);
		} else if(friendly){
			if(!claimList.containsKey(u.getID())){
				incrementPriority(getVolitility() * price / 100, false);
			}
			if(u.getType() == UnitType.Terran_Barracks){
				raxList.remove(u);
			}
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
//		if(targetList.size() == 0){
//			incrementPriority(getVolitility() * NO_TARGET, false);
//		}
		
		updateNextRax();
		frameCount = 0;
	}

	private void updateNextRax(){
		Unit builder = BuildingPlacer.getInstance().getSuitableBuilder(KaonBot.getStartPosition().getTilePosition(), 
				getRaxPriority(), this);
		if(builder != null){
			nextRax = BuildingPlacer.getInstance().getBuildTile(builder, UnitType.Terran_Barracks, KaonBot.mainPosition.getTilePosition());
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
			if(rax.getAddon() != null){
				prodList.add(new UnitOrder(50, 50, this.usePriority(), rax, UnitType.Terran_Medic));
			}
			else{
				prodList.add(new UnitOrder(50, 0, this.usePriority(), rax, UnitType.Terran_Marine));
			}
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
		// only do this once per frame
		if(KaonBot.getGame().getFrameCount() == targetListUpdateFrame){
			return;
		}
		targetListUpdateFrame = KaonBot.getGame().getFrameCount();
		
//		Set<Integer> enemies = KaonBot.discoveredEnemies().keySet();
//		
//		Iterator<Unit> tIt = targetList.iterator();
//		Iterator<Position> pIt = targetPositions.iterator();
//		
//		while(tIt.hasNext() && pIt.hasNext()) {
//			Unit u = tIt.next();
//			pIt.next();
//			
//			if(!enemies.contains(u.getID())){
//				tIt.remove();
//				pIt.remove();
//				KaonBot.print(u.getType() + " removed from target list");
//				incrementPriority(-1 * getVolitility(), false);
//
//			}
//		}
		targetList.clear();
		targetPositions.clear();
		// only check 1 unit each frame to cut down on performance hit
		Unit u = KaonBot.getAllUnits().get(KaonBot.getGame().getFrameCount() % KaonBot.getAllUnits().size());
		if(KaonBot.isFriendly(u) && u.getType().isBuilding()){
			for(Unit e : u.getUnitsInRadius(DEFENSE_RADIUS)){
				if(KaonBot.isEnemy(e)){
					targetList.add(e);
					targetPositions.add(e.getPosition());
				}
			}
		}
		
		
	}
		
	@Override
	public void assignNewUnitBehaviors() {
		updateDefencePoints();
		updateTargetList();
		
		for(Claim c: newUnits){
			if(targetList.size() == 0){
				Position p = defencePoints.get(r.nextInt(defencePoints.size()));
				rushers.add(new Rusher(c, null, p));
			}
			else{
				int target = r.nextInt(targetList.size());
				rushers.add(new Rusher(c, targetList.get(target), targetPositions.get(target)));
			}
		}
		newUnits.clear();
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
		
		for(Rusher r: rushers){
			String toDraw = r.getUnit().getOrder().toString();
			game.drawTextMap(r.getUnit().getPosition(), toDraw);
			game.drawCircleMap(r.getUnit().getPosition(), r.getUnit().getGroundWeaponCooldown(), new Color(0, 0, 0));
			if(r.getUnit().isStuck()) game.drawCircleMap(r.getUnit().getPosition(), 2, new Color(255, 0, 0), true);
		}
	}

	
	class Rusher extends Behavior{
		
		Unit target;
		Position targetPosition;
		private final int MICRO_LOCK = 12;
		private int microCount;
		
		public Rusher(Claim c, Unit target, Position targetPosition) {
			super(c);
			this.target = target;
			this.targetPosition = targetPosition;
			microCount = 0;
		}

		@Override
		public boolean update() {
			if(microCount < MICRO_LOCK){
				microCount++;
				return false;
			}
			
			if(getUnit().isStuck()){
				return true;
			}
			
			if(!getUnit().exists())
			{
				KaonBot.print(getUnit().getID() + " released, does not exist.");
				return true;
			}
			if(target != null && target.exists()){
				targetPosition = target.getPosition();
//				if(getUnit().getType().groundWeapon().maxRange() < getUnit().getDistance(targetPosition)){
//					KaonBot.print("IN RANGE: " + microCount);
//					getUnit().attack(target);
//					microCount = 0;
//					return false;
//				}
			}else if((getUnit().getDistance(targetPosition) < getUnit().getType().sightRange()))
			{
				KaonBot.print(getUnit().getID() + " NOTHING HERE: " + microCount);
				getUnit().stop();
				incrementPriority(getVolitility() * NO_TARGET, false);
				return true;
			}
			
			
			if(getUnit().getOrder() == Order.AttackUnit ||
					getUnit().getOrder() == Order.AttackTile ||
					getUnit().getOrder() == Order.AtkMoveEP){
				//KaonBot.print("ALREADY ATTACKING: " + microCount);

				touchClaim();
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
