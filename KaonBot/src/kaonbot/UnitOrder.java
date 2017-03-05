package kaonbot;
import java.text.DecimalFormat;
import java.util.Comparator;

import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;

public class UnitOrder extends ProductionOrder implements Comparator<ProductionOrder>{
	
	private Unit producer;
	private Position producerPosition;
	private UnitType toProduce;
	
	public UnitOrder(int minerals, int gas, double priority, Unit producer, UnitType toProduce){
		super(ProductionOrder.UNIT, minerals, gas, priority);
		this.producer = producer;
		producerPosition = producer.getPosition();
		this.toProduce = toProduce;
		
	}
	
	public String toString(){
		DecimalFormat df = new DecimalFormat("#.##");
		return  toProduce + "@" + producerPosition + " " + df.format(super.getPriority());
	}
	
	public String getSignature(){
		return producerPosition.toString();
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
	
	@Override
	public int getMinerals(){
		// if it's ready to go, get all the minerals
		if(producer.getTrainingQueue().size() == 0) {
			return super.getMinerals();
		}
		// else reserve what we need
		return (super.getMinerals() - (super.getMinerals() * producer.getRemainingTrainTime()) / producer.getTrainingQueue().get(0).buildTime());
	}
	
	@Override
	public int getGas(){
		// see minerals
		if(producer.getTrainingQueue().size() == 0) return super.getGas();

		return (super.getGas() - (super.getGas() * producer.getRemainingTrainTime()) / producer.getTrainingQueue().get(0).buildTime());
	}

}
