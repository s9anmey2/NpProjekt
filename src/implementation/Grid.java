
package implementation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import np2015.GraphInfo;
import np2015.ImageConvertible;

/**
 * Das Grid stellt das Gitter bestehend aus Columns dar. Es stellt Methoden zur
 * nebenläufigen und sequentiellen Berechnung eines Osmoseprozesses auf dem
 * Gitter zur Verfügung. Grid ist ein Monitor.
 */
public class Grid implements ImageConvertible {
	private Hashtable<Integer,Column> columns;
	private GraphInfo graph;
	private ExecutorService exe;
	private int[] edges = new int[2];//0 -> links, 1-> rechts
	private LeftBorder leftdummy;
	private RightBorder rightdummy;
	/*
	 * Um Terminierung sicher zu stellen wird sich in rem ein Vergleichswert
	 * gemerkt, der regelmäßig (mit counter) auf Verbesserung geprüft wird.
	 */
	private double rem;
	private int counter;
	/**
	 * Der Konstruktor erzeugt ein Grid Objekt, welches sich aus dem
	 * mitgegebenen GraphInfo Objekt den Initialen Wert ausliest. Dieser
	 * Konstruktor ist für die sequentielle Lösung gedacht. Soll nebenläufig
	 * gearbeitet werden, so muss zusätzlich ein ExecutorService übergeben
	 * werden.
	 * 
	 * @param graph
	 *            GraphInfo Objekt, welches die initialen Werte und Raten zur
	 *            Verfügung stellt.
	 */
	public Grid(GraphInfo graph) {
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
	 *            GraphInfo Objekt, welches die initialen Werte und Raten zur
	 *            Verfügung stellt.
	 * @param exe
	 *            Der ExecuterService wird in globalIteration benutzt und muss
	 *            vom Aufrufer beendet werden, wenn er nicht mehr benötigt wird.
	 */
	public Grid(GraphInfo graph, ExecutorService exe) {
		int firstColumn = 0;
		Column column = null;
		this.columns = new Hashtable<>();
		this.exe = exe;
		this.graph = graph;	
		
		for(Entry<Integer,HashMap<Integer,Double>> entry : graph.column2row2initialValue.entrySet())
		{
			for(Entry<Integer,Double> value : entry.getValue().entrySet())
			{
				if(value.getValue() > 0.0)
				{
					firstColumn = entry.getKey();
					if(firstColumn == graph.width-1)
					{
					//init ist RightBorder, linkerdummy ist middle.
						Exchanger<Hashtable<Integer, Double>> ex = new Exchanger<>();
						column = new RightBorder(graph,firstColumn,ex);
						leftdummy = new LeftBorder(graph, firstColumn-1,ex);
						rightdummy = null;
					}
					else if(firstColumn == 0)
					{
					//init ist LeftBorder, rechterdummy ist middle
						Exchanger<Hashtable<Integer, Double>> ex = new Exchanger<>();
						column = new LeftBorder(graph,firstColumn,ex);
						rightdummy = new RightBorder(graph, firstColumn+1,ex);
						leftdummy = null;
					}
					else
					{
						//erste column ist keine randspalte. d.h 2 dummies und eine mittelspalte.
						Exchanger<Hashtable<Integer, Double>> leftEx, rightEx;
						leftEx = new Exchanger<>();
						rightEx = new Exchanger<>();
						column = new Middle(graph,firstColumn,rightEx,leftEx); //die exchanger sind aus der perspektive der Raender gesetzt. 
						this.leftdummy = new LeftBorder(graph,firstColumn-1,rightEx);
						this.rightdummy = new RightBorder(graph,firstColumn+1,leftEx);
					}
					columns.put(firstColumn,column);
					break;
				}
			}
			if(column != null)
				break;
		}
		edges[0] = firstColumn -1;
		edges[1] = firstColumn +1;
		rem = 1;
	}

	/**
	 * Erzeugt alle Spalten mit entsprechenden Exchangern.
	 */
	private synchronized void makeColumns() {
		
		//Loeschen wenn alles fertig ist.
		Exchanger<Hashtable<Integer, Double>> leftEx, rightEx;
		rightEx = new Exchanger<Hashtable<Integer, Double>>();
		columns.put(0, new LeftBorder(graph, 0, rightEx));
		for (int i = 1; i < graph.width - 1; i++) {
			leftEx = rightEx;
			rightEx = new Exchanger<Hashtable<Integer, Double>>();
			columns.put(i, new Middle(graph, i, leftEx, rightEx));
		}
		leftEx = rightEx;
		columns.put(graph.width - 1, new RightBorder(graph, graph.width - 1, leftEx));
		
		
	}

	/**
	 * Die Methode globalIteration führt nebenläufig für allen Spalten eine,
	 * zuvor durch setLokals gesetzte, Anzahl von lokalen Iterationen durch.
	 * 
	 * @return Gibt true zurück, wenn Inflow ~ Outflow gilt oder über die
	 *         Aufrufe dieser Methode hinweg keine Verbesserung (im Sinne von
	 *         Annäherung an das Konvergenzkriterium) erziehlt wird.
	 */
	public synchronized boolean globalIteration() {
		/*
		 * sum enthält die Summe der Knotendifferenzen bezüglich den
		 * horizontalen Flows.
		 */
		double sum = 0.0;
		/*
		 * Nun sollen die lokalen Iterationen auf allen Spalten nebenläufig
		 * berechnet werden, dazu werden die Spalten dem ExecutorService
		 * übergeben, welcher dies dann erledigt. invokeAll kehrt erst dann
		 * zurück, wenn alle Tasks erledigt sind.
		 */
		try {
			Future<Double> left = null, right = null;
			if(leftdummy != null)
				left = exe.submit(leftdummy);
			if(rightdummy != null)
				right = exe.submit(rightdummy);
			
			List<Future<Double>> rets = exe.invokeAll(columns.values());
			for (Future<Double> col : rets)
				sum = col.get() + sum;

			if(leftdummy != null){
				left.get();
				if(leftdummy.hasValue()){
					if(edges[0] == 0){
						columns.put(edges[0], leftdummy);
						leftdummy = null;
					}else{
						Exchanger<Hashtable<Integer,Double>> newex = new Exchanger<>();
						Middle newOne = new Middle(graph,edges[0],newex,leftdummy.getEx());
						newOne.setValues(leftdummy.getValues());
						columns.put(edges[0],newOne);
						edges[0]  = edges[0] -1;
						leftdummy = new LeftBorder(graph, edges[0],newex);
					}
				}
			}
			
			if(rightdummy != null){
				right.get();
				if(rightdummy.hasValue()){
					if(edges[1] == graph.width-1){
						columns.put(edges[1],rightdummy);
						rightdummy = null;
					}else{
						Exchanger<Hashtable<Integer,Double>> newex = new Exchanger<>();
						Middle newOne = new Middle(graph,edges[1],newex,rightdummy.getEx());
						newOne.setValues(rightdummy.getValues());
						columns.put(edges[1],newOne);
						edges[1]  = edges[1] +1;
						rightdummy = new RightBorder(graph, edges[1],newex);
					}
				}
			}
			
			
			
		} catch (Exception e) {
			System.out.println(":/");
			return true;
		}
		/*
		 * Alle 10000 Aufrufe dieser Methode wird auf Annäherung an das
		 * Konvergenzkriterium geprüft. Ist keine Verbesserung feststellbar,
		 * wird true zurückgegeben.
		 */
		if (counter++ >= 10000) {
			counter = 0;
			if (sum < rem)
				rem = sum;
			else
				return true;
		}
		/*
		 * Im Normalfall wird zurückgegeben, ob die Wertdifferenzen bezüglich
		 * des horizontalen Flows schon das Konvergenzkriterium erfüllen.
		 */
		return sum < (graph.epsilon * graph.epsilon);
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
		columns.values().forEach(column -> column.localIteration());
		/*
		 * tausche die den horizontalen Flow der Spalten aus:
		 */
		for (int i = edges[0]; i < edges[1]; i++) 
			exchange(i);
		if(edges[0] > 0 && columns.get(edges[0]).hasValue())
		{
			edges[0] = edges[0]-1;
			columns.put(edges[0],new Middle(graph, edges[0],null,null));
		}
		
		if(edges[1] < graph.width -1 && columns.get(edges[1]).hasValue())
		{
			edges[1] = edges[1]+1;
			columns.put(edges[1],new Middle(graph, edges[1], null,null));
		}
		/*
		 * Zur Prüfung auf globale Konvergenz werden in sigma die Summen der
		 * Quadrate der Knotendifferenzen der Spalen (serialSigma()) putiert.
		 */
		double sigma = 0.0;
		for (Column c : columns.values())
			sigma = sigma + c.serialSigma();
		/*
		 * Jetzt werden die horizontalen Inflows auf die Kontenwerte putiert.
		 */
		columns.values().forEach(column -> column.computeNewValues());
		/*
		 * Rückgabe ist das globale Konvergenzkriterium in der Form: Summe der
		 * Quadtate der Knotendifferenzen < Quadtat von Epsilon
		 */
		return sigma < eps;
	}

	/**
	 * Taucht den Outflow im Falle der sequentiellen Ausführung.
	 * 
	 * @param i
	 *            Index der linken Spalte
	 */
	private synchronized void exchange(int i) {
		Column left = columns.get(i);
		Column right = columns.get(i + 1);
		Hashtable<Integer, Double> dummyRight = left.getRight();
		left.setRight(right.getLeft());
		right.setLeft(dummyRight);
	}

	@Override
	public synchronized double getValueAt(int column, int row) {
		return columns.get(column).getValue(row);
	}
	
	public synchronized void extendByDummies()
	{
		if(leftdummy != null)
			columns.put(edges[0],new Middle(graph,edges[0],null,null));
		if(rightdummy != null)
			columns.put(edges[1],new Middle(graph,edges[1],null,null));
	}

	/**
	 * Setzt die Anzahl der lokalen Iterationen.
	 * 
	 * @param n
	 *            Anzahl der lokalen Iterationen
	 */
	public synchronized void setLocals(int n) {
		columns.values().forEach(column -> column.setLocals(n));
		// Collection<Column> set = columns.values();
		// set.stream().parallel().forEach(column -> column.setLocals(n));
	}
}
