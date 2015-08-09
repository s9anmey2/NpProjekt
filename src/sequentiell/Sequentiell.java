package sequentiell;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import np2015.GraphInfo;
import np2015.ImageConvertible;
import np2015.GuardedCommand;
import np2015.Neighbor;

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
		double left,right,top,bottom, current;
		while(true){
			Iterator<Entry<Integer, HashMap<Integer, Double>>> columns = grid.entrySet().iterator();   
			while(columns.hasNext()){
				++x;
				column = columns.next().getValue();
				Iterator<Entry<Integer, Double>> coefficients = column.entrySet().iterator();
				while(coefficients.hasNext()){
					++y;
					bottom = graph.getRateForTarget(x,y, Neighbor.Bottom);
					left = graph.getRateForTarget(x,y, Neighbor.Left);
					right = graph.getRateForTarget(x,y, Neighbor.Top);
					top = graph.getRateForTarget(x,y, Neighbor.Right);
					current = coefficients.next().getValue();
					column.put(y, (current - (current*bottom + current*top + current*left + current*right)));
					/**naja also das funktioniert natürlich noch nich, aber ich denke, wir haben jetzt alle 
					 * funktionalität beisammen und koennen dann schauen wie wir es funktionieren lassen koennen.**/
				}
			}
			
		}
	}
	
	@Override
	public double getValueAt(int column, int row) {		
		return grid.get(column).get(row);
	}
	
	
	
	
}
