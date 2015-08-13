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
			
	public Grid compute(){
		boolean converged = false;
		int counter= 0;
		while(!(converged)){
			converged = grid.serialComputation();
			//System.out.println("sum at " + counter + " is " + grid.getSum());
			try{
				assert(grid.getSum()==1.0);
			}catch (Exception e){
				System.out.println("assert failed bei : " + counter);
			}
			counter++;
		}
		return grid;
	}
	
	
}
