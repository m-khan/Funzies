import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Unit;
import bwta.BWTA;
import bwta.BaseLocation;

public class KaonBot extends DefaultBWListener {

	public static boolean debug = true;
    public static Mirror mirror = new Mirror();

    public static double SCV_COMMANDEER_BUILDING_MULTIPLIER = 1000.0;
    
    private static Game game;

    private Player self;
    
    private static ArrayList<Manager> managerList = new ArrayList<Manager>();
    private static ArrayList<TempManager> tempManagers = new ArrayList<TempManager>();
    private static Map<Integer, Unit> discoveredEnemies = new HashMap<Integer, Unit>();
    private static BaseLocation startPosition;
    private ArrayList<Unit> unclaimedUnits = new ArrayList<Unit>();
    private Map<Integer, Claim> masterClaimList = new HashMap<Integer, Claim>();
    private BuildingPlacer bpInstance;
    private ProductionQueue pQueue;

    public static Game getGame(){
    	// TODO find a better solution
    	return game;
    }
    
    public static void print(String message, boolean error){
    	try {
			if(error) System.err.println(game.getFrameCount() + " " + message);
			else System.out.println(game.getFrameCount() + " " + message);
} catch (Exception e) {
			System.err.println("ERROR PRINTING MESSAGE");
		}
    }
    
    public static void print(String message){
    	print(message, false);
    }
    
    public static List<Claim> getAllClaims(){
    	List<Claim> allClaims = new ArrayList<Claim>();
    	for(Manager m: managerList){
    		allClaims.addAll(m.getAllClaims());
    	}
    	return allClaims;
    }
    
    public static Map<Integer, Unit> discoveredEnemies(){
    	return discoveredEnemies;
    }
    
    public static BaseLocation getStartPosition(){
    	return startPosition;
    }
    
    public static void addTempManager(TempManager m){
    	tempManagers.add(m);
    }
    
    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onStart() {
    	try{
	        game = mirror.getGame();
    		//game.printf("onStart()");
	        game.enableFlag(1);
	        self = game.self();
	        pQueue = new ProductionQueue(self);
	        bpInstance = BuildingPlacer.getInstance();
	
	        game.setLocalSpeed(15);
	        
	        //Use BWTA to analyze map
	        //This may take a few minutes if the map is processed first time!
	        BWTA.readMap();
	        BWTA.analyze();

	        startPosition = BWTA.getStartLocation(self);
	        
	        EconomyManager econ = new EconomyManager(1.0, 0.5);
	        DepotManager depot = new DepotManager(1.0, 0.1, econ, self);
	        RushManager rush = new RushManager(0.8, 0.01);
	        
	        managerList.add(econ);
	        managerList.add(depot);
	        managerList.add(rush);
	        
	        for(Manager m: managerList){
	        	m.init(game);
	        }
	        
	//        BWTA.getBaseLocations();
	        //unclaimedUnits.addAll(self.getUnits());
	        
	        for(Unit u : game.getAllUnits()){
	        	if(u.getType().isBuilding() || u.getType().isResourceDepot()) bpInstance.reserve(u);
	        }
    	}catch(Exception e){
    		game.printf("Error in onStart(): " + e);
    		e.printStackTrace();
    	}
    }


    @Override
    public void onUnitComplete(Unit unit) {
    	try{
//    		game.printf("onUnitComplete()");
        	if(unit.getPlayer() == self && unit.getType().isBuilding()){
        		for(Manager m: managerList){
        			m.handleCompletedBuilding(unit, unit.getPlayer() == self);
        		}
        	}
    	}catch(Exception e){
    		game.printf("Error in onUnitComplete(): " + e);
    		e.printStackTrace();
    	}
    }
    
    @Override
	public void onUnitDiscover(Unit unit){
    	try{
    		if(unit.getType().isBuilding()){
    			bpInstance.reserve(unit);
    		}
    		if(discoveredEnemies.put(unit.getID(), unit) == null) {
	    		//game.printf("onUnitDiscover()");
		    	if(unit.getType().isBuilding()) bpInstance.reserve(unit);
		
				for(Manager manager: managerList){
					manager.handleNewUnit(unit, unit.getPlayer() == self, unit.getPlayer().isEnemy(self));
				}
    		}
    	}catch(Exception e){
    		game.printf("Error in onUnitDiscover(): " + e);
    		e.printStackTrace();
    	}
    }
    
