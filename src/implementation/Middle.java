package implementation;

import java.util.concurrent.Exchanger;

import np2015.GraphInfo;
import np2015.Neighbor;

/**
 * Der allgemeine Fall: Alle Spalten mit zwei Nachbarn sind Unterklasse Middle.
 */

public class Middle extends Column {

	/*
	 * outLeft und outRight enthalten die Werte, die nach den lokalen
	 * Iterationsschritten an die Nachbarspalten abgegeben werden und werden zu
	 * Beginn eines globalen Iterationsschritts geleert.
	 */
	private double[] outLeft;
	private double[] outRight;

	private Exchanger<double[]> leftEx, rightEx;
	/*
	 * Raten werden gespeicht, damit sie nicht immer neu berechnet werden
	 * müssen.
	 */
	private double[][] rates;

	public Middle(GraphInfo graph, int y, Exchanger<double[]> left, Exchanger<double[]> right, int localIterations) {
		super(graph, y, localIterations);

		this.outRight = new double[graph.height];
		this.outLeft = new double[graph.height];
		this.leftEx = left;
		this.rightEx = right;

		this.rates = new double[graph.height][4];
		for (int i = 0; i < graph.height; i++) {
			this.rates[i][0] = graph.getRateForTarget(me, i, Neighbor.Left);
			this.rates[i][2] = graph.getRateForTarget(me, i, Neighbor.Top);
			this.rates[i][1] = graph.getRateForTarget(me, i, Neighbor.Right);
			this.rates[i][3] = graph.getRateForTarget(me, i, Neighbor.Bottom);
			/*
			 * Normiere Raten
			 */
			double sum = this.rates[i][0] + this.rates[i][1] + this.rates[i][2] + this.rates[i][3];
			if (sum > 1) {
				this.rates[i][0] = this.rates[i][0] / sum;
				this.rates[i][2] = this.rates[i][2] / sum;
				this.rates[i][1] = this.rates[i][1] / sum;
				this.rates[i][3] = this.rates[i][3] / sum;
			}
		}
	}

	/**
	 * Führt die lokalen Iterationen aus. Berechnet den akku und den
	 * horizontalen outflow knotenweise. Der horizontale Flow wird nach jeder
	 * lokalen Iteration mit Exchange getauscht und dann werden die values mit
	 * computeNewValues() neu berechnet. Die Arbeit mit dem Akku findet in
	 * localIteration() statt.
	 * 
	 * @return Summe der Quadrate der Änderung der Knotenwerte über alle lokalen
	 *         Iterationen bezüglich des horizontalen Flows.
	 */
	@Override
	public synchronized Double call() {
		if (this.hasValue())
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
		double[] rightAccu = outRight;
		double[] leftAccu = outLeft;
		exchange();
		double ret = getDelta(leftAccu, outLeft, rightAccu, outRight);
		computeNewValues();
		return ret;
	}

	@Override
	public synchronized void localIteration() {
		/*
		 * Vor den lokalen Iterationen muessen die horizontalen Outflows auf 0
		 * gesetzt werden.
		 */
		outLeft = new double[height];
		outRight = new double[height];
		/*
		 * Jetzt werden so viele lokale Iterationen ausgeführt wie in
		 * localIterations steht.
		 */
		for (int i = 0; i < localIterations; i++) {
			/*
			 * Zu Beginn des lokalen Iterationschrittes muss der Akku des
			 * vertikalen Flows auf 0 gesetzt werden.
			 */
			akku = new double[height];
			/*
			 * Für jeden Knoten werden die Outflows berechnet und in den
			 * entsprechenden Akkumulatoren hinzugefügt.
			 */
			for (int currentPos = 0; currentPos < height; currentPos++) {
				double val = values[currentPos];
				/*
				 * Der Outflow in jede Richtung wird berechnet. Der berechnete
				 * Wert wird in den Akkus der empfangenden Knoten abgelegt (das
				 * uebernimmt die Methode setAndComputeValues). Die Summe des
				 * Outflows wird dann vom Akku abgezogen.
				 */
				val = -(setAndComputeOutflow(akku, val, currentPos - 1, rates[currentPos][Neighbor.Top.ordinal()])
						+ setAndComputeOutflow(akku, val, currentPos + 1, rates[currentPos][Neighbor.Bottom.ordinal()])
						+ setAndComputeOutflow(outLeft, val, currentPos, rates[currentPos][Neighbor.Left.ordinal()])
						+ setAndComputeOutflow(outRight, val, currentPos, rates[currentPos][Neighbor.Right.ordinal()]));
				akku[currentPos] = akku[currentPos] + val;

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
		for (int pos = 0; pos < height; pos++) {
			values[pos] = values[pos] + outLeft[pos] + outRight[pos];
		}
	}

	@Override
	protected synchronized void exchange() {
		try {
			if (me % 2 == 0) {
				outLeft = leftEx.exchange(outLeft);
				outRight = rightEx.exchange(outRight);
			} else {

				outRight = rightEx.exchange(outRight);
				outLeft = leftEx.exchange(outLeft);
			}
		} catch (InterruptedException e) {
			System.out.println("Exchange failed :/");
			e.printStackTrace();
		}
	}

	@Override
	public synchronized double serialSigma() {
		double sigma = 0.0;
		for (int i = 0; i < height; i++) {
			double val = akku[i] + outLeft[i] + outRight[i];
			sigma = sigma + val * val;
		}
		return sigma;
	}

	/*
	 * Die Setter und Getter fuer den sequentiellen Programmteil.
	 */

	@Override
	public synchronized double[] getLeft() {
		return outLeft;
	}

	@Override
	public synchronized double[] getRight() {
		return outRight;
	}

	@Override
	public synchronized void setLeft(double[] right) {
		outLeft = right;
	}

	@Override
	public synchronized void setRight(double[] left) {
		outRight = left;
	}

}
