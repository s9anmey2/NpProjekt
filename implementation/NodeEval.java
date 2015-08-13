package implementation;

/**
 * Die Klasse enthält ein Column Objekt und dient dazu auf Column Objekten 
 * weitere Funktionalität als zusätzliche Ausführungsstänge starten zu können.
 */
public class NodeEval extends Thread {
	
	/**
	 * Die Spalte auf der gearbeitet werden soll.
	 */
	private Column column;
	
	/**
	 * Der Konstruktor baut ein NodeEval Objekt, welches die übergebene 
	 * Column enthält.
	 * 
	 * @param column
	 */
	public NodeEval(Column column) {
		this.column = column;
	}
	
	/**
	 * Ruft die Berechnung der Knotenwerte (computeNewValues()) auf der 
	 * enthaltenen Column auf.
	 */
	@Override
	public synchronized void run() {
		column.computeNewValues();
	}

}
