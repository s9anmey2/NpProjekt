
package implementation;

import java.util.List;
import java.util.Collection;
//import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Exchanger;
//import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import np2015.GraphInfo;
import np2015.ImageConvertible;

public class Grid implements ImageConvertible {
	//public Lab lab;
	private Hashtable<Integer, Column> columns;			// Kein Zugriff von außen
	private GraphInfo graph;
	private ExecutorService exe;
	
	public Grid(GraphInfo graph){ //2. Konstruktor fuer sequentielle Loesung
		this.graph = graph;
		
		/**der konstruktor baut die spalten.**/		
		makeColumns();
	}
	
	public Grid(GraphInfo graph, ExecutorService exe){//Konstruktor fuer die nebenlauefige Loesung

		//this.lab = new Lab();
		this.exe = exe;
		this.graph = graph;
		this.columns = new Hashtable<Integer, Column>(graph.width, 1);		
		/**der konstruktor baut die spalten.**/
		makeColumns();
	}
	
	public synchronized boolean globalIteration() {
		boolean converged = true;

		/**hier jetzt den executor hin**/
		try{

			Collection<Column> tasks = columns.values();
			List<Future<Boolean>> rets =  exe.invokeAll(tasks);
			
			for(Future<Boolean> col: rets)
				converged = converged && col.get();
			
		}catch (Exception e){
			System.out.println(":/");
			return true;
		}		
		return converged;
	}

	private synchronized void exchange(int i){//benutzt nur der sequentielle Teil
		Column left = columns.get(i);
		Column right = columns.get(i+1);
		Hashtable<Integer, Double> dummyRight = left.getRight();	
		left.setRight(right.getLeft());
		right.setLeft(dummyRight);
	}

	private void makeColumns(){
		Exchanger<Hashtable<Integer,Double>> left, right;
		
		right = new Exchanger<Hashtable<Integer,Double>>();
		columns.put(0, new LeftBorder(graph, this, 0, right));
		
		for(int i= 1; i<graph.width-1; i++){
			left = right;
			right = new Exchanger<Hashtable<Integer,Double>>();
			columns.put(i, new Middle(graph, this, i, left, right));
		}//for Schleife	
		left = right;
		
		columns.put(graph.width-1, new RightBorder(graph, this, graph.width-1, left));
	}

	public synchronized Column getColumn(int i) {
		return columns.get(i);
	}
	
	public synchronized void setLocals(int n){
		Collection<Column> set= columns.values();
		for(Column col: set)
			col.setLocals(n);

	}
	
	@Override
	public double getValueAt(int column, int row) {
		return (columns.containsKey(column)) ? columns.get(column).getValue(row): 0.0;
	}
	
	public boolean serialComputation(){

		/**fuer den sequentiellen Teil der loesung**/
		double eps = graph.epsilon*graph.epsilon;

		Iterator<Entry<Integer, Column>> spalten = columns.entrySet().iterator(); //berechnet vertikalen flow mit lokalen Iterationschritten = 1
		while(spalten.hasNext())
			spalten.next().getValue().localIteration();

		
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
	
	/*HILSMETHODE ZUM TESTEN
	 * public double getSum(){
		Iterator<Entry<Integer, Column>> col = columns.entrySet().iterator();
		double ret = 0.0;
		while(col.hasNext())
			ret = ret + col.next().getValue().getSum();
		return ret;
	}*/
}
