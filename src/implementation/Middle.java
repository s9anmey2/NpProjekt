package implementation;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Exchanger;

import np2015.GraphInfo;
import np2015.Neighbor;

public class Middle extends Column {

	/**
	 * outLeft und outRight enthalten die Werte, die nach den lokalen
	 * Iterationsschritten an die Nachbarspalten abgegeben werden und 
	 * werden zu Beginn eines globalen Iterationsschritts geleert.
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
			this.rates[i][Neighbor.Left.ordinal()] = graph.getRateForTarget(me, i, Neighbor.Left); 
			this.rates[i][Neighbor.Top.ordinal()] = graph.getRateForTarget(me, i, Neighbor.Top);
			this.rates[i][Neighbor.Right.ordinal()] = graph.getRateForTarget(me, i, Neighbor.Right);
			this.rates[i][Neighbor.Bottom.ordinal()] = graph.getRateForTarget(me, i, Neighbor.Bottom);
		}
	}
	
	@Override
	public synchronized Boolean call() {
		/**berechnet den akku und den horizontalen outflow knotenweise.**/
		boolean ret;

		if(values.size()!=0)
			localIteration();

		Hashtable<Integer, Double> leftAccu = outLeft;
		exchange();
		ret = getDelta(leftAccu, outLeft);

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
		outLeft = new Hashtable<>(graph.height, 1);
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
				
	
				 val = -(setAndComputeOutflow(akku, val, currentPos-1, rates[currentPos][Neighbor.Top.ordinal()])
						+setAndComputeOutflow(akku, val, currentPos+1, rates[currentPos][Neighbor.Bottom.ordinal()])
						+setAndComputeOutflow(outLeft, val, currentPos, rates[currentPos][Neighbor.Left.ordinal()])
						+setAndComputeOutflow(outRight, val, currentPos, rates[currentPos][Neighbor.Right.ordinal()]));
				
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
			double val = akku.getOrDefault(i, 0.0) + outLeft.getOrDefault(i, 0.0) + outRight.getOrDefault(i,0.0);
			sigma = sigma + val*val;		
		}
		return sigma;
	}
	
	@Override
	public synchronized void computeNewValues() {
		
		/**verrechnet inflow und akku mit den alten values.**/
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
