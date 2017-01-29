import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Unit;
import bwta.BWTA;

public class KaonBot extends DefaultBWListener {

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;
    
    private ArrayList<Manager> managerList = new ArrayList<Manager>();
    private Set<Integer> unitIDs = new HashSet<Integer>();
    private ArrayList<Unit> unclaimedUnits = new ArrayList<Unit>();

    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onUnitCreate(Unit unit) {
    	if(unit.getPlayer() == self && !unitIDs.add(unit.getID())){
    		unclaimedUnits.add(unit);
    	}
    }
    
    @Override
	public void onUnitDiscover(Unit unit){
		for(Manager manager: managerList){
			manager.handleNewUnit(unit);
		}
	}

    @Override
    public void onStart() {
        game = mirror.getGame();
        self = game.self();

        game.setLocalSpeed(30);
        
        managerList.add(new EconomyManager(1.0, 0.001));

        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        BWTA.readMap();
        BWTA.analyze();

//        BWTA.getBaseLocations();
        unclaimedUnits.addAll(self.getUnits());
    }

    private class Claim{
    	public final Manager manager;
    	public final double claim;
    	public final Unit unit;

    	public Claim(Manager man, double cl, Unit u)
    	{
    		manager = man;
    		claim = cl;
    		unit = u;
    	}
    }
    
    @Override
    public void onFrame() {
    	try{
    		runFrame();
    	} catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
    public void runFrame(){
        //game.setTextSize(10);

    	StringBuilder output = new StringBuilder("Priorities:\n");
    	for (Manager manager : managerList){
    		output.append(manager.getName()).append(": ").append(manager.getPriorityStatus()).append("\n");
    	}
    	output.append("Unclaimed Units:\n");
    	for (Unit unit : unclaimedUnits){
    		output.append(unit.getType().toString()).append(": ").append(unit.getPosition().getPoint().toString()).append("\n");
    	}

        game.drawTextScreen(10, 10, output.toString());
        
    	// Give the managers a chance to claim new units
    	int num_units = unclaimedUnits.size();
    	ArrayList<Claim> topClaims = new ArrayList<Claim>(num_units);
    	for(int i = 0; i < num_units; i++){
    		topClaims.add(new Claim(null, 0.0, unclaimedUnits.get(i)));
    	}
    	for (Manager manager : managerList){
    		List<Double> claims = manager.claimUnits(unclaimedUnits);

    		for(int i = 0; i < num_units; i++){
    			Double newClaim = claims.get(i);
        		if(topClaims.get(i).claim < newClaim){
        			topClaims.set(i, new Claim(manager, newClaim, unclaimedUnits.get(i)));
        		}
        	}
    	}
    	
    	// Resolve all claims and assign the new units
    	unclaimedUnits.clear(); // this list will be repopulated with the leftovers
    	for(Claim claim : topClaims){
    		if(claim.manager == null){
    			unclaimedUnits.add(claim.unit);
    		}
    		else{
    			claim.manager.assignNewUnit(claim.unit);
    		}
    	}
    	for (Manager manager : managerList){
    		unclaimedUnits.addAll(manager.assignNewUnitBehaviors());
    		unclaimedUnits.addAll(manager.runFrame());
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