package implementation;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Exchanger;

import np2015.GraphInfo;
import np2015.Neighbor;

/**
 *Der allgemeine Fall: Alle Spalten mit zwei Nachbarn sind Unterklasse Middle.
 */

public class Middle extends Column {

	/**
	 * outLeft und outRight enthalten die Werte, die nach den lokalen Iterationsschritten an die Nachbarspalten abgegeben werden und  werden zu Beginn eines globalen 
	 * Iterationsschritts geleert.
	 */
	
	private Hashtable<Integer, Double> outLeft;	
	private Hashtable<Integer, Double> outRight;
	
	private Exchanger<Hashtable<Integer,Double>> left, right;
	private double[][] rates;

	
	public Middle(GraphInfo graph, Grid grid, int y,
			Exchanger<Hashtable<Integer, Double>> left,
			Exchanger<Hashtable<Integer, Double>> right) {
		super(graph, grid, y);

		this.outRight = new Hashtable<>(graph.height, 1);
		this.outLeft = new Hashtable<>(graph.height, 1);
		this.left = left;
		this.right = right;
		
		this.rates = new double[graph.height][4];
		for(int i=0; i<graph.height; i++){
			this.rates[i][0] = graph.getRateForTarget(me, i, Neighbor.Left); 
			this.rates[i][2] = graph.getRateForTarget(me, i, Neighbor.Top);
			this.rates[i][1] = graph.getRateForTarget(me, i, Neighbor.Right);
			this.rates[i][3] = graph.getRateForTarget(me, i, Neighbor.Bottom);
		}
	}
	
	/**
	 * Berechnet den akku und den horizontalen outflow knotenweise. Der horizontale Flow wird nach jeder lokalen Iteration mit Exchange getauscht und dann werden 
	 * die values mit computeNewValues() neu berechnet. Die Arbeit mit dem Akku findet in localIteration() statt.
	 * 
	 * @return die Information, ob Konvergenz erreicht ist, nicht erreicht ist, oder ob ein Zyklus vorliegt.
	 */
	@Override
	public synchronized Double call() {
		/**berechnet den akku und den horizontalen outflow knotenweise.**/
		double ret;

		if(values.size()!=0)
			localIteration();

		Hashtable<Integer, Double> rightAccu = outRight;
		Hashtable<Integer, Double> leftAccu = outLeft;
		
		exchange();
		ret = getDelta(leftAccu, outLeft, rightAccu, outRight);
		computeNewValues();
		return ret;
		
	}
	
	
	
	@Override
	protected void exchange(){
		try {
			if(me % 2 == 0){ //grade erst nach links tauschen, ungerade erst nach rechts.

				outLeft = left.exchange(outLeft);
				outRight = right.exchange(outRight);
			}else{

				outRight = right.exchange(outRight);
				outLeft = left.exchange(outLeft);
			}
		} catch (InterruptedException e) {
				System.out.println("Exchange failed :/");
				e.printStackTrace();
		}
	}
	

	@Override
	public void localIteration(){
		/*zu Beginn jedes globalen Iterationsschrittes muessen die Akkus des horizontalen Outflows auf 0 gesetzt werden*/
		outLeft = new Hashtable<>(graph.height, 1);
		outRight= new Hashtable<>(graph.height, 1);
		int i = 0;
		for (i=0; i<localIterations; i++){
			/*zu Beginn jedes lokalen Iterationschrittes muss der Akku des vertikalen Flows einer Spalte auf 0 gesetzt werden.*/
			akku = new Hashtable<>(graph.height, 1);
			Iterator<Entry<Integer, Double>> knoten = values.entrySet().iterator();
			while(knoten.hasNext()){
				
				Entry<Integer, Double> dummy= knoten.next();
				double val = dummy.getValue();
				int currentPos = dummy.getKey();
				/*der outflow in jede Richtung wird berechnet. der berechnete Wert wird in den Akkus der empfangenden Knoten abgelegt.
				 * Dasuebernimmt die Methode setAndComputeValues. Die Summe des outflows wird lokal gespeichert. */
				 val = -(setAndComputeOutflow(akku, val, currentPos-1, rates[currentPos][Neighbor.Top.ordinal()])
						+setAndComputeOutflow(akku, val, currentPos+1, rates[currentPos][Neighbor.Bottom.ordinal()])
						+setAndComputeOutflow(outLeft, val, currentPos, rates[currentPos][Neighbor.Left.ordinal()])
						+setAndComputeOutflow(outRight, val, currentPos, rates[currentPos][Neighbor.Right.ordinal()]));
				
				/*der akku eintrag des aktuellen Knotens wird um den kumulierten Outflow ergaenzt. */
				addOrReplaceEntry(akku, currentPos, akku.getOrDefault(currentPos,0.0) + val);

			}//while schleife zu
			
			/*Am Ende jedes lokalen Iterationschrittes werden die Werte der Knoten einer Spalte mit ihrem korrespondierenden Akku 
			 Eintrag verrechnet. Erreicht der vertikale Flow ein Gleichgewicht, also erfuellt ein Konvergenzkriterium, bricht die 
			 lokalte Iteration ab.*/
			if(addAccuToValuesAndLocalConvergence(akku, values))
				break; 
		}//for schleife zu
	}
	
	@Override
	public double serialSigma(){
		/**fuer die sequentielle loesung wichtig. merkt sich in sigma die summe der quadrate aus horizontalem und vertikalem outflow **/
		double sigma=0.0;
		for (int i=0; i<graph.height; i++){
			double val = akku.getOrDefault(i, 0.0) + outLeft.getOrDefault(i, 0.0) + outRight.getOrDefault(i,0.0);
			sigma = sigma + val*val;		
		}
		return sigma;
	}
	
	@Override
	public synchronized void computeNewValues() {
		
		if(me>0){
			Iterator<Entry<Integer, Double>> left = outLeft.entrySet().iterator();
			while(left.hasNext()){
				Entry<Integer, Double> dummy = left.next();
				int pos = dummy.getKey();
				double val = dummy.getValue();
				addOrReplaceEntry(values, pos, values.getOrDefault(pos, 0.0) + val);			
			}//while zu
			
		}if(me<graph.width){
			Iterator<Entry<Integer, Double>> right= outRight.entrySet().iterator();
			while(right.hasNext()){
				Entry<Integer, Double> dummy = right.next();
				int pos = dummy.getKey();
				double val = dummy.getValue();
				addOrReplaceEntry(values, pos, values.getOrDefault(pos, 0.0) + val);			
			}//while zu
		}

	}
	
	/**Die setter und getter brauchen wir fuer den sequentiellen programmteil.**/
	@Override
	public synchronized Hashtable<Integer, Double> getLeft(){
		return outLeft;
	}
	
	@Override
	public synchronized Hashtable<Integer, Double> getRight(){
		return outRight;
	}
	
	@Override
	public synchronized void setLeft(Hashtable<Integer, Double> right){
		outLeft = right;
	}
	
	@Override
	public synchronized void setRight(Hashtable<Integer, Double> left){
		outRight= left;
	}
	
}
