package implementation;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.concurrent.Exchanger;

import np2015.GraphInfo;
import np2015.Neighbor;

/**
 * Diese Klasse erbt von Column und deckt den linken Rand ab.
 **/

public class LeftBorder extends Column {

	/*
	 * Leftborder muss nur mit dem rechten Nachbarn kommunizieren.
	 */
	private Hashtable<Integer, Double> outRight;
	private Exchanger<Hashtable<Integer, Double>> rightEx;
	/*
	 * Raten werden gespeicht, damit sie nicht immer neu berechnet werden
	 * müssen.
	 */
	private double[][] rates;

	public LeftBorder(GraphInfo graph, int y, Exchanger<Hashtable<Integer, Double>> right) {
		super(graph, y);

		this.outRight = new Hashtable<>(graph.height, 1);
		this.rightEx = right;

		this.rates = new double[graph.height][3];
		for (int i = 0; i < graph.height; i++) {
			this.rates[i][0] = graph.getRateForTarget(me, i, Neighbor.Top);
			this.rates[i][1] = graph.getRateForTarget(me, i, Neighbor.Right);
			this.rates[i][2] = graph.getRateForTarget(me, i, Neighbor.Bottom);
			/*
			 * Normiere Raten
			 */
			double sum = this.rates[i][0] + this.rates[i][1] + this.rates[i][2];
			if (sum > 1) {
				this.rates[i][0] = this.rates[i][0] / sum;
				this.rates[i][2] = this.rates[i][2] / sum;
				this.rates[i][1] = this.rates[i][1] / sum;
			}
		}
	}

	/**
	 * Führt die lokalen Iterationen aus (siehe Middle), angepasst auf den
	 * fehlenden linken Nachbarn.
	 * 
	 * @return Summe der Quadrate der Änderung der Knotenwerte über alle lokalen
	 *         Iterationen bezüglich des horizontalen Flows.
	 */
	@Override
	public synchronized Double call() {
		if (values.size() != 0)
			localIteration();
		/*
		 * Zur Berechnung des Rückgabe Wertes sind sowohl Inflow als auch
		 * Outflow nötig, also wird eine Referenz auf den Outflow
		 * zwischengespeichert und dann werden die Outflows mit den
		 * Nachbarspalten getauscht. Die Tables müssen nicht durch Locks
		 * geschützt werden, da bis zum Austauschen alle Schreizugriffe
		 * abgeschlossen sind und duch das blockierende Warten beim Austauschen
		 * auch garantiert ist, dass nach dem Austauschen die Nachbarspalten
		 * fertig mit den Schreibzugriffen sind. Nach dem Austauschen finden
		 * dann nur noch Lesezugriffe auf die (eigenen und gemerkten)
		 * Outflowtables statt und mit dem return werden die gemerkten
		 * Referenzen wieder vergessen. Es gibt also keine Dataraces!
		 */
		Hashtable<Integer, Double> rightAccu = outRight;
		exchange();
		double ret = getDelta(rightAccu, outRight);
		computeNewValues();
		return ret;

	}

	/*
	 * Der Unterschied zum allgemeinen Fall ist, dass setAndComputeOutflow fuer
	 * drei statt vier Nachbarn aufgerufen wird.
	 */
	@Override
	public synchronized void localIteration() {
		/*
		 * Vor den lokalen Iterationen muss der horizontalen Outflows auf 0
		 * gesetzt werden.
		 */
		outRight = new Hashtable<>(height, 1);
		/*
		 * Jetzt werden so viele lokale Iterationen ausgeführt wie in
		 * localIterations steht.
		 */
		for (int i = 0; i < localIterations; i++) {
			/*
			 * Zu Beginn des lokalen Iterationschrittes muss der Akku des
			 * vertikalen Flows auf 0 gesetzt werden.
			 */
			akku = new Hashtable<>(height, 1);
			/*
			 * Für jeden Knoten werden die Outflows berechnet und in den
			 * entsprechenden Akkumulatoren hinzugefügt.
			 */
			for (Entry<Integer, Double> knoten : values.entrySet()) {
				double val = knoten.getValue();
				int currentPos = knoten.getKey();
				/*
				 * Der Outflow in jede Richtung wird berechnet. Der berechnete
				 * Wert wird in den Akkus der empfangenden Knoten abgelegt (das
				 * uebernimmt die Methode setAndComputeValues). Die Summe des
				 * Outflows wird dann vom Akku abgezogen.
				 */
				val = -(setAndComputeOutflow(akku, val, currentPos - 1, rates[currentPos][0])
						+ setAndComputeOutflow(akku, val, currentPos + 1, rates[currentPos][2])
						+ setAndComputeOutflow(outRight, val, currentPos, rates[currentPos][1]));
				akku.put(currentPos, akku.getOrDefault(currentPos, 0.0) + val);
			}
			/*
			 * Am Ende jedes lokalen Iterationschrittes werden die Werte der
			 * Knoten einer Spalte mit ihrem korrespondierenden Akku Eintrag
			 * verrechnet. Erreicht der vertikale Flow ein Gleichgewicht, also
			 * erfuellt ein Konvergenzkriterium, bricht die lokalte Iteration
			 * ab.
			 */
			if (addAccuToValuesAndLocalConvergence(akku, values))
				break;
		}
	}

	@Override
	public synchronized void computeNewValues() {
		for (Entry<Integer, Double> entry : outRight.entrySet()) {
			int pos = entry.getKey();
			double val = entry.getValue();
			values.put(pos, values.getOrDefault(pos, 0.0) + val);
		}
	}

	@Override
	protected synchronized void exchange() {
		try {
			outRight = rightEx.exchange(outRight);
		} catch (InterruptedException e) {
			System.out.println("Exchange failed :/");
			e.printStackTrace();
		}
	}

	@Override
	public synchronized double serialSigma() {
		double sigma = 0.0;
		for (int i = 0; i < height; i++) {
			double val = akku.getOrDefault(i, 0.0) + outRight.getOrDefault(i, 0.0);
			sigma = sigma + val * val;
		}
		return sigma;
	}

	/*
	 * Die Setter und Getter fuer den sequentiellen Programmteil.
	 */

	/**
	 * Da LeftBorder keinen linken Nachbarn hat, wird null zurückgegeben.
	 * 
	 * @return null.
	 */
	@Override
	public synchronized Hashtable<Integer, Double> getLeft() {
		return null;
	}

	@Override
	public synchronized Hashtable<Integer, Double> getRight() {
		return outRight;
	}

	/**
	 * Da LeftBorder keinen linken Nachbarn hat, wird hier nichts gemacht.
	 */
	@Override
	public synchronized void setLeft(Hashtable<Integer, Double> right) {
		return;
	}

	@Override
	public synchronized void setRight(Hashtable<Integer, Double> left) {
		outRight = left;
	}
	
	public synchronized Exchanger<Hashtable<Integer,Double>> getEx(){
		return rightEx;
	}

}
