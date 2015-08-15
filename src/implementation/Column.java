package implementation;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import np2015.GraphInfo;

/**
 * Die Klasse Column stellt eine Spalte eines Gitters dar und enthält Methoden, welche die Berechnungen ausführen, die nur eine Spalte betreffen. 
 *  
 *  Das Gitter in Spalten zu unterteilen, die parallel bearbeitet werden, bedingt zwei Randfaelle. Um diese abzufangen organisieren wir die Menge aller Spalten
 *  in einer Klassenhierarchie, mit 3 Kindern,  von denen eines den linken, eines den rechten Rand implementiert und das dritte den allgemeinen Fall.
 *  **/
abstract public class Column implements Callable<Double>{
	
	/**
	 * values enthält die aktuellen Werte der Spalte.
	 */
	protected Hashtable<Integer, Double> values;
	
	/**
	 * akku enthält die Werde des vertikalen Flows und wird zu Beginn 
	 * jeden lokalen Iterationsschritts geleert.
	 */
	protected Hashtable<Integer, Double> akku;
	
	/**
	 * sigma enthält am Ende der lokalen Iteration die Quadrate der Akkuwerte
	 * und dient der Verhinderung unnötig vieler lokaler Iterationen.
	 */
	protected GraphInfo graph;		
	protected Grid grid; /**ueber das grid kommt die column mit grid.getLOcals an die locale schrittzahl ran.**/
	protected int me, localIterations;
	protected double epsilon;
	
	public Column(GraphInfo graph, Grid grid, int y) {

		/**aufgerufen von grid "echte" spalte.**/
		this.graph = graph;
		this.grid = grid;
		this.values = new Hashtable<>(graph.height, 1);
		this.akku = new Hashtable<>(graph.height, 1);
		this.me = y; //y ist die spaltennummer
		this.epsilon = graph.epsilon*graph.epsilon/graph.width;
	
		HashMap<Integer, Double> name = graph.column2row2initialValue.getOrDefault(y, new HashMap<>());
		Iterator<Entry<Integer,Double>> iter = name.entrySet().iterator();
			
		while(iter.hasNext()){
			Entry<Integer,Double> dummy = iter.next();
			int row = dummy.getKey();
			double val = dummy.getValue();
			values.put(row, val);			
			
		}
	}
	
	@Override
	abstract public Double call();
	
	abstract protected void exchange();
	
	abstract public void localIteration();
	
	abstract public double serialSigma();
	
	abstract public void computeNewValues();
	
	abstract public Hashtable<Integer, Double> getLeft();
	
	abstract public Hashtable<Integer, Double> getRight();
	
	abstract public void setLeft(Hashtable<Integer, Double> right);
	
	abstract public void setRight(Hashtable<Integer, Double> left);
	
	synchronized protected boolean addAccuToValuesAndLocalConvergence(Hashtable<Integer,Double> akku, Hashtable<Integer, Double> values){
		/**hier werden alle eintraege mit denen des akkus verechnet.**/
		double sigma = 0.0;
		Iterator<Entry<Integer, Double>> acc = akku.entrySet().iterator();

		while(acc.hasNext()){
			Entry<Integer, Double> dummy = acc.next();
			int pos = dummy.getKey();
			double val = dummy.getValue();
			sigma = sigma + val*val;
			addOrReplaceEntry(values, pos, values.getOrDefault(pos,0.0) + val);
		}//while schleife zu

		return sigma <= epsilon;
	}
	
	synchronized protected double getDelta(Hashtable<Integer, Double> leftAccu, Hashtable<Integer, Double> outLeft){
		/**		   
		 * Der Sinn dahinter ist: falls das Verhalten des Prozesses mit einer bestimmten lokalen Iterationszahl 
		 * zyklisch ist, dann ist das Verhältnis delta: previousDelta <1, weshalb die Schrittzahl geaendert werden muss.**/
		
		double delta = 0.0;
			for (int j = 0; j<graph.height; j++){
				double val = leftAccu.getOrDefault(j, 0.0) - outLeft.getOrDefault(j, 0.0);
				delta= val*val + delta;
			}
		return delta;
	}
	
	synchronized protected double getDelta(Hashtable<Integer, Double> leftAccu, Hashtable<Integer, Double> outLeft, 
												Hashtable<Integer, Double> rightAccu, Hashtable<Integer, Double> outRight){
		double delta = 0.0; 
			for(int j = 0; j<graph.height; j++){
				double val = (leftAccu.getOrDefault(j, 0.0) + rightAccu.getOrDefault(j, 0.0)) - (outLeft.getOrDefault(j,0.0) + outRight.getOrDefault(j, 0.0));
				delta = val*val + delta;
			}
		return delta;
	}
	
	protected double setAndComputeOutflow(Hashtable<Integer, Double> map, double val, int currentPos, double rate){
		double ret = 0.0;
		if(rate != 0.0 && val != 0.0){
			ret = val * rate;
			addOrReplaceEntry(map, currentPos, map.getOrDefault(currentPos,0.0)  + ret);	
		}
		return ret;
	}
	
	protected synchronized void addOrReplaceEntry(Hashtable<Integer, Double> map, int key, double val){
		
		/**aktualisiert oder ergaenzt eintraege in hashtables**/
		if(map.containsKey(key))
			map.replace(key, val);
		else if(val != 0)
			map.put(key, val);
	}
	
	synchronized public void setLocals(int n){
		localIterations = n;
	}
	
	public synchronized double getValue(int row){
		return values.getOrDefault(row, 0.0);
	}
	
	
	/*HILFSMETHODE ZUM TESTEN
	 * 
	 * public double getSum(){
		double ret= 0.0;
		Iterator<Entry<Integer,Double>> row = values.entrySet().iterator();
		while(row.hasNext())
			ret = ret + row.next().getValue();
		return ret;
	}*/
	
}
