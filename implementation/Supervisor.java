package implementation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import np2015.GraphInfo;
import sequentiell.Sequentiell;

public class Supervisor {
	
	private final GraphInfo gInfo;
	private final Grid grid;
	private final ExecutorService exe;
	private int numLocalIterations;
	
	public Supervisor(GraphInfo graph) {
		this.exe = Executors.newFixedThreadPool(graph.width);
		this.gInfo=graph;
		this.grid = new Grid(gInfo, exe);	
		this.numLocalIterations = 100;
	}
	
	/**
	 * 
	 * @return
	 */
	public synchronized Grid computeOsmose() {
		boolean converged = false;
		
		while(!(converged)&&(numLocalIterations > 3)){
			grid.setLocals(numLocalIterations);
			converged = grid.globalIteration();
			if(converged)
				numLocalIterations = numLocalIterations/2; 
		}
		exe.shutdown();
		Sequentiell seq = new Sequentiell(grid);
		return seq.compute();
	}

}
