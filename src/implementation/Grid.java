/**
 * 
 */
package implementation;

import java.util.Hashtable;
import java.util.Iterator;

import np2015.GraphInfo;
import np2015.ImageConvertible;

public class Grid implements ImageConvertible {

	private Hashtable<Integer, Column> columns;			// Kein Zugriff von außen
	private GraphInfo graph;
	private volatile int localIterations; /**Der supervisor gibt dem grid die anzahl der localen schritte, damit sich die co,lumns diese dort abholen koennen.
	Dazu gibt das Grid dann eine referenz auf sich selbst an die columns mit**/
	
	/**wird mit dem aktuellen GraphInfo Objekt initialisiert und reicht dieses nach unten an die Columns weiter.**/
	
	public Grid(GraphInfo graph){
		this.graph = graph;
	}
	
	public Grid() {
		// TODO Auto-generated constructor stub
	}
	
	public synchronized void globalIteration() {
		// TODO
	}
	
	private void globalIteration(Iterator iter) {
		// TODO
	}
	
	public synchronized void columnValueComputation() {
		// TODO
	}
	
	private void columnValueComputation(Iterator iter) {
		// TODO
	}
	
	public synchronized void removeColumns() {
		// TODO
	}
	
	public synchronized void addColumn(int pos, Column column){
		columns.put(pos, column);
	}
	
	public synchronized void addDummyColums() {
		// TODO
	}

	public synchronized Column getColumn(int i) {
		return columns.get(i);
	}
	
	@Override
	public double getValueAt(int column, int row) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public synchronized void setLocals(int i){
		localIterations = i;
	}
	
	public int getLocals(){
		return localIterations;
	}
}
