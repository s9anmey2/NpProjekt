package implementation;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import np2015.GraphInfo;

/**
 * Die Klasse Column stellt eine Spalte eines Gitters dar und enthält Methoden,
 * welche die Berechnungen ausführen, die nur eine Spalte betreffen.
 * 
 * Alle Unterklassen sind Monitore.
 * 
 * Das Gitter in Spalten zu unterteilen, die parallel bearbeitet werden, bedingt
 * zwei Randfaelle. Um diese abzufangen organisieren wir die Menge aller Spalten
 * in einer Klassenhierarchie, mit 3 Kindern, von denen eines den linken, eines
 * den rechten Rand implementiert und das dritte die Mitte.
 **/
abstract public class Column implements Callable<Double> {

	/*
	 * values enthält die aktuellen Werte der Spalte.
	 */
	protected double[] values;

	/*
	 * akku enthält die Werde des vertikalen Flows und wird zu Beginn jeden
	 * lokalen Iterationsschritts geleert.
	 */
	protected double[] akku;

	/*
	 * height ist die Höhe der Spalte (Höhe des Gitters). LocalIterations wird
	 * vom Supervisor ueber das Grid immer dann gesetzt, wenn sich die Zahl
	 * aendert. me ist Id der Spalte. (MittelSpalten muessen das wissen, die
	 * Raender wissen implizit wer sie sind. Trotzdem erhalten alle Kinder von
	 * Column das Feld, weil es angenehmer zu programmieren und lesbarerer Code
	 * ist, ueber eine Zeile mit ihrer Id zu sprechen als mit einem konkreten
	 * Integer.)
	 */
	protected int height, me, localIterations;

	/*
	 * Das Konvergenzkriterium anhand dessen lokale, vertikale Konvergenz
	 * gemessen wird.
	 **/
	protected double epsilonSquareDivWidth;

	/**
	 * @param graph
	 * @param y
	 */
	public Column(GraphInfo graph, int y, int localIterations) {

		this.height = graph.height;
		this.values = new double[graph.height];
		this.akku = new double[graph.height];
		this.me = y;
		this.epsilonSquareDivWidth = graph.epsilon * graph.epsilon / graph.width;
		this.localIterations = localIterations;

		/*
		 * Alle initialen Werte werden vom Konstruktor in der Hashtable values
		 * gesetzt, dazu werden die entsprechenden Eintraege erschaffen.
		 */
		HashMap<Integer, Double> initialMap = graph.column2row2initialValue.getOrDefault(y, new HashMap<>());

		for (Entry<Integer, Double> entry : initialMap.entrySet()) {
			values[entry.getKey()] = entry.getValue();
		}
	}

	/**
	 * Implementiert die Methode Call der Oberklasse Callable. Funktioniert wie
	 * run() aber mit dem Unterschied, dass call() einen Rueckgabewert haben
	 * kann.(S.http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/
	 * Callable.html). Die Methode steuert den Arbeitsablauf des Prozesses und
	 * wird vom Grid in jedem globalen Iterationsschritt aufgerufen. Hier werden
	 * die lokalen Iterationen ausgeführt, danach der Outflow mit den
	 * Nachbarspalen getauscht und die neuen Knotenwerte berechnet.
	 * 
	 * @return Das Quadrat des euklidischen Abstandes des horizontalen Flows aus
	 *         einem bestimmten globalen Iterationschritt.
	 */
	@Override
	abstract public Double call();

	/**
	 * Berechnet mit einer For- Schleife den vertikalen wie horizontalen Flow.
	 * Der Flow in jede Richtung wird mit setAndComputeOutflow(...) ermittelt.
	 * Der vertikale Flow wird auf die Akku- Positonen des oberen und unteren
	 * Nachbarn gerechnet, der Flow nach rechts und links wird akkumuliert und
	 * spaeter mit den Nachbarspalten getauscht. Nach jedem lokalen
	 * Iterationsschritt wird der Wert von values neu berechnet mit value(i) =
	 * value(i) - outflow(gesamt) + inflow(vertikal). Falls vertikale Konvergenz
	 * erreicht sein sollte, wird die Schleife abgebrochen um nicht unnötig viel
	 * zu rechnen.
	 **/
	abstract public void localIteration();

	/**
	 * Verrechnet den horizontalen Inflow mit den den Werten der Knoten einer
	 * Spalte.
	 */
	abstract public void computeNewValues();

	/**
	 * Führt den Austausch den Outflows mit den Nachbarspalten aus. Benutzt wird
	 * dazu Exchanger.exchange, dieses blockiert einen Thread solange, bis ein
	 * Partner Exchanger.exchange auf dem gleichen Exchanger aufgerufen hat.
	 * (S.Javadocs:
	 * docs.oracle.com/javase/7/docs/api/java/util/concurrent/Exchanger.html).
	 * Um lange Warteketten zu vermeiden tauscht eine Spalte mit einer ungraden
	 * Id zuerst mit ihrem rechten Partner, dann mit ihrem linken und eine
	 * Spalte mit einer graden Id erst mit ihrem linken, dann mit ihrem rechten
	 * Partner.
	 */
	abstract protected void exchange();

