import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;

import bwapi.Color;
import bwapi.Game;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public class BuildingOrder extends ProductionOrder implements Comparator<ProductionOrder>{
	
	private Unit producer;
	private Claim tempClaim = null;
	private UnitType toProduce;
	private TilePosition position;
	private BuildManager buildManager = null;
	private final double priorityMultiplier = 100000.0; // multiplier used for commendeering SCVs
	
	public BuildingOrder(int minerals, int gas, double priority, Unit producer, UnitType toProduce, TilePosition position) {
		super(ProductionOrder.BUILDING, minerals, gas, priority);
		this.producer = producer;
		this.toProduce = toProduce;
		this.position = position;
	}
	
	public TilePosition getPosition(){
		return position;
	}
	
	public UnitType getUnitType(){
		return toProduce;
	}
	
	@Override
	public boolean equals(Object o){
		BuildingOrder other = (BuildingOrder) o;
		// TODO Test this
		return position.equals(other.getPosition()) && this.getUnitType().equals(other.getUnitType());
	}
	
	@Override
	public String toString(){
		DecimalFormat df = new DecimalFormat("#.##");
		String toReturn =  toProduce + " @ " + position.toPosition() + " " + df.format(getPriority());
		if(buildManager != null){
			toReturn += "\nBuilder: " + buildManager.getAllClaims().get(0).unit.getPosition();
			toReturn += " Spent:" + this.isSpent();
		}
		
		return toReturn;
	}

	@Override
	public String getSignature(){
		return toProduce + "" + position.toPosition();
	}
	
	@Override
	public boolean execute(){
		if(!canExecute()){
			KaonBot.print("Failed to execute " + this);
			return false;
		}

		if(tempClaim == null){
			KaonBot.print("No claim for " + this);
		}
		
		BuildManager bm = new BuildManager(tempClaim, this);
		if(tempClaim.commandeer(bm, this.getPriority() * priorityMultiplier)){
			tempClaim.addOnCommandeer(tempClaim.new CommandeerRunnable(bm) {
				@Override
				public void run() {
					((BuildManager) arg).setDone();
					setDone();
				}
			});
			KaonBot.addTempManager(bm);
			buildManager = bm;
			executed = producer.build(toProduce, position);
		}
		
		return executed;
	}
	
	@Override
	public boolean isDone(){
		if(buildManager == null){
			return true;
		}
		else{
			return super.isDone();
		}
	}
	
	private void retry(){
		producer.build(toProduce, position);
	}
	
	public boolean canExecute(){
		if(producer == null || !producer.exists()){
			findNewProducer();
		}
		
		return producer != null && producer.exists() && KaonBot.getGame().canBuildHere(position, toProduce);
	}
	
	private void findNewProducer(){
		List<Claim> claimList = KaonBot.getAllClaims();
		tempClaim = KaonUtils.getClosestClaim(position.toPosition(), claimList, UnitType.Terran_SCV, this.getPriority() * priorityMultiplier, null);
		if(tempClaim != null){
			producer = tempClaim.unit;
		}
	}
	
	private class BuildManager extends TempManager{
		private BuildingOrder order;
		private boolean started = false;
		private Color debugColor;
		
		private BuildManager(Claim claim, BuildingOrder order){
			super(claim);
			this.order = order;
			debugColor = KaonUtils.getRandomColor();
			
			BuildingPlacer.getInstance().reserve(order.getPosition(), order.toProduce, debugColor);
			KaonBot.print("Building  " + order + " started with: " + claim.unit.getID());
		}
		
		@Override
		public void runFrame() {
			Unit builder = this.getAllClaims().get(0).unit;
			if(builder.isConstructing()){
				started = true;
				order.setSpent();
			}
			else{
				if(started || !builder.exists()){
					order.setDone();
					this.setDone();
				}
				builder.move(order.getPosition().toPosition());
				order.retry();
			}
		}
		
		@Override
		public void assignNewUnit(Claim claim) {
		}

		@Override
		public void assignNewUnitBehaviors() {
		}

		@Override
		public void displayDebugGraphics(Game game) {
			Unit builder = this.getAllClaims().get(0).unit;
			game.drawCircleMap(order.getPosition().toPosition(), 20, debugColor, false);
			game.drawCircleMap(builder.getPosition(), 10, debugColor, true);
			if(builder.isConstructing()){
				game.drawCircleMap(builder.getPosition(), 10, new Color(255, 0, 0), true);
			}
		}
	}
}

