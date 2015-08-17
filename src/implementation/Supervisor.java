package implementation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import np2015.GraphInfo;

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
	 * Berechnung nicht zu ungenau wird. Ist der Wert zu hoch, kann es
	 * vorkommen, dass sich ein Gleichgewicht des vertikalen Flows einstellt und
	 * Rechenzeit verloren geht, ohne dass noch Aenderungen erreicht werden. Um
	 * diesen Fall prinzipiell auszuschließen, ist als weitere Abbruchbedingung
	 * ein vertikal-lokales Konvergenzkriterium implementiert, dass geprueft
	 * wird. Die lokale Iteration bricht ab, wenn es erreicht ist.
	 * 
	 * Dieser Parameter kann individuell angepasst werden.
	 */
	private int maxLocal = 512;

	/**
	 * Erzeugt ein neues Supervisor Objekt.
	 * 
	 * @param graph
	 *            GraphInfo Objekt, welches die initialen Werte und Raten zur
	 *            Verfügung stellt.
	 */
	public Supervisor(GraphInfo graph) {
		this.exe = Executors.newFixedThreadPool(
				graph.width); /**
								 * (s.http://docs.oracle.com/javase/7/docs/api/
								 * java/util/concurrent/ExecutorService.html)
								 */
		this.gInfo = graph;
		this.grid = new Grid(gInfo, exe);
		grid.setLocals(1);
		this.numLocalIterations = 2;
	}

	/**
	 * Berechnet nebenläufig einen Osmoseprozess eines Gitters bis zur
	 * Konvergenz. Ist Konvergenz oder ein Zyklus erreicht , wird die Zahl
	 * lokaler Iterationschritte reduziert und weiter gerechnet. Ist die
	 * Schrittzahl == 1, wechselt der Supervisor vom nebenlauefigen in den
	 * sequentiellen Modus.
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
		 * Iterationsschritte hinweg keine Verbesserung mehr erzielt wird. So
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