	/**
	 * Diese Methode wird von den Raendern aufgerufen. Sie arbeitet mit der
	 * Differenz des horizontalen Outflow/Inflow.
	 * 
	 * @param outflow
	 *            Der Outflow, den diese Spalte in einem bestimmten globalen
	 *            Iterationsschritt abgegeben hat.
	 * @param inflow
	 *            Der Inflow, den diese Spalte in demselben globalen
	 *            Iterationschritt erhalten hat.
	 * @return das Quadrat des euklidischen Abstandes zwischen Inflow, Outflow
	 *         am Ende desselben globalen Iterationsschrittes.
	 */
	synchronized protected double getDelta(double[] outflow, double[] inflow) {
		double delta = 0.0;
		for (int j = 0; j < height; j++) {
			double val = (outflow[j] - inflow[j]);
			delta = val * val + delta;
		}
		return delta;
	}

	/**
	 * Diese Methode wird von den mittleren Spalten aufgerufen. Sie arbeitet mit
	 * der Differenz des horizontalen Outflow/Inflow.. Mit dieser Information
	 * kann dann die globale Konvergenz abgeschaetzt werden.
	 * 
	 * @param outLeft
	 *            der linke Outflow, den die Spalte in einem bestimmten globalen
	 *            Iterationsschritt abegegeben hat.
	 * @param inLeft
	 *            der linke Inflow der Spalte, den die Spalte in demselben
	 *            globalen Iterationsschritt abegegeben hat.
	 * @param outRight
	 *            der rechte Outflow der Spalte, den die Spalte in demselben
	 *            globalen Iterationsschritt abegegeben hat.
	 * @param inRight
	 *            der rechte Inflow der Spalte, den die Spalte in demselben
	 *            globalen Iterationsschritt abegegeben hat.
	 * @return die Summe der Quadrate des horizontalen Flows aller Knoten der
	 *         Spalte.
	 */
	synchronized protected double getDelta(double[] outLeft, double[] inLeft, double[] outRight, double[] inRight) {
		double delta = 0.0;
		for (int j = 0; j < height; j++) {
			double val = ((outLeft[j] + outRight[j]) - (inLeft[j] + inRight[j]));
			delta = val * val + delta;
		}
		return delta;
	}

	/**
	 * Die Methode berechnet den Flow eines Knotens in eine Richtung und setzt
	 * den Inflow.
	 * 
	 * @param map
	 *            Hashmap zu der der Inflow addiert werden soll.
	 * @param val
	 *            Der Wert des Knotens, von dem der Flow ausgeht.
	 * @param position
	 *            Die Position an der der Inflow addiert werden soll.
	 * @param rate
	 *            Die Abflussrate in eine bestimmte Richtung.
	 * @return Absoluter Outflow, dieser wurde noch nicht gesetzt.
	 */
	protected synchronized double setAndComputeOutflow(double[] map, double val, int position, double rate) {
		double ret = 0.0;
		/*
		 * Falls rate oder val 0 ist, aendert sich der Wert in der Map nicht,
		 * dann muss auch nichts angepasst werden.
		 */
		if (rate != 0.0 && val != 0.0) {
			ret = val * rate;
			map[position] = map[position] + ret;
		}
		return ret;
	}

	/**
	 * Die Methode addiert die Akkuwerte auf die Knotenwerte und bestimmt ob die
	 * Spalte einen lokal konvergenten Zustand erreicht hat.
	 * 
	 * @param akku
	 *            Hashtable mit den zu addierenden Werten
	 * @param values
	 *            Hashtable mit den Knotenwerten
	 * @return true falls die Spalte einen lokal konvergenten Zustand erreicht
	 *         hat
	 */
	synchronized protected boolean addAccuToValuesAndLocalConvergence(double[] akku, double[] values) {
		double sigma = 0.0;
		for (int pos = 0; pos < height; pos++) {
			double val = akku[pos];
			sigma = sigma + val * val;
			values[pos] = values[pos] + val;
		}
		return sigma <= epsilonSquareDivWidth;
	}

	/**
	 * Diese Methode wird von der sequentielle Loesung verwendet. Hier wird die
	 * Summe der Quadrate der Wertänderungen einer Spalte berechntet (dient dem
	 * Überprüfen den globalen Konvergenz).
	 * 
	 * @return sigma = sum(akku(i)^2), 0<=i<graph.height.
	 */
	abstract public double serialSigma();

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
		return values[row];
	}

	public synchronized boolean hasValue() {
		boolean result = false;
		for (double x : values)
			result = result || (x > 0.0);
		return result;
	}

	public synchronized double[] getValues() {
		return values;
	}

	public synchronized void setValues(double[] init) {
		this.values = init;
	}

	abstract public double[] getLeft();

	abstract public double[] getRight();

	abstract public void setLeft(double[] right);

	abstract public void setRight(double[] left);
}
