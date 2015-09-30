
package implementation;

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
	private Hashtable<Integer, Column> columns;
	private GraphInfo graph;
	private int localIterations;
	private ExecutorService exe;
	/*
	 * In edges wird die Position der Dummys gespeichert. edges[0] -> links,
	 * edges[1] -> rechts
	 */
	private int[] edges = new int[2];
	private LeftBorder leftdummy;
	private RightBorder rightdummy;

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
		this.columns = new Hashtable<>();
		this.graph = graph;

		int firstColumn = 0;
		Column column = null;
		for (Entry<Integer, HashMap<Integer, Double>> entry : graph.column2row2initialValue.entrySet()) {
			for (Entry<Integer, Double> value : entry.getValue().entrySet()) {
				if (value.getValue() > 0.0) {
					firstColumn = entry.getKey();
					if (firstColumn == graph.width - 1) {
						// init ist RightBorder.
						column = new RightBorder(graph, firstColumn, null, localIterations);
						leftdummy = new LeftBorder(graph, firstColumn - 1, null, localIterations);
						rightdummy = null;
					} else if (firstColumn == 0) {
						// init ist LeftBorder.
						column = new LeftBorder(graph, firstColumn, null, localIterations);
						rightdummy = new RightBorder(graph, firstColumn + 1, null, localIterations);
						leftdummy = null;
					} else {
						// init ist Middle
						column = new Middle(graph, firstColumn, null, null, localIterations);
						this.leftdummy = new LeftBorder(graph, firstColumn - 1, null, localIterations);
						this.rightdummy = new RightBorder(graph, firstColumn + 1, null, localIterations);
					}
					columns.put(firstColumn, column);
					break;
				}
			}
			if (column != null)
				break;
		}
		edges[0] = firstColumn - 1;
		edges[1] = firstColumn + 1;
		this.extendByDummies();
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
		this.columns = new Hashtable<>();
		this.exe = exe;
		this.graph = graph;

		int firstColumn = 0;
		Column column = null;
		for (Entry<Integer, HashMap<Integer, Double>> entry : graph.column2row2initialValue.entrySet()) {
			for (Entry<Integer, Double> value : entry.getValue().entrySet()) {
				if (value.getValue() > 0.0) {
					firstColumn = entry.getKey();
					if (firstColumn == graph.width - 1) {
						// init ist RightBorder.
						Exchanger<double[]> ex = new Exchanger<>();
						column = new RightBorder(graph, firstColumn, ex, localIterations);
						leftdummy = new LeftBorder(graph, firstColumn - 1, ex, localIterations);
						rightdummy = null;
					} else if (firstColumn == 0) {
						// init ist LeftBorder.
						Exchanger<double[]> ex = new Exchanger<>();
						column = new LeftBorder(graph, firstColumn, ex, localIterations);
						rightdummy = new RightBorder(graph, firstColumn + 1, ex, localIterations);
						leftdummy = null;
					} else {
						// init ist Middle
						Exchanger<double[]> leftEx, rightEx;
						leftEx = new Exchanger<>();
						rightEx = new Exchanger<>();
						column = new Middle(graph, firstColumn, rightEx, leftEx, localIterations);
						this.leftdummy = new LeftBorder(graph, firstColumn - 1, rightEx, localIterations);
						this.rightdummy = new RightBorder(graph, firstColumn + 1, leftEx, localIterations);
					}
					columns.put(firstColumn, column);
					break;
				}
			}
			if (column != null)
				break;
		}
		edges[0] = firstColumn - 1;
		edges[1] = firstColumn + 1;
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
		boolean returnvalue = true;
		/*
		 * Nun sollen die lokalen Iterationen auf allen Spalten nebenläufig
		 * berechnet werden, dazu werden die Spalten, sowie die Dummies dem
		 * ExecutorService übergeben, welcher dies dann erledigt. invokeAll
		 * kehrt erst dann zurück, wenn alle Tasks erledigt sind.
		 */
		try {
			Future<Boolean> left = null, right = null;
			if (leftdummy != null)
				left = exe.submit(leftdummy);
			if (rightdummy != null)
				right = exe.submit(rightdummy);

			List<Future<Boolean>> returns = exe.invokeAll(columns.values());
			for (Future<Boolean> col : returns)
				returnvalue &= col.get();

			/*
			 * Falls nötig werden noch neue Dummies angelegt.
			 */
			if (leftdummy != null) {
				returnvalue &= left.get();
				if (leftdummy.hasValue()) {
					if (edges[0] == 0) {
						columns.put(edges[0], leftdummy);
						leftdummy = null;
					} else {
						Exchanger<double[]> newex = new Exchanger<>();
						Middle newOne = new Middle(graph, edges[0], newex, leftdummy.getEx(), localIterations);
						newOne.setValues(leftdummy.getValues());
						columns.put(edges[0], newOne);
						edges[0] = edges[0] - 1;
						leftdummy = new LeftBorder(graph, edges[0], newex, localIterations);
					}
				}
			}

			if (rightdummy != null) {
				returnvalue &= right.get();
				if (rightdummy.hasValue()) {
					if (edges[1] == graph.width - 1) {
						columns.put(edges[1], rightdummy);
						rightdummy = null;
					} else {
						Exchanger<double[]> newex = new Exchanger<>();
						Middle newOne = new Middle(graph, edges[1], newex, rightdummy.getEx(), localIterations);
						newOne.setValues(rightdummy.getValues());
						columns.put(edges[1], newOne);
						edges[1] = edges[1] + 1;
						rightdummy = new RightBorder(graph, edges[1], newex, localIterations);
					}
				}
			}

		} catch (Exception e) {
			System.out.println(":/");
			return true;
		}
		/*
		 * Es wird zurückgegeben, ob schon das Konvergenzkriterium erfüllt wird.
		 */
		return returnvalue;
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
		for (int i = edges[0]; i <= edges[1]; i++)
			columns.get(i).localIteration();
		/*
		 * tausche die den horizontalen Flow der Spalten aus:
		 */
		for (int i = edges[0]; i < edges[1]; i++)
			exchange(i);

		/*
		 * Zur Prüfung auf globale Konvergenz werden in sigma die Summen der
		 * Quadrate der Knotendifferenzen der Spalen (serialSigma()) putiert.
		 */
		double sigma = 0.0;
		for (int i = edges[0]; i <= edges[1]; i++)
			sigma += columns.get(i).serialSigma();
		/*
		 * Jetzt werden die horizontalen Inflows auf die Kontenwerte putiert.
		 */
		for (int i = edges[0]; i <= edges[1]; i++)
			columns.get(i).computeNewValues();

		if (edges[0] > 0 && columns.get(edges[0]).hasValue()) {
			edges[0] = edges[0] - 1;
			columns.put(edges[0], new Middle(graph, edges[0], null, null, localIterations));
		}

		if (edges[1] < graph.width - 1 && columns.get(edges[1]).hasValue()) {
			edges[1] = edges[1] + 1;
			columns.put(edges[1], new Middle(graph, edges[1], null, null, localIterations));
		}
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
		double[] dummyRight = left.getRight();
		left.setRight(right.getLeft());
		right.setLeft(dummyRight);
	}

	@Override
	public synchronized double getValueAt(int column, int row) {
		return columns.getOrDefault(column, new Middle(graph, column, null, null, localIterations)).getValue(row);
	}

	/**
	 * Dient dem Einfügen der Dummies in die Hashtable der Spalten, beim Umstieg
	 * vom Nebenläufigen zum Sequentiellen.
	 */
	public synchronized void extendByDummies() {
		if (leftdummy != null)
			columns.put(edges[0], new Middle(graph, edges[0], null, null, localIterations));
		if (rightdummy != null)
			columns.put(edges[1], new Middle(graph, edges[1], null, null, localIterations));
	}

	/**
	 * Setzt die Anzahl der lokalen Iterationen.
	 * 
	 * @param n
	 *            Anzahl der lokalen Iterationen
	 */
	public synchronized void setLocals(int n) {
		localIterations = n;
		columns.values().forEach(column -> column.setLocals(n));
	}
}
