package sequentiell;

import java.util.HashMap;

import np2015.Neighbor; /**Das ist noch ein bisschen problematisch, aber denke bisschen googeln will do it, kann nur 
kann nur grade nicht weil ich im zug sitze. **/
import np2015.GraphInfo;
import np2015.ImageConvertible;
import np2015.GuardedCommand;

public class Sequentiell implements ImageConvertible{
	private GraphInfo graph;
	private HashMap<Integer, HashMap<Integer, Double>> grid = new HashMap<>();

	public Sequentiell(GraphInfo graph){
		this.graph = graph;
		this.compute();
	}
	
	private void compute(){
		int x=0;
		int y=0;
		double akku;
		while(true){
			++x;
			graph.getRateForTarget(x, y, Left);
		}
	}
	
	@Override
	public double getValueAt(int column, int row) {		
		return grid.get(column).get(row);
	}
	
	
	
	
}
