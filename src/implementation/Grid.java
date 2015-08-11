/**
 * 
 */
package implementation;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeSet;

import np2015.GraphInfo;
import np2015.ImageConvertible;

public class Grid implements ImageConvertible {

	private Hashtable<Integer, Column> columns;			// Kein Zugriff von außen
	private GraphInfo graph;
	private volatile int localIterations; /**Der supervisor gibt dem grid die anzahl der localen schritte, damit sich die co,lumns diese dort abholen koennen.
	Dazu gibt das Grid dann eine referenz auf sich selbst an die columns mit**/
	
	/**wird mit dem aktuellen GraphInfo Objekt initialisiert und reicht dieses nach unten an die Columns weiter.**/
	
	public Grid(GraphInfo graph){
		this.graph = graph;
	}
	
	public Grid() {
		// TODO Auto-generated constructor stub
	}
	
	public synchronized void globalIteration() {
	
		/**global iteration legt alle columns an, baut einen Iterator und schmeisst den dann in globalIteration(iteration iter)**/
		Iterator<Entry<Integer, Column>> columnIter = columns.entrySet().iterator();
		globalIteration(columnIter);
		
		/**passe die gridstruktur fuer die arbeit des exchangers an: fuege dummy spalten hinzu**/
		addDummyColumns();
		
		/**tausche rechten outflow gegen linken outflow zweier benachbarter columns**/
		for(int i = 0; i < graph.width-1; i++){
			if(columns.contains(i) && columns.contains(i+1)){
				Column left = columns.get(i);
				Column right = columns.get(i+1);
				Hashtable<Integer, Double> dummy = left.getRight();				
				left.setRight(right.getLeft());
				right.setLeft(dummy);				
			}
		}//for schleife zu
		
		/**neue values der columns berechnen mit rekursiver Methode columnValueComputation(iterator)**/
		Iterator<Entry<Integer, Column>> columnIter2 = columns.entrySet().iterator();
		columnValueComputation(columnIter2);
	}
	
	private void globalIteration(Iterator<Entry<Integer, Column>> iter) {
		
		/**das wird eine rekursive Methode, die startet solange threads wies welche gibt und wartet auf deren terminieren.**/
		if(iter.hasNext()){
			Column current = iter.next().getValue();
			current.start();
			globalIteration(iter);
			try {
				current.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("globaliteration(iterator) kaputt");
			}
		}
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
			
			if(!(columns.containsKey(key+1)) && key<graph.width)
				toMake.add(key +1);
			if(!(columns.containsKey(key-1)) && key>0)
				toMake.add(key -1);		
		}//Ende while schleife
		
		/**ergaenzen der dummies**/
		Iterator<Integer> set = toMake.iterator();
		while(set.hasNext()){
			int c = set.next().intValue();
			columns.put(c, new Column(graph, this, c));
		}
		
	}

	public synchronized Column getColumn(int i) {
		return columns.get(i);
	}
	
	@Override
	public double getValueAt(int column, int row) {
		return columns.getOrDefault(column, new Column(graph, this, column)).getValue(row);
	}
	
	public synchronized void setLocals(int i){
		localIterations = i;
	}
	
	public int getLocals(){
		return localIterations;
	}
}
