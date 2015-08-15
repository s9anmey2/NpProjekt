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

	private int grain = 5; //grain schritte bis epsilon TODO funktioniert noch nicht richtig.
	
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
	 * Berechnet nebenläufig einen Osmoseprozess eines Gitters bis zur
	 * Konvergenz. Dabei wird am Ende sequentiell ausgeführt um die globale
	 * Konvergenz zu prüfen und gegebenenfalls noch zu erlangen.
	 * 
	 * @return Grid
	 */
	public synchronized Grid computeOsmose() {

		boolean converged = false;
		int exp = grain;
		
		grid.setEpsilonSchlange(1);

		// numLocalIterations wächst von eins bis maxLocal (sofern keine
		// Konvergenz erreicht ist) um die werte schneller zu verteilen
		while(!converged){
			grid.setLocals(numLocalIterations);
			converged = grid.globalIteration();
			if(converged || numLocalIterations >= maxLocal ){
				break;
			}
			numLocalIterations++;
		}
		
		//int i=0, j=0;
		grid.setEpsilonSchlange(Math.pow(10, exp));

		// ab jetzt wir numLocalIterations nur noch verringert
		while(!converged){
			
			//if(i++ >= 1000){
			//	i=0; gInfo.write2File("./zwischenergebnis"+ j++ +".txt", grid);
			//}
			
			grid.setLocals(numLocalIterations);
			converged = grid.globalIteration();
			if(converged){
				System.out.println("Converged " + exp + ": " + new java.text.SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(new java.util.Date())); 
				if(exp>0) {
					numLocalIterations = (int)((double)numLocalIterations * ((double)exp/(double)(exp+1)));
					exp--;
					grid.setEpsilonSchlange(Math.pow(10, exp));
					converged = false;	
				} else {
					break;
				}
			
			}
		}
		
		exe.shutdown();
		
		// zum Schluss noch die sequentielle Ausführung
		Sequentiell seq = new Sequentiell(grid);
		return seq.computeOsmose();
	}

}
