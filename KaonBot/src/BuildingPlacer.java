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
	
	private BuildingPlacer(){
		game = KaonBot.mirror.getGame();
		reservationMap = new boolean[game.mapWidth()][game.mapHeight()];
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
					game.drawBoxMap(i * 32, j * 32, i * 32 + 32, j * 32 + 32, new Color(100, 100, 100), false);
				}
			}
		}
	}
	
	public static TilePosition getTilePosition(int px, int py){
		TilePosition tp = new TilePosition(px / 32, py / 32);
		tp.makeValid();
		return tp;
	}
	
	public static TilePosition getTilePosition(Unit u){
		return getTilePosition(u.getTop(), u.getLeft());
	}
	
	public void reserve(TilePosition p, int width, int height){
		
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
