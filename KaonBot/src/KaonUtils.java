import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import bwapi.Color;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;


public class KaonUtils {

	public static Unit getClosest(Position pos, List<Unit> units){
		if(units.size() == 0) return null;
		
		Unit closest = units.get(0);
		double distance = closest.getDistance(pos);
		for(Unit u : units){
			double newDist = u.getDistance(pos);
			if(distance > newDist){
				distance = newDist;
				closest = u;
			}
		}
		return closest;
	}

	public static TilePosition getRandomBase(){
		List<Unit> allUnits = KaonBot.getGame().getAllUnits();
		List<Unit> bases = new ArrayList<Unit>();
		for(Unit u: allUnits){
			if(u.getType().isResourceDepot()){
				bases.add(u);
			}
		}
		
		Random r = new Random();
		
		return bases.get(r.nextInt(bases.size())).getTilePosition();
	}
	
	public static Claim getClosestClaim(Position pos, List<Claim> units, UnitType ut, double priority, UnitCommander searcher){
		Iterator<Claim> it = units.iterator();

		Claim closest = null;
		double distance = Double.MAX_VALUE;
		
		while(it.hasNext()){
			Claim next = it.next();
			if(ut == null || next.unit.getType() == ut){
				double newDist = next.unit.getDistance(pos);
				if(distance > newDist && next.canCommandeer(priority, searcher)){
					distance = newDist;
					closest = next;
				}
			}
		}
		return closest;
	}
	
	public static Color getRandomColor(){
		Random rn = new Random();
		return new Color(rn.nextInt(255), rn.nextInt(255), rn.nextInt(255));

	}
}
