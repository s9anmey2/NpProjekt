package implementation;

import np2015.GraphInfo;

public class Supervisor {
	
	public final GraphInfo gInfo;
	public final Grid grid;
	public final double epsilon;
	
	private int numLocalIterations;
	private double valueForAbortionCheck;

	public Supervisor() {
		gInfo=null;grid=null;epsilon=0;
		// TODO Auto-generated constructor stub
	}
	
	public synchronized Grid computeOsmose() {
		// TODO
		return null;
	}
	
	public synchronized int getLocals(){
		return numLocalIterations;
	}

}
