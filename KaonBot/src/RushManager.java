import java.util.ArrayList;
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
	List<Unit> targetList = new ArrayList<Unit>();
	List<Position> targetPositions = new ArrayList<Position>();
	List<Rusher> rushers = new ArrayList<Rusher>();
	private final double RAX_WEIGHT = 0.6;
	private final double MARINE_PER_MEDIC = 5;
	TilePosition nextRax = null;
	TilePosition raxBase;
	int frameCount = 0;
	final int FRAME_LOCK = 51;
	private Random r = new Random();
	
	public RushManager(double baselinePriority, double volitilityScore) {
		super(baselinePriority, volitilityScore);
		
		raxBase = KaonBot.getStartPosition().getTilePosition();
	}

	@Override
	public String getName(){
		return "RUSH " + targetList.size() + "|" + rushers.size();
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
	}

	@Override
	public void handleCompletedBuilding(Unit unit, boolean friendly) {
		if(friendly && unit.getType() == UnitType.Terran_Barracks){
			raxList.add(unit);
		}
	}

	@Override
	public void handleUnitDestroy(Unit u, boolean friendly, boolean enemy) {
		int price = u.getType().mineralPrice() + u.getType().gasPrice();
		
		if(enemy){
			incrementPriority(getVolitility() * price / 100, false);
		} else if(friendly){
			incrementPriority(getVolitility() * price / -100, false);
		
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
				List<BaseLocation> starts = BWTA.getBaseLocations();
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
			touchClaim();
			if(microCount < MICRO_LOCK){
				microCount++;
				return false;
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
				return true;
			}
			
			if(getUnit().getOrder() == Order.AttackMove ||
					getUnit().getOrder() == Order.AttackUnit ||
					getUnit().getOrder() == Order.AttackTile ||
					getUnit().getOrder() == Order.AtkMoveEP){
				//KaonBot.print("ALREADY ATTACKING: " + microCount);

				microCount = 0;
				return false;
			}
			KaonBot.print(getUnit().getID() + "ATTACKING: " + microCount);
			

			// TODO: better micro
			getUnit().attack(targetPosition);
			microCount = 0;
			return false;
		}
	}


}
