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
	private int maxLocal;
	
	private int grain = 0; //grain schritte bis epsilon
	
	public Supervisor(GraphInfo graph) {
		this.exe = Executors.newFixedThreadPool(graph.width);
		this.gInfo=graph;
		this.grid = new Grid(gInfo, exe);	
		this.numLocalIterations = 1;
		this.maxLocal = graph.width*graph.height;
	}
	/**
	 * 
	 * Die großen Fragen unsrer Zeit: was is maxLocal? Wie wächst numLocal? wie verringern wir numLocal? wie variieren wir epsilon? gibt es einen determinator für 
	 * alle?
	 * 
	 * **/
	/**
	 * 
	 * @return
	 */
	public synchronized Grid computeOsmose() {

		boolean converged = false;
		int exp = grain;
		
		grid.setEpsilonSchlange(Math.pow(10, exp));
		
		while(!converged){
			
			grid.setLocals(numLocalIterations);
			converged = grid.globalIteration();
			
			if(converged){
				
				System.out.println("Converged " + exp + ": " + new java.text.SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(new java.util.Date())); 
				
				if(exp>0) {
					numLocalIterations = numLocalIterations * (exp/(exp+1));
					exp--;
					grid.setEpsilonSchlange(Math.pow(10, exp));
					converged = false;	
				} else {
					break;
				}
			
			}else if(numLocalIterations < maxLocal){
				numLocalIterations++;
			}
		}
		
		exe.shutdown();
		Sequentiell seq = new Sequentiell(grid);
		return seq.compute();
	}

}
