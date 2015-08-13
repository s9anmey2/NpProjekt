
package implementation;

import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import np2015.GraphInfo;
import np2015.ImageConvertible;

public class Grid implements ImageConvertible {

	private Hashtable<Integer, Column> columns;			// Kein Zugriff von außen
	private GraphInfo graph;
	private ExecutorService exe;
	private volatile int localIterations; /**Der supervisor gibt dem grid die anzahl der localen schritte, damit sich die co,lumns diese dort abholen koennen.
	Dazu gibt das Grid dann eine referenz auf sich selbst an die columns mit**/
	
	/**wird mit dem aktuellen GraphInfo Objekt initialisiert und reicht dieses nach unten an die Columns weiter.**/
	
	public Grid(GraphInfo graph){
		this.graph = graph;
		this.columns = new Hashtable<Integer, Column>();
		this.exe = Executors.newFixedThreadPool(graph.width);//new ThreadPoolExecutor(graph.width)
		
		/**der konstruktor baut die spalten.**/
		Iterator<Entry<Integer,HashMap<Integer,Double>>> iter  = graph.column2row2initialValue.entrySet().iterator();
		
		while(iter.hasNext()){
			Entry<Integer, HashMap<Integer, Double>> forGrid = iter.next();
			HashMap<Integer,Double> spalte =forGrid.getValue();
			Column column = new Column(graph, this, forGrid.getKey(), false);		
			columns.put(forGrid.getKey(), column);
		}//aeußere Schleife
		
		
	}
	
	public boolean serialComputation(){
		
		/**fuer eine rein sequentielle loesung**/
		double eps = graph.epsilon*graph.epsilon;

		Iterator<Entry<Integer, Column>> spalten = columns.entrySet().iterator(); //berechnet vertikalen flow mit lokalen Iterationschritten = 1
		while(spalten.hasNext())
			spalten.next().getValue().call();
		
		addDummyColumns(); //bereitet alles fuer den exchange vor
		
		for(int i = 0; i < graph.width-1; i++){ //exchange: outflow -> inflow
			if(columns.containsKey(i) && columns.containsKey(i+1))							
				exchange(i);
		}//for schleife zu
		
		spalten = columns.entrySet().iterator();
		double sigma = 0.0;
		while(spalten.hasNext()) //berechnet delta(inflow, outflow) jeder Spalte und bildet die Summe der quadrate.
			sigma = sigma + spalten.next().getValue().serialSigma();		
		
		spalten = columns.entrySet().iterator(); //im nebenlaeufigen macht das nodeeval! Hier wird einfach der horizontale flow verrechnet.
		while(spalten.hasNext())
			spalten.next().getValue().computeNewValues();

		return sigma < (eps);
	}
	
	public synchronized boolean globalIteration() {
		boolean converged = true;

		/**hier jetzt den executor hin**/
		try{
			Collection<Column> tasks = columns.values();
			List<Future<Double>> rets =  exe.invokeAll(tasks);
			for(Future<Double> col: rets)
				col.get();
		}catch (Exception e){
			System.out.println(":/");
			return true;
		}
		
		
		/**passe die gridstruktur fuer die arbeit des exchangers an: fuege dummy spalten hinzu**/
		addDummyColumns();
		
		/**tausche rechten outflow gegen linken outflow zweier benachbarter columns**/
		for(int i = 0; i < graph.width-1; i++){
			if(columns.containsKey(i) && columns.containsKey(i+1)){
				
				/**berechnet die outflow/inflow differenz zweier benachbarter spalten.**/
				double epsilon = 0;
				for (int j = 0; j<graph.height; j++){
					double val = columns.get(i).getRight().getOrDefault(j, 0.0) - columns.get(i+1).getLeft().getOrDefault(j, 0.0);
					epsilon = val*val + epsilon;
				}
				
				converged = (epsilon <= (graph.epsilon/(graph.width-1))) && converged;
				
				exchange(i);			
			}
		}//for schleife zu
		
		/**neue values der columns berechnen mit rekursiver Methode columnValueComputation(iterator)**/
		Iterator<Entry<Integer, Column>> columnIter2 = columns.entrySet().iterator();
		columnValueComputation(columnIter2);

		
		return converged;
	}
	
	public double getSum(){
		Iterator<Entry<Integer, Column>> col = columns.entrySet().iterator();
		double ret = 0.0;
		while(col.hasNext())
			ret = ret + col.next().getValue().getSum();
		return ret;
	}

	private synchronized void exchange(int i){
		Column left = columns.get(i);
		Column right = columns.get(i+1);
		Hashtable<Integer, Double> dummyRight = left.getRight();	
		left.setRight(right.getLeft());
		right.setLeft(dummyRight);
	}

	private void columnValueComputation(Iterator<Entry<Integer,Column>> iter) {
		
		/**benutzt den Wrapper nodeeval um nebenläufig die neuen column eintraege zu berechnen.**/
		if(iter.hasNext()){
			NodeEval eval = new NodeEval(iter.next().getValue());
			eval.start();
			columnValueComputation(iter);
			try {
				eval.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("columnValueComputation kaputt.");
			}
		}
	}

	public synchronized void addDummyColumns() {
		
		/**iteriert ueber das grid und erganzt rechts und links von existierenden columns leere dummy columns. Dazu merkt es sich die erst die keys 
		 in einer ersten iteration und fuegt dann ein in einer zweiten.**/
		TreeSet<Integer> toMake = new TreeSet<>();
		Iterator<Entry<Integer, Column>> iter = columns.entrySet().iterator();
		while(iter.hasNext()){
			Entry<Integer, Column> dummy = iter.next();
			int key = dummy.getKey();
			
			if(!(columns.containsKey(key+1)) && key<graph.width-1)
				toMake.add(key +1);
			if(!(columns.containsKey(key-1)) && key>0)
				toMake.add(key -1);		
		}//Ende while schleife
		
		/**ergaenzen der dummies**/
		Iterator<Integer> set = toMake.iterator();
		while(set.hasNext()){
			int c = set.next().intValue();
			columns.put(c, new Column(graph, this, c, true));
		}
		
	}

	public synchronized Column getColumn(int i) {
		return columns.get(i);
	}
	
	@Override
	public double getValueAt(int column, int row) {
		return (columns.containsKey(column)) ? columns.get(column).getValue(row): 0.0;
	}
	
	public synchronized void setLocals(int i){
		localIterations = i;
	}
	
	public int getLocals(){
		return localIterations;
	}
}
