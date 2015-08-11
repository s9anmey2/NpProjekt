package sequentiell;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import np2015.GraphInfo;

import implementation.Grid;
import implementation.Column;

public class Sequentiell {
	private Grid grid;

	public Sequentiell(GraphInfo graph){
		this.grid = new Grid(graph);
		this.grid.setLocals(1);
		Iterator<Entry<Integer,HashMap<Integer,Double>>> iter  = graph.column2row2initialValue.entrySet().iterator();
		
		while(iter.hasNext()){
			Entry<Integer, HashMap<Integer, Double>> forGrid = iter.next();
			HashMap<Integer,Double> spalte =forGrid.getValue();
			Column column = new Column(graph, grid);
			Iterator<Entry<Integer,Double>> iter2 = spalte.entrySet().iterator();
			
			while(iter2.hasNext()){
				Entry<Integer,Double> ent = iter2.next();
				column.setValue(ent.getKey(),ent.getValue());
			}//innere Schleife
			
			grid.addColumn(forGrid.getKey(), column);
		}//aeußere Schleife
		
		this.compute();
	}
	
	public Sequentiell(Grid grid){
		this.grid = grid;
		this.grid.setLocals(1);
	}
		
	/**Bisher wird hier nur über das Grid im ganzen iteriert, berechnet wird noch nix.**/
	
	private void compute(){
		
	}
	
	
}
