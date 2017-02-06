import java.util.Comparator;

import bwapi.Unit;
import bwapi.UnitType;

public class UnitOrder extends ProductionOrder implements Comparator<ProductionOrder>{
	
	private Unit producer;
	private UnitType toProduce;
	
	public UnitOrder(int minerals, int gas, double priority, Unit producer, UnitType toProduce){
		super(ProductionOrder.UNIT, minerals, gas, priority);
		this.producer = producer;
		this.toProduce = toProduce;
		
	}
	
	public String toString(){
		return  toProduce + " @ " + producer.getType() + producer.getPosition() + " " +super.getPriority();
	}
	
	public String getSignature(){
		return toProduce+""+producer.getPosition();
	}
	
	public int getSupply(){
		return toProduce.supplyRequired();
	}
	
	public boolean execute(){
		setDone();
		return producer.train(toProduce);
	}

	public boolean canExecute(){
		return  producer.exists() && producer.getRemainingTrainTime() == 0 && producer.getTrainingQueue().size() == 0;
	}
	
	public int timeUntilExecutable(){
		return producer.getRemainingTrainTime();
	}
}

