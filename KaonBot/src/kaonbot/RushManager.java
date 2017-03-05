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


public class RushManager extends AbstractManager {

	List<Unit> raxList = new ArrayList<Unit>();
	LinkedList<Unit> targetList = new LinkedList<Unit>();
	LinkedList<Position> targetPositions = new LinkedList<Position>();
	List<Rusher> rushers = new ArrayList<Rusher>();
	private final double RAX_WEIGHT = 0.6;
	//private final double MARINE_PER_MEDIC = 5;
	private final double NEW_ARMY_UNIT = 0.1;
	final double BUILDING_KILL_MULTIPLIER = 5.0;
	TilePosition nextRax = null;
	TilePosition raxBase;
	private Position lastRusherDeath = null;
	private boolean waitingForRushers = false;
	private int waitForNRushers = 1;
	private int deadRushers = 0;
	private Set<Unit> rushersWaiting = new HashSet<Unit>();
	int frameCount = 0;
	private boolean justStartLocations = true;
	final int FRAME_LOCK = 51;
	private Random r = new Random();
	
	public RushManager(double baselinePriority, double volitilityScore) {
		super(baselinePriority, volitilityScore);
		
		raxBase = KaonBot.getStartPosition().getTilePosition();
	}

	@Override
	public String getName(){
		return "ATTACK " + targetList.size() + "|" + rushers.size() + "|" + waitForNRushers + "|" +waitingForRushers + ":" + rushersWaiting.size();
	}
	
	@Override
	public void init(Game game) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleNewUnit(Unit unit, boolean friendly, boolean enemy) {
		if(enemy){
			if(unit.getType().isBuilding()){
				targetList.add(unit);
				targetPositions.add(unit.getPosition());
				KaonBot.print(unit.getType() + " added to target list");
				incrementPriority(getVolitility(), false);
			}
		}
		if(friendly && !unit.getType().isWorker() && !unit.getType().isBuilding()){
			incrementPriority(getVolitility() * NEW_ARMY_UNIT, false);
		}
	}

	public void addTarget(Unit unit, boolean addFront){
		if(unit == null || !unit.exists()){
			return;
		}
		
		if(addFront){
			targetList.add(0, unit);
			targetPositions.add(0, unit.getPosition());
		} else {
			targetList.add(unit);
			targetPositions.add(unit.getPosition());
		}
		KaonBot.print(unit.getType() + " added to target list");
		//incrementPriority(getVolitility(), false);
	}
	
	public void removeTarget(Unit unit){
		Iterator<Unit> tIt = targetList.iterator();
		Iterator<Position> pIt = targetPositions.iterator();
		
		while(tIt.hasNext() && pIt.hasNext()) {
			Unit u = tIt.next();
			pIt.next();
			
			if(u == unit){
				tIt.remove();
				pIt.remove();
				KaonBot.print(u.getType() + " removed from target list");
				//incrementPriority(-1 * getVolitility(), false);
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
	public void handleUnitDestroy(Unit u, boolean friendly, boolean enemy) {
		double price = u.getType().mineralPrice() + u.getType().gasPrice();
		
		if(enemy){
			if(u.getType().isBuilding()){
				price = price * BUILDING_KILL_MULTIPLIER;
				//waitForNRushers = waitForNRushers / 2;
				deadRushers = 0;
			}
			incrementPriority(getVolitility() * price / 100, false);
		} else if(friendly){
			incrementPriority(getVolitility() * price / -100, false);
			
			if(claimList.containsKey(u.getID())){
				lastRusherDeath = u.getPosition();
				if(!waitingForRushers) deadRushers++;
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

		if(waitingForRushers){
			if(rushersWaiting.size() > waitForNRushers){
				waitingForRushers = false;
				rushersWaiting.clear();
			} else if(claimList.size() / 2 < waitForNRushers){
				waitForNRushers = claimList.size() / 2;
			}
		} else if(deadRushers > waitForNRushers){
			// Attack deemed failure
			waitingForRushers = true;
			waitForNRushers = claimList.size() / 2;
			deadRushers = 0;
		}

		if(KaonBot.getSupply() > 380){
			incrementPriority(getVolitility(), false);
		}

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
		Set<Integer> enemies = KaonBot.discoveredEnemies().keySet();
		
		Iterator<Unit> tIt = targetList.iterator();
		Iterator<Position> pIt = targetPositions.iterator();
		
		while(tIt.hasNext() && pIt.hasNext()) {
			Unit u = tIt.next();
			pIt.next();
			
			if(!enemies.contains(u.getID())){
				tIt.remove();
				pIt.remove();
				KaonBot.print(u.getType() + " removed from target list");
				incrementPriority(-1 * getVolitility(), false);

			}
		}
	}
	
	@Override
	public void assignNewUnitBehaviors() {
		updateTargetList();

		for(Claim c: newUnits){
			if(targetList.size() == 0){
				
				List<BaseLocation> starts;
				if(justStartLocations){
					starts = BWTA.getStartLocations();
				} else {
					starts = BWTA.getBaseLocations();
				}
				Position p = starts.get(r.nextInt(starts.size())).getPosition();
				rushers.add(new Rusher(c, null, p));
			}
			else{
				rushers.add(new Rusher(c, targetList.get(0), targetPositions.get(0)));
			}
		}
		newUnits.clear();
	}

	@Override
	protected void addCommandeerCleanup(Claim claim) {
		for (Iterator<Rusher> iterator = rushers.iterator(); iterator.hasNext();) {
			Rusher r = iterator.next();
			if(r.getUnit() == claim.unit){
				rushersWaiting.remove(r.getUnit());
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
			String toDraw = toString() + "\n" + r.getUnit().getOrder();
//			if(r.getUnit().isStartingAttack()){
//				toDraw += "\nisStartingAttack";
//			}
//			if(r.getUnit().isAttackFrame())
//			{
//				toDraw += "\nisAttackFrame";
//			}
//			if(r.getUnit().isAttacking());
//			{
//				toDraw += "\nisAttacking";
//			}
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

		public void forceAttack(){
			getUnit().attack(targetPosition);
			rushersWaiting.remove(getUnit());
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
			
			// if it's fighting we just let it do it's thing
			if(	getUnit().getOrder() == Order.AttackUnit) {
				//addTarget(getUnit().getTarget(), true);
				touchClaim();
				microCount = 0;
				return false;
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
				if(target != null){
					//removeTarget(target);
				}
				return true;
			}
			
			if(lastRusherDeath != null && waitingForRushers){
				if(getUnit().getDistance(lastRusherDeath) < getUnit().getType().sightRange() * 3){
					if(rushersWaiting.add(getUnit())){
						getUnit().stop();
					}
					touchClaim();
					microCount = 0;
					return false;
				} else {
					getUnit().attack(lastRusherDeath);
					microCount = 0;
					return false;
				}
			}
			
			if( getUnit().getOrder() == Order.AttackMove){
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
