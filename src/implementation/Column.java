package implementation;

import java.util.Hashtable;
import np2015.GraphInfo;

public class Column extends Thread {
	
	private Hashtable<Integer, Double> values;			// kein Zugriff von außen
	
	private Hashtable<Integer, Double> outLeft;			// geschützt durch explizites Lock, oder besser durch implizites, weil da jeder der das Objekt kennt Zugiff hat? TODO 
	private Hashtable<Integer, Double> outRight;		// geschützt durch explizites Lock TODO 
	
	private Hashtable<Integer, Double> oldvalues;		// kein Zugriff von außen
	private double valueDifference;						// kein Zugriff von außen
	private double deleteFlag;							// kein Zugriff von außen
	private double sigma;								// kein Zugriff von außen
	private GraphInfo graph;		//graph.getRateForTarget(x,y,<Neighbor>) Neighbor:={Left, Right, Top, Bottom}
	private Grid grid; /**ueber das grid kommt die column mit grid.getLOcals an die locale schrittzahl ran.**/

	public Column(GraphInfo graph, Grid grid) {
		this.graph = graph;
		this.grid = grid;
		// TODO Auto-generated constructor stub
	}

	@Override
	public synchronized void run() {
		/**berechnet den akku und den horizontalen outflow knotenweise.**/
		
		
		
	}
	
	public synchronized void computeNewValues() {
		// TODO
	}
	
	
	public void setValue(int pos, double val){
		values.put(pos, val);
	}
	
}
