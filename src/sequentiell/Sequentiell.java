package src.sequentiell;

import src.np2015.GraphInfo;

import src.implementation.Grid;

/**
 * Diese Klasse organisiert die sequentielle Ausführung eines Osmose Prozesses.
 */
public class Sequentiell {

	/**
	 * Gitter auf dem gearbeitet wird.
	 */
	private Grid grid;

	/**
	 * Der Konstruktor erzeugt mit dem übergegebenen GraphInfo Objekt ein neues
	 * Gitter.
	 * 
	 * @param graph
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
	 */
	public Sequentiell(Grid grid) {
		this.grid = grid;
		this.grid.setLocals(1);
	}

	/**
	 * Berechnet sequentiell einen Osmoseprozess eines Gitters bis zur
	 * Konvergenz.
	 * 
	 * @return Grid
	 */
	public synchronized Grid compute() {

		boolean converted = false;

		while (!(converted)) {
			converted = grid.serialComputation();
		}

		return grid;
	}

}
