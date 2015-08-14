package implementation;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import np2015.GraphInfo;

/**
 * Die Klasse Column stellt eine Spalte eines Gitters dar und enthält Methoden,
 * welche die Berechnungen ausführen, die nur eine Spalte betreffen.
 */
abstract public class Column implements Callable<Boolean>{
	
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
	protected int me;
	protected final double epsilonSchlange;
	private double previousDelta = 0.0;
	
	public Column(GraphInfo graph, Grid grid, int y) {

		/**aufgerufen von grid "echte" spalte.**/
		this.graph = graph;
		this.grid = grid;
		this.values = new Hashtable<>();

		this.akku = new Hashtable<>();
		this.me = y; //y ist die spaltennummer
		this.epsilonSchlange = (graph.epsilon*graph.epsilon)/graph.width;
		
	
		HashMap<Integer, Double> name = graph.column2row2initialValue.getOrDefault(y, new HashMap<>());
		Iterator<Entry<Integer,Double>> iter = name.entrySet().iterator();
			
		while(iter.hasNext()){
			Entry<Integer,Double> dummy = iter.next();
			int row = dummy.getKey();
			double val = dummy.getValue();
			values.put(row, val);			
			
		}
	}
/**-------------------------KONSTRUKTOR ENDE--------------------------------------------------------------------------------------------------------------
 * 
 * 
 * 
 * 
 * 
 * 
 * 																																							**/
	
	@Override
	abstract public Boolean call();
	
	abstract protected void exchange();
	
	abstract public void localIteration();
	
	abstract public double serialSigma();
	
	abstract public void computeNewValues();
	
	abstract public Hashtable<Integer, Double> getLeft();
	
	abstract public Hashtable<Integer, Double> getRight();
	
	abstract public void setLeft(Hashtable<Integer, Double> right);
	
	abstract public void setRight(Hashtable<Integer, Double> left);
	
	synchronized protected boolean getDelta(Hashtable<Integer, Double> leftAccu, Hashtable<Integer, Double> outLeft){
		double delta = 0.0;
					
			for (int j = 0; j<graph.height; j++){
				double val = leftAccu.getOrDefault(j, 0.0) - outLeft.getOrDefault(j, 0.0);
				delta= val*val + delta;
			}
		if(delta != 0.0)
			System.out.println("Delta: " + delta + " Ratio: " + previousDelta/delta + " invoke by " + me);
		previousDelta = delta;
		return  delta<=epsilonSchlange;
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
