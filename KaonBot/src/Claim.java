import java.util.ArrayList;
import java.util.List;

import bwapi.Unit;

public class Claim implements Comparable<Claim>{
	private Manager manager;
	private double priority;
	public final Unit unit;
	private List<Runnable> onCommandeer = new ArrayList<Runnable>();
	
	public Claim(Manager man, double cl, Unit u)
	{
		manager = man;
		priority = cl;
		unit = u;
	}

	public void addOnCommandeer(Runnable r){
		onCommandeer.add(r);
	}
	
	public boolean commandeer(Manager newManager, double newClaim){
		if(newClaim < priority){
			return false;
		}

		for(Runnable r: onCommandeer){
			r.run();
		}
		
		manager = newManager;
		priority = newClaim;
		return true;
	}
	
	public Manager getManager(){
		return manager;
	}
	
	public double getPriority(){
		return priority;
	}
	
	@Override
	public int compareTo(Claim o) {
		return new Double(priority).compareTo(o.priority);
	}
	
	public abstract class CommandeerRunnable implements Runnable {
		Object arg;
		
		public CommandeerRunnable()
		{
			arg = null;
		}
		
		public CommandeerRunnable(Object parameter) {
			arg = parameter;
		}

		public abstract void run();
	}}
