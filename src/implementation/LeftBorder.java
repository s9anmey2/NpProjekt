package implementation;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Exchanger;

import np2015.GraphInfo;
import np2015.Neighbor;

/**
 * Diese Klasse erbt von Column und deckt den linken Rand ab.
 * **/

public class LeftBorder extends Column {
	
	/**
	 * Leftborder muss nur mit einem Nachbarn kommunizieren.
	 * **/
	private Hashtable<Integer, Double> outRight;
	private Exchanger<Hashtable<Integer,Double>> right;
	private double[][] rates;
	
	
	public LeftBorder(GraphInfo graph, Grid grid, int y,
			Exchanger<Hashtable<Integer, Double>> right) {
		super(graph, grid, y);

		this.outRight = new Hashtable<>(graph.height, 1);
		this.right = right;
		
		this.rates = new double[graph.height][3];
		for(int i=0; i<graph.height; i++){
			this.rates[i][0] = graph.getRateForTarget(me, i, Neighbor.Top);
			this.rates[i][1] = graph.getRateForTarget(me, i, Neighbor.Right);
			this.rates[i][2] = graph.getRateForTarget(me, i, Neighbor.Bottom);
		}
	}

	/**
	 * Im Unterschied zu den uebirgen Faellen, muss leftBorder nicht ueberpruefen, ob das Konvergenzkriterium erfuellt ist, dass erledigt der rechte Nachbar.
	 * 
	 * @return immer 0.
	 */
	
	@Override
	public synchronized Double call() {
		double ret;

		if(values.size()!=0)
			localIteration();
		Hashtable<Integer, Double> rightAccu = outRight;
		exchange();
		ret = getDelta(rightAccu, outRight);
		computeNewValues();
		return ret;
		
	}
	
	/**
	 * Holt den Inflow von rechts ab. 
	 */

	@Override
	protected void exchange(){
		
		try {
			outRight = right.exchange(outRight);	
		} catch (InterruptedException e) {
				System.out.println("Exchange failed :/");
				e.printStackTrace();
		}
	}

	/**
	 * Der Unterschied zum allgemeinen Fall ist, dass setAndComputeOutflow fuer drei statt vier Nachbarn aufgerufen wird. 
	 */
	
	@Override
	public void localIteration(){

		outRight= new Hashtable<>(graph.height, 1);
		int i = 0;
		for (i=0; i<localIterations; i++){
			akku = new Hashtable<>(graph.height, 1);
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
				break; //falls lokale konvergenz erreicht ist, bricht die Forschleife ab.**/
		}//for schleife zu
	}
	
	/**
	 * Diese Methode verwendet die sequentielle Loesung. merkt sich in sigma die summe der quadrate aus horizontalem und vertikalem outflow 
	 * 
	 * @return sigma = sum(akku(i)^2), 0<=i<graph.height.
	 */
	
	@Override
	public double serialSigma(){
		double sigma=0.0;
		for (int i=0; i<graph.height; i++){
			double val = akku.getOrDefault(i, 0.0) + outRight.getOrDefault(i,0.0);
			sigma = sigma + val*val;		
		}
		return sigma;
	}
	/**
	 * Berechnet nach jeder lokalen Iteration values neu: value(i) = value(i) + outRight(i)
	 * **/
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

	/**
	 * Da Leftborder keinen linken NAchbarn hat, gibts auch keinen linken Flow zurueck.
	 * 
	 * @return null.
	 */
	
	@Override
	public Hashtable<Integer, Double> getLeft() {
		return null;
	}

	@Override
	public Hashtable<Integer, Double> getRight() {
		return outRight;
	}

	/**
	 * Da Leftborder keinen linken NAchbarn hat, kann hier auch nichts gesetzt werden.
	 */
	@Override
	public void setLeft(Hashtable<Integer, Double> right) {
		return;
	}

	@Override
	public void setRight(Hashtable<Integer, Double> left) {
		outRight = left;
	}

}
