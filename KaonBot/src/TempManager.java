import java.util.ArrayList;
import java.util.List;

import bwapi.Unit;

public abstract class TempManager{
	
	private List<Claim> claims;
	private boolean done = false;
	
	public TempManager(){
		claims = new ArrayList<Claim>();
	}
	
	public TempManager(Claim c){
		this();
		claims.add(c);
	}
	
	public TempManager(List<Claim> claims){
		this.claims = claims;
	}
	
	public abstract void runFrame();
	protected void setDone(){
		done = true;
	}
	public boolean isDone(){
		return done;
	}
	public List<Claim> getClaims(){
		return claims;
	}
	public List<Unit> freeUnits(){
		ArrayList<Unit> toReturn = new ArrayList<Unit>();
		for(Claim c: claims){
			toReturn.add(c.unit);
		}
		claims.clear();
		return toReturn;
	}
}
