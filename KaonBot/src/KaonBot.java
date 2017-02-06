import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Unit;
import bwta.BWTA;

public class KaonBot extends DefaultBWListener {

	public static boolean debug = true;
    public static Mirror mirror = new Mirror();

    private static Game game;

    private Player self;
    
    private static ArrayList<Manager> managerList = new ArrayList<Manager>();
    private static ArrayList<TempManager> tempManagers = new ArrayList<TempManager>();
    private ArrayList<Unit> unclaimedUnits = new ArrayList<Unit>();
    private BuildingPlacer bpInstance;
    private ProductionQueue pQueue;

    public static Game getGame(){
    	// TODO find a better solution
    	return game;
    }
    
    public static List<Claim> getAllClaims(){
    	List<Claim> allClaims = new ArrayList<Claim>();
    	for(Manager m: managerList){
    		allClaims.addAll(m.getAllClaims());
    	}
    	return allClaims;
    }
    
    public static void addTempManager(TempManager m){
    	tempManagers.add(m);
    }
    
    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onUnitComplete(Unit unit) {
    	try{
    		game.printf("onUnitComplete()");
        	if(unit.getPlayer() == self && !unit.getType().isBuilding()){
        		unclaimedUnits.add(unit);
        	}
        	else{
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
    		//game.printf("onUnitDiscover()");
	    	if(unit.getType().isBuilding()) bpInstance.reserve(unit);
	
			for(Manager manager: managerList){
				manager.handleNewUnit(unit, unit.getPlayer() == self);
			}
    	}catch(Exception e){
    		game.printf("Error in onUnitDiscover(): " + e);
    		e.printStackTrace();
    	}
    }
    
    @Override
    public void onUnitDestroy(Unit unit){
    	try{
    		//game.printf("onUnitDestroy()");
    		System.out.println("Unit Destroyed: " + unit.getType());
    	
    		if(unit.getType().isBuilding()) bpInstance.free(unit);
    	}catch(Exception e){
    		game.printf("Error in onUnitDestroy(): " + e);
    		e.printStackTrace();
    	}
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

	        managerList.add(new EconomyManager(1.0));
	        
	        for(Manager m: managerList){
	        	m.init(game);
	        }
	        
	//        BWTA.getBaseLocations();
	        unclaimedUnits.addAll(self.getUnits());
	        
	        for(Unit u : game.getAllUnits()){
	        	if(u.getType().isBuilding() || u.getType().isResourceDepot()) bpInstance.reserve(u);
	        }
    	}catch(Exception e){
    		game.printf("Error in onStart(): " + e);
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
    	game.drawTextScreen(0, 0, game.getFrameCount() + "");
    }
    
    public void runFrame(){
        //game.setTextSize(10);

    	StringBuilder output = new StringBuilder("===MANAGERS===\n");
    	for (Manager manager : managerList){
    		output.append(manager.getName()).append(": \n").append(manager.getStatus()).append("\n");
    	}

    	handleUnclaimedUnits(output);
        
    	Iterator<TempManager> it = tempManagers.iterator();
    	while(it.hasNext()){
    		TempManager next = it.next();
    		if(next.isDone()){
    			unclaimedUnits.addAll(next.freeUnits());
    			it.remove();
    		}
    		else
    		{
    			next.runFrame();
    		}
    	}
    	
    	
        pQueue.clear();
        for(Manager m: managerList){
        	pQueue.addAll(m.getProductionRequests());
        }

        String out = pQueue.processQueue();
        output.append(out);

        displayDebugGraphics();
        
        game.drawTextScreen(10, 10, output.toString());
    }
    
    public void handleUnclaimedUnits(StringBuilder output){
    	// Give the managers a chance to claim new units
    	output.append("Unclaimed Units:\n");
    	for (Unit unit : unclaimedUnits){
    		output.append(unit.getType().toString()).append(": ").append(unit.getPosition().getPoint().toString()).append("\n");
    	}
    	
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

    		for(int i = 0; i < num_units; i++){
    			Double newClaim = claims.get(i);
        		if(topClaims.get(i).getPriority() < newClaim){
        			topClaims.set(i, new Claim(manager, newClaim, unclaimedUnits.get(i)));
        		}
        	}
    	}

    	// Resolve all claims and assign the new units
    	unclaimedUnits.clear(); // this list will be repopulated with the leftovers
    	for(Claim claim : topClaims){
    		if(claim.getManager() == null){
    			unclaimedUnits.add(claim.unit);
    		}
    		else{
    			claim.getManager().assignNewUnit(claim);
    		}
    	}
    	
    	// TODO this is dumb fix it
    	for (Manager manager : managerList){
    		unclaimedUnits.addAll(manager.assignNewUnitBehaviors());
    		unclaimedUnits.addAll(manager.runFrame());
    	}
    }
    
    public void displayDebugGraphics(){
    	bpInstance.drawReservations();
    	for(Manager m: managerList){
    		m.displayDebugGraphics(game);
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