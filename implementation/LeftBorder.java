package implementation;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Exchanger;

import np2015.GraphInfo;
import np2015.Neighbor;

public class LeftBorder extends Column {
	
	private Hashtable<Integer, Double> outRight;
	
	private Exchanger<Hashtable<Integer,Double>> right;
	private double[][] rates;
	
	
	public LeftBorder(GraphInfo graph, Grid grid, int y,
			Exchanger<Hashtable<Integer, Double>> right) {
		super(graph, grid, y);

		this.outRight = new Hashtable<>();
		this.right = right;
		
		this.rates = new double[graph.height][3];
		for(int i=0; i<graph.height; i++){
			this.rates[i][0] = graph.getRateForTarget(me, i, Neighbor.Top);
			this.rates[i][1] = graph.getRateForTarget(me, i, Neighbor.Right);
			this.rates[i][2] = graph.getRateForTarget(me, i, Neighbor.Bottom);
		}
	}

	@Override
	public synchronized Boolean call() {
		/**berechnet den akku und den horizontalen outflow knotenweise.**/

		if(values.size()!=0)
			localIteration();
		exchange();
		computeNewValues();
		return true;
		
	}

	@Override
	protected void exchange(){
		try {
			outRight = right.exchange(outRight);	
		} catch (InterruptedException e) {
				System.out.println("Exchange failed :/");
				e.printStackTrace();
		}
	}

	@Override
	public void localIteration(){

		outRight= new Hashtable<>();
		int localIterations = grid.getLocals();

		for (int i=0; i<localIterations; i++){
			akku = new Hashtable<>();
			Iterator<Entry<Integer, Double>> knoten = values.entrySet().iterator();
			while(knoten.hasNext()){
				/** hier ist keine ordnung definiert, also muss immer mit geprueft werden, ob es an der Stelle schon einen Knoten gibt.**/
				
				Entry<Integer, Double> dummy= knoten.next();
				double val = dummy.getValue();
				int currentPos = dummy.getKey();
				
	
				 val = -(setAndComputeOutflow(akku, val, currentPos-1, rates[currentPos][0])
						+setAndComputeOutflow(akku, val, currentPos+1, rates[currentPos][2])
						+setAndComputeOutflow(outRight, val, currentPos, rates[currentPos][1]));
				
				/**der korrespondierende akku eintrag wird aktualisiert/angelegt. **/
				addOrReplaceEntry(akku, currentPos, akku.getOrDefault(currentPos,0.0) + val);

			}//while schleife zu
			
			if(addAccuToValuesAndLocalConvergence(akku, values))
				break;// falls lokale konvergenz erreicht ist, bricht die Forschleife ab.
			
		}//for schleife zu
	}

	@Override
	public double serialSigma(){
		/**fuer die sequentielle loesung wichtig. merkt sich in sigma die summe der quadrate aus horizontalem und vertikalem outflow **/
		double sigma=0.0;
		for (int i=0; i<graph.height; i++){
			double val = akku.getOrDefault(i, 0.0) + outRight.getOrDefault(i,0.0);
			sigma = sigma + val*val;		
		}
		return sigma;
	}

	@Override
	public synchronized void computeNewValues() {
		
		/**verrechnet inflow computeNewValues1und akku mit den alten values.**/
			Iterator<Entry<Integer, Double>> right= outRight.entrySet().iterator();
			while(right.hasNext()){
				Entry<Integer, Double> dummy = right.next();
				int pos = dummy.getKey();
				double val = dummy.getValue();
				addOrReplaceEntry(values, pos, values.getOrDefault(pos, 0.0) + val);			
			}//while zu

	}

	@Override
	public Hashtable<Integer, Double> getLeft() {
		return null;
	}

	@Override
	public Hashtable<Integer, Double> getRight() {
		return outRight;
	}

	@Override
	public void setLeft(Hashtable<Integer, Double> right) {
		return;
	}

	@Override
	public void setRight(Hashtable<Integer, Double> left) {
		outRight = left;
	}

}
