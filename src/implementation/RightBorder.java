package implementation;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Exchanger;

import np2015.GraphInfo;
import np2015.Neighbor;

/**
 * Diese Klasse erbt von Column und deckt den rechten Rand ab.
 * **/

public class RightBorder extends Column {
	
	/**
	 * Rightborder muss nur mit einem Nachbarn kommunizieren.
	 * **/
	private Hashtable<Integer, Double> outLeft;	
	private Exchanger<Hashtable<Integer,Double>> left;
	private double[][] rates;

	
	public RightBorder(GraphInfo graph, Grid grid, int y,
			Exchanger<Hashtable<Integer, Double>> left) {
		super(graph, grid, y);
		this.outLeft = new Hashtable<>(graph.height, 1);
		this.left = left;
		
		this.rates = new double[graph.height][3];
		for(int i=0; i<graph.height; i++){
			this.rates[i][0] = graph.getRateForTarget(me, i, Neighbor.Left); 
			this.rates[i][1] = graph.getRateForTarget(me, i, Neighbor.Top);
			this.rates[i][2] = graph.getRateForTarget(me, i, Neighbor.Bottom);
		}
	}	
	
	/**
	 * Kein Unterschied zur Implementierung in Middle.
	 */
	
	@Override
	public synchronized Integer call() {
		int ret;
		if(values.size()!=0)
			localIteration();
		
		Hashtable<Integer, Double> leftAccu = outLeft;
		exchange(); //nach exchange nur noch lese zugriffe auf outleft/outright. Das sichert uns die Eigenschaft von Exchange zu, weil es blockiert, bis alle Threads 
					//angekommen sind, kommt bei uns ein Thread bei Exchange an, hat er alle Schreibzugriffe (die finden nur in localiterations statt) bereits erledigt. 
		ret = getDelta(leftAccu, outLeft);
		computeNewValues();
		return ret;
	}

	/**
	 * Holt den Inflow von links ab. 
	 */
	
	@Override
	protected void exchange(){
		try {					
			outLeft = left.exchange(outLeft);
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
		
		outLeft = new Hashtable<>(graph.height, 1);
		int i = 0;
		for (i=0; i<localIterations; i++){
			akku = new Hashtable<>(graph.height, 1);
			Iterator<Entry<Integer, Double>> knoten = values.entrySet().iterator();
			while(knoten.hasNext()){
				/** hier ist keine ordnung definiert, also muss immer mit geprueft werden, ob es an der Stelle schon einen Knoten gibt.**/
				
				Entry<Integer, Double> dummy= knoten.next();
				double val = dummy.getValue();
				int currentPos = dummy.getKey();
				
	
				 val = -(setAndComputeOutflow(akku, val, currentPos-1, rates[currentPos][1])
						+setAndComputeOutflow(akku, val, currentPos+1, rates[currentPos][2])
						+setAndComputeOutflow(outLeft, val, currentPos, rates[currentPos][0]));
				
				/**der korrespondierende akku eintrag wird aktualisiert/angelegt. **/
				addOrReplaceEntry(akku, currentPos, akku.getOrDefault(currentPos,0.0) + val);

			}//while schleife zu
			
			if(addAccuToValuesAndLocalConvergence(akku, values)){

				//grid.lab.setBreak(i);
				break; //falls lokale konvergenz erreicht ist, bricht die Forschleife ab.**/
			}
		}//for schleife zu
		//if(i==localIterations)
			//grid.lab.setNoBreak(i);
	}

	@Override
	public double serialSigma(){
		/**fuer die sequentielle loesung wichtig. merkt sich in sigma die summe der quadrate aus horizontalem und vertikalem outflow **/
		double sigma=0.0;
		for (int i=0; i<graph.height; i++){
			double val = akku.getOrDefault(i, 0.0) + outLeft.getOrDefault(i, 0.0);
			sigma = sigma + val*val;		
		}
		return sigma;
	}

	@Override
	public synchronized void computeNewValues() {
		
		/**verrechnet inflow und akku mit den alten values.**/
		
			Iterator<Entry<Integer, Double>> left = outLeft.entrySet().iterator();
			while(left.hasNext()){
				Entry<Integer, Double> dummy = left.next();
				int pos = dummy.getKey();
				double val = dummy.getValue();
				addOrReplaceEntry(values, pos, values.getOrDefault(pos, 0.0) + val);			
			}//while zu
	}

	@Override
	public Hashtable<Integer, Double> getLeft() {
		return outLeft;
	}

	@Override
	public Hashtable<Integer, Double> getRight() {
		return null;
	}

	@Override
	public void setLeft(Hashtable<Integer, Double> right) {
		outLeft = right;
	}

	@Override
	public void setRight(Hashtable<Integer, Double> left) {
		return;
	}	

}