    @Override
    public void onUnitDestroy(Unit unit){
    	try{
    		if(unit.getType().isBuilding()) bpInstance.free(unit);

    		boolean friendly = unit.getPlayer() == self;
    		boolean enemy = self.isEnemy(unit.getPlayer());
    		
    		for(Manager m: managerList){
    			m.handleUnitDestroy(unit, friendly, enemy);
    		}
    		
    		if(friendly)
    		{
	    		//game.printf("onUnitDestroy()");
	    		KaonBot.print("Unit Destroyed: " + unit.getType());
	    		Claim toCleanup = masterClaimList.remove(unit.getID());
    		
    			if(toCleanup != null){
    			// notify the manager the unit has been "commandeered" by the reaper
    				toCleanup.commandeer(null, Double.MAX_VALUE); 
    			}
    		}
    		
    	}catch(Exception e){
    		game.printf("Error in onUnitDestroy(): " + e);
    		e.printStackTrace();
    	}
    }
    
    @Override
    public void onFrame() {
    	try{
    		//game.printf("onFrame()");
    		runFrame();
    	} catch(Exception e){
    		game.printf("Error in onFrame(): " + e);
    		e.printStackTrace();
    	}
    	game.drawTextScreen(0, 0, "FRAME: " + game.getFrameCount());
    	game.drawTextScreen(200, 0, "APM: " + game.getAPM());
    }
    
    public void runFrame(){
        //game.setTextSize(10);

//    	KaonBot.print("FRAME: " + game.getFrameCount());
    	
    	StringBuilder output = new StringBuilder("===MANAGERS===\n");
    	for (Manager manager : managerList){
    		output.append(manager.getName()).append(": \n").append(manager.getStatus());
    	}

    	output.append("TEMP MANAGERS: " + tempManagers.size() + "\n");
    	Iterator<TempManager> it = tempManagers.iterator();
    	while(it.hasNext()){
    		TempManager next = it.next();
    		if(next.isDone()){
    			next.freeUnits();
    			it.remove();
    		}
    		else
    		{
    			next.runFrame();
    		}
    	}
    	
    	handleUnclaimedUnits(output);
    	for (Manager manager : managerList){
    		manager.assignNewUnitBehaviors();
    		manager.runFrame();
    	}

        game.drawTextScreen(400, 10, output.toString());

        pQueue.clear();
        for(Manager m: managerList){
        	pQueue.addAll(m.getProductionRequests());
        }

        String out = pQueue.processQueue();
        game.drawTextScreen(10, 10, out);

        displayDebugGraphics();
        
    }
    
    public void handleUnclaimedUnits(StringBuilder output){
    	// Give the managers a chance to claim new units
    	
    	for(Unit u: self.getUnits()){
    		if(u.exists() && u.isCompleted() &&
    				!masterClaimList.containsKey(u.getID())){
    			unclaimedUnits.add(u);
    		}
    	}
    	
    	output.append("Unclaimed Units: " + unclaimedUnits.size() + "\n");
//    	for (Unit unit : unclaimedUnits){
//    		output.append(unit.getType().toString()).append(": ").append(unit.getPosition().getPoint().toString()).append("\n");
//    	}
    	
    	// Make sure all the units in the claims list exist
    	Iterator<Unit> it = unclaimedUnits.iterator();
    	while(it.hasNext()){
    		if(!it.next().exists()){
    			it.remove();
    		}
    	}
    	
    	int num_units = unclaimedUnits.size();
    	ArrayList<Claim> topClaims = new ArrayList<Claim>(num_units);
    	for(int i = 0; i < num_units; i++){
    		topClaims.add(new Claim(null, 0.0, unclaimedUnits.get(i)));
    	}
    	for (Manager manager : managerList){
    		List<Double> claims = manager.claimUnits(unclaimedUnits);

    		if(claims != null){
	    		for(int i = 0; i < num_units; i++){
	    			Double newClaim = claims.get(i);
	        		if(topClaims.get(i).getPriority() < newClaim){
	        			topClaims.set(i, new Claim(manager, newClaim, unclaimedUnits.get(i)));
	        		}
	        	}
    		}
    	}

    	// Resolve all claims and assign the new units
    	unclaimedUnits.clear(); // this list will be repopulated with the leftovers
    	for(Claim claim : topClaims){
    		if(claim.getCommander() != null){
    			claim.addOnCommandeer(claim.new CommandeerRunnable() {
					@Override
					public void run() {
						if(newManager == null){
							masterClaimList.remove(claim.unit.getID());
						}
					}
				});
    			
    			claim.getCommander().assignNewUnit(claim);
    			Claim duplicate = masterClaimList.put(claim.unit.getID(), claim);
    			if(duplicate != null){
    				System.err.println("WARNING - Duplicate claim: " + claim);
    				duplicate.free(); // attempt to clean up the old claim
    			}
    		}
    	}
    }
    
    public void displayDebugGraphics(){
    	//TODO add flag
    	
    	//bpInstance.drawReservations();
    	for(Manager m: managerList){
    		m.displayDebugGraphics(game);
    	}
    	for(UnitCommander c: tempManagers){
    		c.displayDebugGraphics(game);
    	}
    }
    
    public static void main(String[] args) {
    	try{
            new KaonBot().run();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
}