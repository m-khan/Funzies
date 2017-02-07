import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;


public class DepotManager extends AbstractManager {

	EconomyManager econ;
	Player player;
	TilePosition nextDepot = null;
	Map<Integer, Unit> depotList = new HashMap<Integer, Unit>();
	int frameLock = 0;
	
	public DepotManager(double baselinePriority, EconomyManager econ, Player player) {
		super(baselinePriority);
		this.econ = econ;
		this.player = player;
	}

	@Override
	public void init(Game game) {
	}

	@Override
	public void handleNewUnit(Unit unit, boolean friendly) {
		if(friendly && unit.getType() == UnitType.Terran_Supply_Depot)
		{
			depotList.put(unit.getID(), unit);
		}
	}

	@Override
	public void handleCompletedBuilding(Unit unit, boolean friendly) {
	}

	@Override
	public ArrayList<Double> claimUnits(List<Unit> unitList) {
		return null;
	}

	@Override
	public void runFrame() {
		if(frameLock > 0){
			frameLock -= 1;
			return;
		}
		findNextDepotSpot();
		frameLock = 100;
	}

	private void findNextDepotSpot(){
		Unit builder = BuildingPlacer.getInstance().getSuitableBuilder(player.getStartLocation());
		if(builder != null){
			try{
				nextDepot = BuildingPlacer.getInstance().getBuildTile(builder, UnitType.Terran_Supply_Depot, player.getStartLocation());
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
		
	@Override
	public List<ProductionOrder> getProductionRequests() {
		List<ProductionOrder> toReturn = new ArrayList<ProductionOrder>();
		if(nextDepot == null){
			return toReturn;
		}
		
		int supply = player.supplyTotal();
		for(Integer i: depotList.keySet()){
			if(depotList.get(i).isConstructing()){
				supply += 8;
			}
		}
		
		int used = player.supplyUsed();

		double multiplier = 1.0 - ((supply - used) / 8.0);
		toReturn.add(new BuildingOrder(100, 0, this.usePriority(multiplier), null, 
				UnitType.Terran_Supply_Depot, nextDepot));
		return toReturn;
	}

	@Override
	public void assignNewUnitBehaviors() {
	}

	@Override
	protected void addCommandeerCleanup(Claim claim) {
	}

	@Override
	public void displayDebugGraphics(Game game){
		if(nextDepot != null){
			game.drawCircleMap(nextDepot.toPosition(), frameLock, new Color(0, 0, 0));
		}
	}
	
}
