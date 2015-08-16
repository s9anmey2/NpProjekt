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
 * 
 * Das Gitter in Spalten zu unterteilen, die parallel bearbeitet werden, bedingt
 * zwei Randfaelle. Um diese abzufangen organisieren wir die Menge aller Spalten
 * in einer Klassenhierarchie, mit 3 Kindern, von denen eines den linken, eines
 * den rechten Rand implementiert und das dritte den allgemeinen Fall.
 **/
abstract public class Column implements Callable<Double> {

	/**
	 * values enthält die aktuellen Werte der Spalte.
	 */
	protected Hashtable<Integer, Double> values;

	/**
	 * akku enthält die Werde des vertikalen Flows und wird zu Beginn jeden
	 * lokalen Iterationsschritts geleert.
	 */
	protected Hashtable<Integer, Double> akku;

	/**
	 * Der graph erzaehlt einer Column, wie groß sie ist. LocalIterations wird
	 * vom Supervisor ueber das Grid immer dann gesetzt, wenn sich die Zahl
	 * aendert. me ist id der Spalte. MittelSpalten muessen das wissen, die
	 * Raender wissen implizit wer sie sind. Trotzdem erhalten alle Kinder von
	 * Column das Feld, weil es angenehmer zu programmieren und lesbarerer Code
	 * ist, ueber eine Zeile mit ihrer Id zu sprechen als mit einem konkreten
	 * Integer.
	 */
	protected GraphInfo graph;
	protected Grid grid;
	protected int me, localIterations;

	/**
	 * Das Konvergenzkriterium anhand dessen lokale, vertikale Konvergenz
	 * gemessen wird.
	 * **/
	protected double epsilon;

	public Column(GraphInfo graph, Grid grid, int y) {

		this.graph = graph;
		this.grid = grid;
		this.values = new Hashtable<>(graph.height, 1);
		this.akku = new Hashtable<>(graph.height, 1);
		this.me = y; // y ist die spaltennummer/id
		this.epsilon = graph.epsilon * graph.epsilon / graph.width;

		/*
		 * Alle a priori bekannten Werte werden vom Konstruktor in der Hashtable
		 * Values gesetzt, dazu werden die Eintraege erschaffen
		 */
		HashMap<Integer, Double> name = graph.column2row2initialValue
				.getOrDefault(y, new HashMap<>());
		Iterator<Entry<Integer, Double>> iter = name.entrySet().iterator();

		while (iter.hasNext()) {
			Entry<Integer, Double> dummy = iter.next();
			int row = dummy.getKey();
			double val = dummy.getValue();
			values.put(row, val);

		}
	}

	/**
	 * Implementiert die Methode Call der Oberklasse Callable. Funktioniert wie
	 * run() aber mit dem Unterschied, dass call() einen Rueckgabewert haben
	 * kann.(S.http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/
	 * Callable.html). Die Methode steuert den Arbeitsablauf des Prozesses und
	 * wird vom Grid in jedem globalen Iterationsschritt aufgerufen.
	 * 
	 * @return Das Quadrat des euklidischen Abstandes des horizontalen Flows aus
	 *         einem bestimmten globalen Iterationschritt.
	 */
	@Override
	abstract public Double call();

	/**
	 * Exchange.exchange blockiert einen Thread solange, bis der exchange
	 * Partner auf der anderen Seite exchange.exchange aufgerufen hat.
	 * (S.Javadocs:
	 * docs.oracle.com/javase/7/docs/api/java/util/concurrent/Exchanger.html) Um
	 * einen Deadlock zu vermeiden tauscht eine ungerade Spalte zuerst mit ihrem
	 * rechten Partner, dann mit ihrem linken und eine gerade Spalte erst mit
	 * ihrem rechten, dann mit ihrem linken Partner.
	 */
	abstract protected void exchange();

	/**
	 * Berechnet mit einer For- Schleife den vertikalen wie horizontalen Flow.
	 * Der Flow in jede Richtung wird mit setAndComputeOutflow(...) ermittelt.
	 * Der vertikale Flow wird auf die Akku- Positonen des oberen und unteren
	 * Nachbarn gerechnet, der Flow nach rechts wird in outRight
	 * zwischengespeichert und spaeter mit Exchange nach rechts ueber- geben.
	 * Nach jedem lokalen Iterationsschritt wird der Wert von values neu
	 * berechnet mit value(i) = value(i) - outflow(gesamt) + inflow(vertikal).
	 * Falls vertikale Konvergenz erreicht sein sollte, wird die Schleife
	 * abgebrochen.
	 **/
	abstract public void localIteration();

	/**
	 * Diese Methode verwendet die sequentielle Loesung. merkt sich in sigma die
	 * summe der quadrate aus horizontalem und vertikalem outflow
	 * 
	 * @return sigma = sum(akku(i)^2), 0<=i<graph.height.
	 */
	abstract public double serialSigma();

	/**
	 * verrechnet den horizontalen inflow mit den den Werten der Knoten einer
	 * Spalte.
	 */
	abstract public void computeNewValues();

	/**
	 * Setter und Getter Anfang.
	 */
	abstract public Hashtable<Integer, Double> getLeft();

	abstract public Hashtable<Integer, Double> getRight();

	abstract public void setLeft(Hashtable<Integer, Double> right);

	abstract public void setRight(Hashtable<Integer, Double> left);

	/**
	 * Setter und Getter Ende.
	 */

	synchronized protected boolean addAccuToValuesAndLocalConvergence(
			Hashtable<Integer, Double> akku, Hashtable<Integer, Double> values) {
		/** hier werden alle eintraege mit denen des akkus verechnet. **/
		double sigma = 0.0;
		Iterator<Entry<Integer, Double>> acc = akku.entrySet().iterator();

		while (acc.hasNext()) {
			Entry<Integer, Double> dummy = acc.next();
			int pos = dummy.getKey();
			double val = dummy.getValue();
			sigma = sigma + val * val;
			addOrReplaceEntry(values, pos, values.getOrDefault(pos, 0.0) + val);
		} // while schleife zu

		return sigma <= epsilon;
	}

