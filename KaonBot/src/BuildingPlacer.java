import java.util.List;

import bwapi.Color;
import bwapi.Game;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;


public class BuildingPlacer {

	private static BuildingPlacer buildingPlacer = new BuildingPlacer();
	private Game game;
	private boolean[][] reservationMap;
	private Color[][] reservationColors;
	
	private BuildingPlacer(){
		game = KaonBot.mirror.getGame();
		reservationMap = new boolean[game.mapWidth()][game.mapHeight()];
		reservationColors = new Color[game.mapWidth()][game.mapHeight()];
	}
	
	public void reserve(TilePosition p, UnitType building, Color color){
		if(!building.isBuilding()){
			KaonBot.print("WARNING - tried to reserve and non-unit " + p.getPoint());
			return;
		}
		
		for(int x = p.getX(); x == p.getX() + building.tileHeight(); x++){
			for(int y = p.getY(); y == p.getX() + building.tileHeight(); y++){
				if(!reservationMap[x][y]){
					reservationMap[x][y] = true;
					reservationColors[x][y] = color;
				}
			}
		}
		
	}
	
	public void reserve(Unit u){
		mark(u, true);
	}
	
	public void free(Unit u){
		mark(u, false);
	}
	
	private void mark(Unit u, boolean marker){
		TilePosition tp = u.getTilePosition();
		for(int i = 0; i < u.getType().tileWidth(); i++){
			for(int j = 0; j < u.getType().tileHeight(); j++){
				reservationMap[tp.getX() + i][tp.getY() + j] = true;
			}
		}
	}
	
	public void drawReservations(){
		for(int i = 0; i < reservationMap.length; i++){
			for(int j = 0; j < reservationMap[i].length; j++)
			{
				if(reservationMap[i][j]){
					Color color;
					if(reservationColors[i][j] != null){
						color = reservationColors[i][j];
					} else {
						color = new Color(100, 100, 100);
					}
					
					game.drawBoxMap(i * 32, j * 32, i * 32 + 32, j * 32 + 32, color, false);
				}
			}
		}
	}
	
	public Unit getSuitableBuilder(TilePosition position, double priority, UnitCommander searcher){
		List<Claim> claimList = KaonBot.getAllClaims();
		Claim c = KaonUtils.getClosestClaim(position.toPosition(), claimList, UnitType.Terran_SCV, priority, searcher);
		if(c != null){
			return c.unit;
		}
		return null;
	}
	
	// Returns a suitable TilePosition to build a given building type near 
	// specified TilePosition aroundTile, or null if not found. (builder parameter is our worker)
	public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
		TilePosition ret = null;
		int maxDist = 3;
		int stopDist = 40;
		
		// Refinery, Assimilator, Extractor
		if (buildingType.isRefinery()) {
			for (Unit n : game.neutral().getUnits()) {
				if ((n.getType() == UnitType.Resource_Vespene_Geyser) && 
						( Math.abs(n.getTilePosition().getX() - aroundTile.getX()) < stopDist ) &&
						( Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist )
						) return n.getTilePosition();
			}
		}
		while ((maxDist < stopDist) && (ret == null)) {
			for (int i=aroundTile.getX()-maxDist; i<=aroundTile.getX()+maxDist; i++) {
				for (int j=aroundTile.getY()-maxDist; j<=aroundTile.getY()+maxDist; j++) {
					if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false)) {
						// units that are blocking the tile
						boolean unitsInWay = false;
						for (Unit u : game.getAllUnits()) {
							if (u.getID() == builder.getID()) continue;
							if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
						}
						if (!unitsInWay) {
							return new TilePosition(i, j);
						}
					}
				}
			}
			maxDist += 2;
		}
		
		if (ret == null) KaonBot.print("Unable to find suitable build position for " + buildingType.toString());
		return ret;
	}
	
	public static TilePosition getTilePosition(int px, int py){
		TilePosition tp = new TilePosition(px / 32, py / 32);
		tp.makeValid();
		return tp;
	}
	
	public static TilePosition getTilePosition(Unit u){
		return getTilePosition(u.getTop(), u.getLeft());
	}
	
	public static BuildingPlacer getInstance(){
		return buildingPlacer;
	}
	
	public boolean canBuildHere(TilePosition position, UnitType b){
		if (!game.canBuildHere(position, b))
			return false;
		return true;
	}
}
