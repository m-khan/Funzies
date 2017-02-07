import java.util.ArrayList;
import java.util.List;

import bwapi.Unit;

public class Claim implements Comparable<Claim>{
	private UnitCommander commander;
	private double priority;
	public final Unit unit;
	private List<Runnable> onCommandeer = new ArrayList<Runnable>();
	private int claimFrame;
	private static final int CLAIM_LOCK = 100;
	
	public Claim(UnitCommander man, double cl, Unit u)
	{
		commander = man;
		priority = cl;
		unit = u;
		claimFrame = KaonBot.getGame().getFrameCount();
	}

	public String toString(){
		return unit.getType() + " - " + commander.getName() + " - " + onCommandeer.size();
	}
	
	public void addOnCommandeer(Runnable r){
		onCommandeer.add(r);
	}
	
	public void free(){
		commandeer(null, Double.MAX_VALUE);
	}
	
	public boolean canCommandeer(){
		int currentFrame = KaonBot.getGame().getFrameCount();
		return (currentFrame - CLAIM_LOCK) > claimFrame;
	}
	
	public boolean commandeer(UnitCommander newManager, double newClaim){
		if(!canCommandeer() || newClaim < priority){
			return false;
		}

		for(Runnable r: onCommandeer){
			if(r instanceof CommandeerRunnable){
				((CommandeerRunnable) r).setNewValues(this, newManager);
			}
			r.run();
		}
		
		commander = newManager;
		priority = newClaim;
		return true;
	}
	
	public void removeCommandeerRunnable(Runnable r){
		onCommandeer.remove(r);
	}
	
	public UnitCommander getCommander(){
		return commander;
	}
	
	public double getPriority(){
		return priority;
	}
	
	@Override
	public int compareTo(Claim o) {
		return new Double(priority).compareTo(o.priority);
	}
	
	public abstract class CommandeerRunnable implements Runnable {
		Claim claim = null;
		UnitCommander newManager = null;
		Object arg;
		
		protected void setNewValues(Claim claim, UnitCommander newManager){
			this.claim = claim;
			this.newManager = newManager;
		}
		
		public CommandeerRunnable(Object arg) {
			this.arg = arg;
		}
		
		public CommandeerRunnable()
		{
			this(null);
		}

		public void deleteThis()
		{
			
		}
		@Override
		public abstract void run();
	}}
