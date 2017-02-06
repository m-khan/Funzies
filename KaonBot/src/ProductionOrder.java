import java.util.Comparator;


public abstract class ProductionOrder implements Comparator<ProductionOrder>, Comparable<ProductionOrder>{
	
	private int type;
	private int minerals;
	private int gas;
	private double priority;
	protected boolean executed;
	public static final int RESERVATION = 0;
	public static final int UNIT = 1;
	public static final int BUILDING = 2;
	public static final int ADDON = 3;
	private boolean isDone = false;
	
	public ProductionOrder(int type, int minerals, int gas, double priority){
		this.type = type;
		this.minerals = minerals;
		this.gas = gas;
		this.priority = priority;
		executed = false;
		
	}
	
	public String toString(){
		return "ProductionOrder: " + type + ", " + priority;
	}

	public String getSignature(){
		return "PO" + type + minerals + gas;
	}
	
	public int getType() {
		return type;
	}

	public int getMinerals() {
		return minerals;
	}

	public int getGas() {
		return gas;
	}

	public double getPriority() {
		return priority;
	}
	
	protected void setDone(){
		isDone = true;
	}
	
	public boolean isDone(){
		return isDone;
	}

	public abstract boolean execute();
	
	public int compare(ProductionOrder o1, ProductionOrder o2){
		return -1 * new Double(o1.getPriority()).compareTo(new Double( o2.getPriority()));
	}
	
	public int compareTo(ProductionOrder other){
		return -1 * new Double(this.getPriority()).compareTo(other.getPriority());
	}
	
	public boolean equals(ProductionOrder o){
		return new Double(getPriority()).equals(new Double(o.getPriority()));
	}
	
	public boolean canExecute(){
		return false;
	}
	
	public int timeUntilExecutable(){
		return -1;
	}
}

