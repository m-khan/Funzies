import java.util.Comparator;
import java.util.List;

import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public class BuildingOrder extends ProductionOrder implements Comparator<ProductionOrder>{
	
	private Unit producer;
	private Claim tempClaim = null;
	private UnitType toProduce;
	private TilePosition position;

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
		return toProduce + " @ " + position.toPosition() + " " + getPriority();
	}

	@Override
	public String getSignature(){
		return toProduce + "" + position.toPosition();
	}
	
	@Override
	public boolean execute(){
		if(producer == null || !producer.exists()){
			findNewProducer();
		}
		
		if(producer == null || !producer.exists()){
			System.err.println("Unable to find producer for " + this);
			return false;
		}

		if(tempClaim != null){
			if(tempClaim.commandeer(null, this.getPriority() * 10)){
				KaonBot.addTempManager(new BuildManager(tempClaim, this));
			}
		}
		
		executed = producer.build(toProduce, position);
		return executed;
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
		List<Claim> scvList = KaonBot.getAllClaims();
		tempClaim = KaonUtils.getClosestClaim(position.toPosition(), scvList);
		producer = tempClaim.unit;
	}
	
	private class BuildManager extends TempManager{
		private BuildingOrder order;
		private boolean started = false;
		
		private BuildManager(Claim claim, BuildingOrder order){
			super(claim);
			this.order = order;
		}
		
		@Override
		public void runFrame() {
			Unit builder = tempClaim.unit;
			if(builder.exists() && builder.isConstructing()){
				System.out.println("Constructing...");
				started = true;
			}
			else{
				if(started){
					order.setDone();
					this.setDone();
				}
				builder.move(order.getPosition().toPosition());
				order.retry();
			}
		}
	}
}

