package sequentiell;


import np2015.GraphInfo;

import implementation.Grid;

public class Sequentiell {
	private Grid grid;

	public Sequentiell(GraphInfo graph){
		this.grid = new Grid(graph);
		this.grid.setLocals(1);
	}
	
	public Sequentiell(Grid grid){
		
		/**der konstruktor ist fuer die nebenlauefge loesung, in der irgendwann auf sequentielle berechnung gewechselt wird.**/
		this.grid = grid;
		this.grid.setLocals(1);
	}
		
	/**Bisher wird hier nur über das Grid im ganzen iteriert, berechnet wird noch nix.**/
	
	public Grid compute(){
		boolean converged = false;
		while(!(converged)){
			converged = grid.serialComputation();
		}
		return grid;
	}
	
	
}
