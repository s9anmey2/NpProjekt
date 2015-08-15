package implementation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import np2015.GraphInfo;
import sequentiell.Sequentiell;

/**
 * Diese Klasse organisiert die nebenläufige Ausführung eines Osmose Prozesses.
 * Die einzige Methode ist synchronized, die Klasse stellt also einen Monitor
 * dar.
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
	 * in einer Spalte intern berechnet werden sollen. Wird diese geändert, wird
	 * sie auch im Grid gesetzt.
	 */
	private int numLocalIterations;
	/**
	 * maxLocal ist die obere Schranke von numLocalIterations, damit die
	 * Berechnung nicht zu ungenau wird.
	 */
	private int maxLocal;

	/**
	 * Erzeugt ein neues Supervisor Objekt.
	 * 
	 * @param graph
	 */
	public Supervisor(GraphInfo graph) {
		this.exe = Executors.newFixedThreadPool(graph.width);
		this.gInfo = graph;
		this.grid = new Grid(gInfo, exe);
		grid.setLocals(1);
		this.numLocalIterations = 1;
		this.maxLocal = graph.width * graph.height * 7;
	}

	/**
	 * Berechnet nebenläufig einen Osmoseprozess eines Gitters bis zur
	 * Konvergenz. Dabei wird am Ende sequentiell ausgeführt um die globale
	 * Konvergenz zu prüfen und gegebenenfalls noch zu erlangen.
	 * 
	 * @return Grid welches ImageConvertible implementiert.
	 */
	public synchronized Grid computeOsmose() {

		/*
		 * converged gibt an ob das System schon lokal konvergiert ist, also ob
		 * für alle Übergänge zwischen den Spalten gilt: Flow nach rechts ~ Flow
		 * nach links Kriterium für die Konvergenz ist:
		 */
		int converged = 1;

		/*
		 * numLocalIterations wächst von eins bis maxLocal (sofern keine
		 * Konvergenz erreicht ist) um die werte schneller zu verteilen.
		 */
		while (!(converged == 0)) { // bleibt in der Schleife solnag gilt:
									// epsilon<delta, delta/previousdelta>1.0001
			converged = grid.globalIteration();
			if ((converged == 0) || numLocalIterations >= maxLocal) {
				break;
			}
			numLocalIterations++;
			grid.setLocals(numLocalIterations);

		}

		/*
		 * Ab jetzt wird numLocalIteration nur noch verringert.
		 */
		while (!(converged == 0)) {
			// TODO hier muessen wir die Anzahl der localen Iterationen
			// veraendern, falls converged < -1!!
			converged = grid.globalIteration();
		}

		exe.shutdown();

		/*
		 * Zum Schluss noch die sequentielle Ausführung um das globale
		 * Konvergenzkriterium zu prüfen und eventuell die globale Konvergenz
		 * noch zu erreichen.
		 */
		Sequentiell seq = new Sequentiell(grid);

		// TODO entfernen: grid.lab.print();
		return seq.computeOsmose();
	}

}
