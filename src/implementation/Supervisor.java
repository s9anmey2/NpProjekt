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
	/*
	 * Gitter auf dem gearbeitet wird.
	 */
	private final Grid grid;
	private final ExecutorService exe;
	/*
	 * numLocalIterations ist die aktuelle Anzahl der lokalen Iterationen, die
	 * in einer Spalte intern berechnet werden sollen. Wird diese geändert, wird
	 * sie auch im Grid gesetzt.
	 */
	private int numLocalIterations;
	/*
	 * maxLocal ist die obere Schranke von numLocalIterations, damit die
	 * Berechnung nicht zu ungenau wird.
	 */
	private int maxLocal;

	/**
	 * Erzeugt ein neues Supervisor Objekt.
	 * 
	 * @param graph
	 *            GraphInfo Objekt, welches die initialen Werte und Raten zur
	 *            Verfügung stellt.
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
	 * @return Grid Objekt (implementiert ImageConvertible)
	 */
	public synchronized Grid computeOsmose() {
		boolean converged = false;
		/*
		 * numLocalIterations wächst von eins bis maxLocal (sofern keine
		 * Konvergenz erreicht ist) um die Werte schneller zu verteilen.
		 */
		while ((!converged) && !(numLocalIterations >= maxLocal)) {
			converged = grid.globalIteration();
			numLocalIterations++;
			grid.setLocals(numLocalIterations);
		}
		/*
		 * Ab jetzt wird numLocalIteration nur noch verringert. globalIteration
		 * gibt true zurück, wenn Inflow ~ Outflow oder über die
		 * Iterationsschritte hinweg keine Verbesserung mehr erziehlt wird. So
		 * wird sichergestellt, dass bei Zyklen die Anzahl der lokalen
		 * Iterationen verringert wird, was der Terminierung dient.
		 */
		while (numLocalIterations > 1) {
			while (!converged) {
				converged = grid.globalIteration();
			}
			numLocalIterations = numLocalIterations / 2;
			grid.setLocals(numLocalIterations);
			converged = false;
		}
		/*
		 * Im sequentiellen Teil wird der ExecutorService nicht mehr benötigt,
		 * daher kann er nun beendet werden.
		 */
		exe.shutdown();
		/*
		 * Zum Schluss noch die sequentielle Ausführung um das globale
		 * Konvergenzkriterium zu prüfen und eventuell die globale Konvergenz
		 * noch zu erreichen.
		 */
		Sequentiell seq = new Sequentiell(grid);
		return seq.computeOsmose();
	}

}
