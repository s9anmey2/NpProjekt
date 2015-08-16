package implementation;

import np2015.GraphInfo;

/**
 * Diese Klasse organisiert die sequentielle Ausführung eines Osmose Prozesses.
 * Diese Klasse ist ein Monitor. Ist fuer die konkrete Implementierung nicht
 * wichtig, aber man soll ja immer auf erweiterbarkeit achten.
 */
public class Sequentiell {

	/*
	 * Gitter auf dem gearbeitet wird.
	 */
	private Grid grid;

	/**
	 * Der Konstruktor erzeugt mit dem übergegebenen GraphInfo Objekt ein neues
	 * Gitter.
	 * 
	 * @param graph
	 *            GraphInfo Objekt, welches die initialen Werte und Raten zur
	 *            Verfügung stellt.
	 */
	public Sequentiell(GraphInfo graph) {
		this.grid = new Grid(graph);
		this.grid.setLocals(1);
	}

	/**
	 * Der Konstruktor nimmt ein Grid entgegen, damit auf einem nicht initialen
	 * Grid weiter gerechnet werden kann.
	 * 
	 * @param grid
	 *            Grid Objekt
	 */
	public Sequentiell(Grid grid) {
		this.grid = grid;
		this.grid.setLocals(1);
	}

	/**
	 * Berechnet sequentiell einen Osmoseprozess eines Gitters bis zur
	 * Konvergenz.
	 * 
	 * @return Grid Objekt (implementiert ImageConvertible)
	 */
	public synchronized Grid computeOsmose() {
		System.out.println(
				"Sequentiell: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(new java.util.Date()));
		boolean converged = false;
		while (!(converged)) {
			converged = grid.serialComputation();
		}
		return grid;
	}

}