	/**
	 * Diese Methode wird von den Raendern aufgerufen. Sie arbeitet mit der
	 * Differenz des horizontalen Outflow/Inflow.
	 * 
	 * @param outflow
	 *            Der outflow, den diese Spalte in einem bestimmten globalen
	 *            Iterationsschritt abgegeben hat.
	 * @param inflow
	 *            Der Inflow, den diese Spalte in demselben globalen
	 *            Iterationschritt erhalten hat.
	 * @return das Quadrat des euklidischen Abstandes zwischen Inflow, Outflow
	 *         am Ende desselben globalen Iterationsschrittes.
	 */
	synchronized protected double getDelta(Hashtable<Integer, Double> outflow,
			Hashtable<Integer, Double> inflow) {
		/**
		 * Der Sinn dahinter ist: falls das Verhalten des Prozesses mit einer
		 * bestimmten lokalen Iterationszahl zyklisch ist, dann ist das
		 * Verhältnis delta: previousDelta <1, weshalb die Schrittzahl geaendert
		 * werden muss.
		 **/

		double delta = 0.0;
		for (int j = 0; j < graph.height; j++) {
			double val = (outflow.getOrDefault(j, 0.0) - inflow.getOrDefault(j,
					0.0));
			delta = val * val + delta;
		}
		return delta;
	}

	/**
	 * Diese Methode wird von den mittleren Spalten aufgerufen. Sie arbeitet mit
	 * der Differenz des horizontalen Outflow/Inflow.. Mit dieser Information
	 * kann dann die globale Konvergenz abgeschaetzt werden.
	 * 
	 * @param leftAccu
	 *            der linke Outflow, den die Spalte in einem bestimmten globalen
	 *            Iterationsschritt abegegeben hat.
	 * @param outLeft
	 *            der linke Inflow der Spalte, den die Spalte in demselben
	 *            globalen Iterationsschritt abegegeben hat.
	 * @param rightAccu
	 *            der rechte Outflow der Spalte, den die Spalte in demselben
	 *            globalen Iterationsschritt abegegeben hat.
	 * @param outRight
	 *            der rechte Inflow der Spalte, den die Spalte in demselben
	 *            globalen Iterationsschritt abegegeben hat.
	 * @return die Summe der Quadrate des horizontalen Flows aller Knoten der
	 *         Spalte.
	 */
	synchronized protected double getDelta(Hashtable<Integer, Double> leftAccu,
			Hashtable<Integer, Double> outLeft,
			Hashtable<Integer, Double> rightAccu,
			Hashtable<Integer, Double> outRight) {
		double delta = 0.0;
		for (int j = 0; j < graph.height; j++) {
			double val = ((leftAccu.getOrDefault(j, 0.0) + rightAccu
					.getOrDefault(j, 0.0)) - (outLeft.getOrDefault(j, 0.0) + outRight
					.getOrDefault(j, 0.0)));
			delta = val * val + delta;
		}
		return delta;
	}

	/**
	 * Die Methode berechnet den Outflow eines Knotens in eine Richtung.
	 * 
	 * @param map
	 *            Enthaelt den Akku des Empfaenger des Outflows, wo dieser als
	 *            Inflow gespeichert wird.
	 * @param val
	 *            Der Wert des aufrufenden Knotens.
	 * @param currentPos
	 *            Die Position des Akku, an der der Inflow abgelegt werden muss.
	 * @param rate
	 *            Die Abflussrate in eine bestimmte Richtung.
	 * @return den Outflow, den der Aufrufer sich in seinem Akku merkt.
	 */
	protected double setAndComputeOutflow(Hashtable<Integer, Double> map,
			double val, int currentPos, double rate) {
		double ret = 0.0;
		/*
		 * falls die rate 0 ist oder der val 0 ist, aendert sich nichts, dann
		 * muss auch nichts angepasst werden.
		 */
		if (rate != 0.0 && val != 0.0) {
			ret = val * rate;
			addOrReplaceEntry(map, currentPos,
					map.getOrDefault(currentPos, 0.0) + ret);
		}
		return ret;
	}

	/**
	 * Fuegt in einem Akku oder einer Spalte Eintraege hinzu oder ersetzt
	 * vorhandene Eintraege durch neue.
	 * 
	 * @param map
	 *            Der zu manipulierende Hashtable.
	 * @param key
	 *            Die Position die neu angelegt oder überschrieben werden soll.
	 * @param val
	 *            Der Wert der dort zu speicher ist.
	 */
	protected synchronized void addOrReplaceEntry(
			Hashtable<Integer, Double> map, int key, double val) {

		if (map.containsKey(key))
			map.replace(key, val);
		else if (val != 0)
			map.put(key, val);
	}

	/**
	 * Setzt Die Anzahl der lokalen Iterationsschritte neu.
	 * 
	 * @param n
	 *            Die neue Anzahl lokaler Iterationsschritte.
	 */
	synchronized public void setLocals(int n) {
		localIterations = n;
	}

	/**
	 * Liefert den Wert einer Spalte in der spezifizierten Spalte. Die Funktion
	 * wird von getValeAt() im Grid aufgerufen, das diese Methode von
	 * ImageConvertible implementiert.
	 * 
	 * @param row
	 *            Die Zeile, deren Wert geliefert werden soll.
	 * @return Den Wert des Knoten mit id row.
	 */
	public synchronized double getValue(int row) {
		return values.getOrDefault(row, 0.0);
	}

}
