package sequentiell;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import np2015.GraphInfo;
import np2015.ImageConvertible;
import np2015.GuardedCommand;

public class Sequentiell implements ImageConvertible{
	private GraphInfo graph;
	private HashMap<Integer, HashMap<Integer, Double>> grid = new HashMap<>();

	public Sequentiell(GraphInfo graph){
		this.graph = graph;
		this.grid = graph.column2row2initialValue;
		this.compute();
	}
	
	/**Bisher wird hier nur über das Grid im ganzen iteriert, berechnet wird noch nix.**/
	
	private void compute(){
		HashMap<Integer, Double> column = new HashMap<>();
		int x = 0,y = 0;/**die brauchen wir um unsere koeffizieten zu identifiezieren, dass wir getneighbor aufrufen 
		koennen.**/
		while(true){
			Iterator<Entry<Integer, HashMap<Integer, Double>>> columns = grid.entrySet().iterator();   
			while(columns.hasNext()){
				++x;
				column = columns.next().getValue();
				Iterator<Entry<Integer, Double>> coefficients = column.entrySet().iterator();
				while(coefficients.hasNext()){
					++y;
					/*graph.column2row2initialValue(x,y,Left);*//**Wie das mit dem enum funktioniert, kA. Import oder so 
					jedenfalls nich.**/
					double dummy = coefficients.next().getValue(); /**hier haben wir jetzt einen grid(x,y)=greyvalue
					 gerechnet wird aber noch nix**/
				}
			}
			
		}
	}
	
	@Override
	public double getValueAt(int column, int row) {		
		return grid.get(column).get(row);
	}
	
	
	
	
}
