package src.implementation;

import src.np2015.GraphInfo;

public class Supervisor {
	
	public final GraphInfo gInfo;
	public final Grid grid;
	public final double epsilon;
	
	private int numLocalIterations;
	private double valueForAbortionCheck;

	public Supervisor() {
		gInfo=null;grid=null;epsilon=0;
	}
	
	public synchronized Grid computeOsmose() {
		Grid grid = new Grid(gInfo);
		boolean converged = false;
		
		while(!(converged))
			converged = grid.globalIteration();
		return grid;		//berechnung der lokalen schrittzahl fehlt noch.
	}
	
	public synchronized int getLocals(){
		return numLocalIterations;
	}

}
