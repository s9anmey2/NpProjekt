package implementation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import np2015.GraphInfo;
import sequentiell.Sequentiell;

/**
 * Diese Klasse organisiert die nebenläufige Ausführung eines Osmose Prozesses.
 * Die einzige Methode ist synchronized, die Klasse stellt also einen Monitor dar.
 */
public class Supervisor {
	
	private final GraphInfo gInfo;
	/**
	 * Gitter auf dem gearbeitet wird.
	 */
	private final Grid grid;
	private final ExecutorService exe;
	/**
	 * numLocalIterations ist die aktuelle Anzahl der lokalen Iterationen, die
	 * in einer Spalte intern berechnet werden sollen. Wird diese geändert,
	 * wird sie auch im Grid gesetzt. 
	 */
	private int numLocalIterations;
	/**
	 * maxLocal ist die obere Schranke von numLocalIterations, damit die 
	 * Berechnung nicht zu ungenau wird.
	 */
	private int maxLocal;
	
	public Supervisor(GraphInfo graph) {
		this.exe = Executors.newFixedThreadPool(graph.width);
		this.gInfo=graph;
		this.grid = new Grid(gInfo, exe);	
		grid.setLocals(1);
		this.numLocalIterations = 1;
		this.maxLocal = graph.width*graph.height*7;
	}
	
	/**
	 * 
	 * Die großen Fragen unsrer Zeit: was is maxLocal? Wie wächst numLocal? wie verringern wir numLocal? wie variieren wir epsilon? gibt es einen determinator für 
	 * alle?
	 * 
	 * **/
	
	/**
	 * Berechnet nebenläufig einen Osmoseprozess eines Gitters bis zur
	 * Konvergenz. Dabei wird am Ende sequentiell ausgeführt um die globale
	 * Konvergenz zu prüfen und gegebenenfalls noch zu erlangen.
	 * 
	 * @return Grid
	 */
	public synchronized Grid computeOsmose() {

		boolean converged = false;

		// numLocalIterations wächst von eins bis maxLocal (sofern keine
		// Konvergenz erreicht ist) um die werte schneller zu verteilen
		while(!converged){
			converged = grid.globalIteration();
			if(converged || numLocalIterations >= maxLocal ){
				break;
			}
			numLocalIterations++;
			grid.setLocals(numLocalIterations);

		}
		
		//int i=0, j=0;

		// ab jetzt wir numLocalIterations nur noch verringert
		while(!converged){
			//if(i++ >= 1000){
			//	i=0; gInfo.write2File("./zwischenergebnis"+ j++ +".txt", grid);
			//}
			
			converged = grid.globalIteration();
		}
		
		exe.shutdown();
		
		// zum Schluss noch die sequentielle Ausführung
		Sequentiell seq = new Sequentiell(grid);

		//grid.lab.print();
		return seq.computeOsmose();
	}

}
