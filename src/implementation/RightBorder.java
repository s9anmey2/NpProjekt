package implementation;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.concurrent.Exchanger;

import np2015.GraphInfo;
import np2015.Neighbor;

/**
 * Diese Klasse erbt von Column und deckt den rechten Rand ab.
 **/

public class RightBorder extends Column {

	/*
	 * Rightborder muss nur mit dem linken Nachbarn kommunizieren.
	 */
	private Hashtable<Integer, Double> outLeft;
	private Exchanger<Hashtable<Integer, Double>> leftEx;
	/*
	 * Raten werden gespeicht, damit sie nicht immer neu berechnet werden
	 * müssen.
	 */
	private double[][] rates;

	public RightBorder(GraphInfo graph, int y, Exchanger<Hashtable<Integer, Double>> left) {
		super(graph, y);
		this.outLeft = new Hashtable<>(graph.height, 1);
		this.leftEx = left;

		this.rates = new double[graph.height][3];
		for (int i = 0; i < graph.height; i++) {
			this.rates[i][0] = graph.getRateForTarget(me, i, Neighbor.Left);
			this.rates[i][1] = graph.getRateForTarget(me, i, Neighbor.Top);
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
	 * fehlenden rechten Nachbarn.
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
		Hashtable<Integer, Double> leftAccu = outLeft;
		exchange();
		double ret = getDelta(leftAccu, outLeft);
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
		outLeft = new Hashtable<>(height, 1);
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
				val = -(setAndComputeOutflow(akku, val, currentPos - 1, rates[currentPos][1])
						+ setAndComputeOutflow(akku, val, currentPos + 1, rates[currentPos][2])
						+ setAndComputeOutflow(outLeft, val, currentPos, rates[currentPos][0]));
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
		for (Entry<Integer, Double> entry : outLeft.entrySet()) {
			int pos = entry.getKey();
			double val = entry.getValue();
			values.put(pos, values.getOrDefault(pos, 0.0) + val);
		}
	}

	@Override
	protected synchronized void exchange() {
		try {
			outLeft = leftEx.exchange(outLeft);
		} catch (InterruptedException e) {
			System.out.println("Exchange failed :/");
			e.printStackTrace();
		}
	}

	@Override
	public synchronized double serialSigma() {
		double sigma = 0.0;
		for (int i = 0; i < height; i++) {
			double val = akku.getOrDefault(i, 0.0) + outLeft.getOrDefault(i, 0.0);
			sigma = sigma + val * val;
		}
		return sigma;
	}

	/*
	 * Die Setter und Getter fuer den sequentiellen Programmteil.
	 */

	@Override
	public synchronized Hashtable<Integer, Double> getLeft() {
		return outLeft;
	}

	/**
	 * Da RightBorder keinen rechten Nachbarn hat, wird null zurückgegeben.
	 * 
	 * @return null.
	 */
	@Override
	public synchronized Hashtable<Integer, Double> getRight() {
		return null;
	}

	@Override
	public synchronized void setLeft(Hashtable<Integer, Double> right) {
		outLeft = right;
	}

	/**
	 * Da RightBorder keinen rechten Nachbarn hat, wird hier nichts gemacht.
	 */
	@Override
	public synchronized void setRight(Hashtable<Integer, Double> left) {
		return;
	}

	public synchronized Exchanger<Hashtable<Integer,Double>> getEx(){
		return leftEx;
	}
}
