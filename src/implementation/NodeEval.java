package src.implementation;

public class NodeEval extends Thread {
	
	private Column column;
	
	public NodeEval(Column column) {
		this.column = column;
	}
	
	@Override
	public synchronized void run() {
		column.computeNewValues();
	}

}
