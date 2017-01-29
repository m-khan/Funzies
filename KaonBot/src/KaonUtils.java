import java.util.List;

import bwapi.Position;
import bwapi.Unit;


public class KaonUtils {

	public static Unit getClosest(Position pos, List<Unit> units){
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
}
