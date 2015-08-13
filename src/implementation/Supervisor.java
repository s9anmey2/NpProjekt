package implementation;

import np2015.GraphInfo;
import sequentiell.Sequentiell;

public class Supervisor {
	
	public final GraphInfo gInfo;
	public final Grid grid;
	
	private int numLocalIterations;

	public Supervisor(GraphInfo graph) {
		this.gInfo=graph;
		this.grid = new Grid(gInfo);	
		this.numLocalIterations = 100;
	}
	
	public synchronized Grid computeOsmose() {
		boolean converged = false;
		
		while(!(converged)&&(numLocalIterations > 3)){
			grid.setLocals(numLocalIterations);
			converged = grid.globalIteration();
			if(converged)
				numLocalIterations = numLocalIterations/2; 
		}
		Sequentiell seq = new Sequentiell(grid);
		return seq.compute();
	}

}
