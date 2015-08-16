
package implementation;

import java.util.List;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import np2015.GraphInfo;
import np2015.ImageConvertible;

/**
 * Das Grid stellt das Gitter bestehend aus Columns dar. Es stellt Methoden zur
 * nebenläufigen und sequentiellen Berechnung eines Osmoseprozesses auf dem
 * Gitter zur Verfügung.
 * Grid ist ein Monitor.
 */
public class Grid implements ImageConvertible {
	// TODO entfernen: public Lab lab;
	private Hashtable<Integer, Column> columns;
	private GraphInfo graph;
	private ExecutorService exe;

	/**
	 * Der Konstruktor erzeugt ein Grid Objekt, welches sich aus dem
	 * mitgegebenen GraphInfo Objekt den Initialen Wert ausliest. Dieser
	 * Konstruktor ist für die sequentielle Lösung gedacht. Soll nebenläufig
	 * gearbeitet werden, so muss zusätzlich ein EcecutorService übergeben
	 * werden.
	 * 
	 * @param graph
	 */
	private double rem;
	private int counter;
	public Grid(GraphInfo graph){ //2. Konstruktor fuer sequentielle Loesung
		this.graph = graph;
		makeColumns();
	}

	/**
	 * Der Konstruktor erzeugt ein Grid Objekt, welches sich aus dem
	 * mitgegebenen GraphInfo Objekt den Initialen Wert ausliest. Dieser
	 * Konstruktor ist für die nebenläufige Lösung gedacht. Soll sequentiell
	 * gearbeitet werden, so kann auf den EcecutorService verzichtet werden.
	 * 
	 * @param graph
	 * @param exe
	 */
	public Grid(GraphInfo graph, ExecutorService exe) {
		// TODO entfernen: this.lab = new Lab();
		this.exe = exe;
		this.graph = graph;
		this.columns = new Hashtable<Integer, Column>(graph.width, 1);
		makeColumns();
	}

	/**
	 * Die Methode globalIteration führt nebenläufig für allen Spalten eine,
	 * zuvor durch setLokals gesetzte, Anzahl von lokalen Iterationen durch.
	 * 
	 * @return TODO beschreiben
	 */
	public synchronized boolean globalIteration() {
		double dummy = 0.0;
		try {
			Collection<Column> tasks = columns.values();
			List<Future<Double>> rets =  exe.invokeAll(tasks);
			for(Future<Double> col: rets)
				 dummy = col.get() + dummy;
			
		}catch (Exception e){
			System.out.println(":/");
			return true;
		}		
		if(counter++ >= 10000){
			counter = 0;
			if(dummy < rem)
				rem = dummy;
			else
				return true;
		}
		
		boolean ret =dummy<(graph.epsilon*graph.epsilon);
		//System.out.println(ret);
		return ret;
	}

	/**
	 * serialComputation führt einen lokalen Iterationsschritt auf jeder Column
	 * aus prüft die globale Konvergenz. Zuvor muss setLocals(1) aufgerufen
	 * worden sein.
	 * 
	 * @return true falls die globale Konvergenz erreicht ist.
	 */
	public synchronized boolean serialComputation() {
		double eps = graph.epsilon * graph.epsilon;

		/*
		 * führe auf allen Columns eine lokale Iteration aus:
		 */
		Iterator<Entry<Integer, Column>> spalten = columns.entrySet().iterator();
		while (spalten.hasNext())
			spalten.next().getValue().localIteration();

		/*
		 * tausche die den horizontalen Flow der Spalten aus:
		 */
		for (int i = 0; i < graph.width - 1; i++) {
			if (columns.containsKey(i) && columns.containsKey(i + 1))
				exchange(i);
		}

		/*
		 * Zur Prüfung auf globale Konvergenz werden in sigma die Summen der
		 * Quadtate der Knotendifferenzen (serialSigma()) addiert
		 */
		spalten = columns.entrySet().iterator();
		double sigma = 0.0;
		while (spalten.hasNext())
			sigma = sigma + spalten.next().getValue().serialSigma();

		/*
		 * Jetzt werden die horizontalen Inflows auf die Kontenwerte addiert.
		 */
		spalten = columns.entrySet().iterator();
		while (spalten.hasNext())
			spalten.next().getValue().computeNewValues();

		/*
		 * Rückgabe ist das globale Konvergenzkriterium in der Form: Summe der
		 * Quadtate der Knotendifferenzen < Quadtat von Epsilon
		 */
		return sigma < (eps);
	}

	/**
	 * Erzeugt alle Spalten mit entsprechenden Exchangern.
	 */
	private synchronized void makeColumns() {
		Exchanger<Hashtable<Integer, Double>> leftEx, rightEx;

		rightEx = new Exchanger<Hashtable<Integer, Double>>();
		columns.put(0, new LeftBorder(graph, this, 0, rightEx));

		for (int i = 1; i < graph.width - 1; i++) {
			leftEx = rightEx;
			rightEx = new Exchanger<Hashtable<Integer, Double>>();
			columns.put(i, new Middle(graph, this, i, leftEx, rightEx));
		}
		leftEx = rightEx;

		columns.put(graph.width - 1, new RightBorder(graph, this, graph.width - 1, leftEx));
	}

	/**
	 * Setzt die Anzahl der lokalen Iterationen.
	 * 
	 * @param n
	 */
	public synchronized void setLocals(int n) {
		Collection<Column> set = columns.values();
		set.stream().parallel().forEach(column -> column.setLocals(n));

	}

	@Override
	public synchronized double getValueAt(int column, int row) {
		return (columns.containsKey(column)) ? columns.get(column).getValue(row) : 0.0;
	}

	/**
	 * Taucht den Outflow im falle der sequentiellen Ausführung.
	 * 
	 * @param i
	 */
	private synchronized void exchange(int i) {// benutzt nur der sequentielle
												// Teil
		Column left = columns.get(i);
		Column right = columns.get(i + 1);
		Hashtable<Integer, Double> dummyRight = left.getRight();
		left.setRight(right.getLeft());
		right.setLeft(dummyRight);
	}
}
