import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;

import bwapi.Player;


public class ProductionQueue extends PriorityQueue<ProductionOrder> {

	private static final long serialVersionUID = 2962631698946196631L;
	private Player player;
	private ArrayList<ProductionOrder> activeOrders = new ArrayList<ProductionOrder>();
	
	public ProductionQueue(Player player){
		this.player = player;
	}
	
	public String processQueue(){
    	StringBuilder output = new StringBuilder("Production Queue:\n");

		if(peek() == null){
			output.append("Empty\n");
			return output.toString();
		}
//		ArrayList<ProductionOrder> reserve = new ArrayList<ProductionOrder>();
		int min = player.minerals();
		int gas = player.gas();
		
		int freeSupply = player.supplyTotal() - player.supplyUsed();
		
		int minRes = 0;
		int gasRes = 0;
		
		// remove all finished orders from active orders
		Iterator<ProductionOrder> it = activeOrders.iterator();
		while(it.hasNext()){
			if(it.next().isDone()){
				it.remove();
			}
		}
		output.append("ACTIVE ORDRES: " + activeOrders.size() + "\n");
		
		for(ProductionOrder o: activeOrders){
			output.append("AO: " + o.toString() + "\n");
		}
		
		while(	peek() != null && 
				peek().getMinerals() <= (min - minRes) && 
				peek().getGas() <= (gas - gasRes)){
			
			ProductionOrder toExecute = poll();
			
			boolean isDuplicate = false;
			String currentSig = toExecute.getSignature();
			for(ProductionOrder activeOrder: activeOrders){
				if(activeOrder.getSignature().equals(currentSig)){
					isDuplicate = true;
				}
			}
			
			if(isDuplicate){
				output.append("=" + toExecute + " - " + minRes  + "\n");
				if(!toExecute.isSpent()){
					minRes += toExecute.getMinerals();
					gasRes += toExecute.getGas();
				}
			}
			else if(toExecute.getType() == ProductionOrder.UNIT &&
					((UnitOrder) toExecute).getSupply() <= freeSupply &&
					toExecute.canExecute()){
				System.out.println("Producing " + toExecute);
				toExecute.execute();
				activeOrders.add(toExecute);
				output.append("!" + toExecute + " - " + minRes + "\n");
			}
			else if(toExecute.getType() == ProductionOrder.BUILDING && toExecute.canExecute()){
				toExecute.execute();
				activeOrders.add(toExecute);
				if(!toExecute.isSpent()){
					minRes += toExecute.getMinerals();
					gasRes += toExecute.getGas();
				}

				output.append("!" + toExecute + " - " + minRes  + "\n");
			} else {
				output.append("*" + toExecute + " - " + minRes  + "\n");
				
				//TODO calculate actual reserve needed based on timeUntilExecutable
				if(!toExecute.isSpent()){
					minRes += toExecute.getMinerals();
					gasRes += toExecute.getGas();
				}
			}
		}
		while(peek() != null){
			output.append(poll() + "\n");
		}
		
		return output.toString();
	}
}
