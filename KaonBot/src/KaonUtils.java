import java.util.List;

import bwapi.Position;
import bwapi.Unit;


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

	public static Claim getClosestClaim(Position pos, List<Claim> units){
		if(units.size() == 0) return null;

		Claim closest = units.get(0);
		double distance = closest.unit.getDistance(pos);
		for(Claim c : units){
			double newDist = c.unit.getDistance(pos);
			if(distance > newDist){
				distance = newDist;
				closest = c;
			}
		}
		return closest;
	}
}
