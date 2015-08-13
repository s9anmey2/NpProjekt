package implementation;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import np2015.GraphInfo;
import np2015.Neighbor;

/**
 * Die Klasse Column stellt eine Spalte eines Gitters dar und enthält Methoden,
 * welche die Berechnungen ausführen, die nur eine Spalte betreffen.
 */
public class Column implements Callable<Double>{
	
	/**
	 * values enthält die aktuellen Werte der Spalte.
	 */
	private Hashtable<Integer, Double> values;
	/**
	 * outLeft und outRight enthalten die Werte, die nach den lokalen
	 * Iterationsschritten an die Nachbarspalten abgegeben werden und 
	 * werden zu Beginn eines globalen Iterationsschritts geleert.
	 */
	private Hashtable<Integer, Double> outLeft;	
	private Hashtable<Integer, Double> outRight;
	/**
	 * akku enthält die Werde des vertikalen Flows und wird zu Beginn 
	 * jeden lokalen Iterationsschritts geleert.
	 */
	private Hashtable<Integer, Double> akku;

	//private double deleteFlag; // TODO ferner liefen: ganz am schluss wenn noch zeit ist.-
	
	/**
	 * sigma enthält am Ende der lokalen Iteration die Quadrate der Akkuwerte
	 * und dient der Verhinderung unnötig vieler lokaler Iterationen.
	 */
	private double sigma;
	private GraphInfo graph;		//graph.getRateForTarget(x,y,<Neighbor>) Neighbor:={Left, Right, Top, Bottom}
	private Grid grid; /**ueber das grid kommt die column mit grid.getLOcals an die locale schrittzahl ran.**/
	private int me;
	private final double crit;

	public Column(GraphInfo graph, Grid grid, int y, boolean isDummy) {
		
		/**aufgerufen von grid "echte" spalte.**/
		double square;
		this.graph = graph;
		this.grid = grid;
		this.values = new Hashtable<>();
		this.outRight = new Hashtable<>();
		this.outLeft = new Hashtable<>();
		this.akku = new Hashtable<>();
		this.me = y; //y ist die spaltennummer
		square = graph.epsilon/graph.width;
		this.crit = square*square;
		
		if(!(isDummy)){
			HashMap<Integer, Double> name = graph.column2row2initialValue.getOrDefault(y, new HashMap<>());
			Iterator<Entry<Integer,Double>> iter = name.entrySet().iterator();
			
			while(iter.hasNext()){
				Entry<Integer,Double> dummy = iter.next();
				int row = dummy.getKey();
				double val = dummy.getValue();
				values.put(row, val);			
			}
		}
	}

	
	@Override
	public synchronized Double call() {
		
		/**berechnet den akku und den horizontalen outflow knotenweise.**/
		outLeft = new Hashtable<>();
		outRight= new Hashtable<>();
		int localIterations = grid.getLocals();
		for (int i=0; i<localIterations; i++){
			akku = new Hashtable<>();
			sigma = 0.0;
			Iterator<Entry<Integer, Double>> knoten = values.entrySet().iterator();
			while(knoten.hasNext()){
				/** hier ist keine ordnung definiert, also muss immer mit geprueft werden, ob es an der Stelle schon einen Knoten gibt.**/
				
				Entry<Integer, Double> dummy= knoten.next();
				double outflowLeft,outflowRight, outFlowTop, outFlowDown; //outflowTop (von n nach n-1) outFlowDown (von n nach n+1)
				double val = dummy.getValue();
				int currentPos = dummy.getKey();
				
				/**value wird jetzt mit den outflow rate verrechnet, dabei muss **/
				
				outflowLeft = val * graph.getRateForTarget(me, currentPos, Neighbor.Left);
				outflowRight= val * graph.getRateForTarget(me, currentPos, Neighbor.Right);
				outFlowTop  = val * graph.getRateForTarget(me, currentPos, Neighbor.Top);
				outFlowDown = val * graph.getRateForTarget(me, currentPos, Neighbor.Bottom);
				
				/**die summe des outflows wird jetzt vom akku der aktuellen position abgezogen und gesetzt.**/
				
				val = -(outflowLeft + outflowRight + outFlowTop + outFlowDown);
								
				/**der korrespondierende akku eintrag wird aktualisiert/angelegt. **/
				addOrReplaceEntry(akku, currentPos, akku.getOrDefault(currentPos,0.0) + val);
			
				/**die akkus von vorgaenger und nachfolger muessen geaendert werden. Zwei Faelle: *sessor gibt es noch nicht -> machen und mit outflowtop/bottom 
				 initialisieren. *sessor gibt es schon -> alten wert mit outflowtop bottom verrechnen und setzen.
				 Die Fallunterscheidung is in der der Methode addOrReplaceEntry implementiert.**/

				if(currentPos > 0)	//if fuer randfall 0	
					addOrReplaceEntry(akku, currentPos -1, akku.getOrDefault(currentPos -1,0.0) + outFlowTop);
				
				if(currentPos < graph.height-1)//if fuer randfall max
					addOrReplaceEntry(akku, currentPos +1, akku.getOrDefault(currentPos +1, 0.0) + outFlowDown);
				else if(outFlowDown>0.0)
					System.out.println("Hier läuft es raus top: " +  outFlowDown + "at position " + currentPos);
					
				if(me>0) //if fuer randfall, spalte = 0
					addOrReplaceEntry(outLeft, currentPos, outLeft.getOrDefault(currentPos, 0.0) + outflowLeft);
				
				if(me<graph.width-1)
					addOrReplaceEntry(outRight, currentPos, outRight.getOrDefault(currentPos, 0.0) + outflowRight);
				
			
			}//while schleife zu
			
			/**hier werden alle eintraege mit denen des akkus verechnet.**/
			Iterator<Entry<Integer, Double>> acc = akku.entrySet().iterator();
			while(acc.hasNext()){
				Entry<Integer, Double> dummy = acc.next();
				int pos = dummy.getKey();
				double val = dummy.getValue();
				sigma = sigma + val*val;
				addOrReplaceEntry(values, pos, values.getOrDefault(pos,0.0) + val);
			}//while schleife zu
			
			if(sigma <= crit)
				break; /**falls lokale konvergenz erreicht ist, bricht die Forschleife ab.**/
		}//for schleife zu
		return 0.0;
	}
	
	public double getSum(){
		double ret= 0.0;
		Iterator<Entry<Integer,Double>> row = values.entrySet().iterator();
		while(row.hasNext())
			ret = ret + row.next().getValue();
		return ret;
	}
	
	public double serialSigma(){
		/**fuer die sequentielle loesung wichtig. merkt sich in sigma die summe der quadrate aus horizontalem und vertikalem outflow **/
		double sigma=0.0;
		for (int i=0; i<graph.height; i++){
			double val = akku.getOrDefault(i, 0.0) + outLeft.getOrDefault(i, 0.0) + outRight.getOrDefault(i,0.0);
			sigma = sigma + val*val;		
		}
		return sigma;
	}
	
	public synchronized void computeNewValues() {
		
		/**verrechnet inflow und akku mit den alten values.**/
		if(me>0){
			Iterator<Entry<Integer, Double>> left = outLeft.entrySet().iterator();
			while(left.hasNext()){
				Entry<Integer, Double> dummy = left.next();
				int pos = dummy.getKey();
				double val = dummy.getValue();
				addOrReplaceEntry(values, pos, values.getOrDefault(pos, 0.0) + val);			
			}//while zu
			
		}if(me<graph.width){
			Iterator<Entry<Integer, Double>> right= outRight.entrySet().iterator();
			while(right.hasNext()){
				Entry<Integer, Double> dummy = right.next();
				int pos = dummy.getKey();
				double val = dummy.getValue();
				addOrReplaceEntry(values, pos, values.getOrDefault(pos, 0.0) + val);			
			}//while zu
		}

	}
	
	private synchronized void addOrReplaceEntry(Hashtable<Integer, Double> map, int key, double val){
		
		/**aktualisiert oder ergaenzt eintraege in hashtables**/
		if(map.containsKey(key))
			map.replace(key, val);
		else
			map.put(key, val);
	}
	
	public synchronized Hashtable<Integer, Double> getLeft(){
		return outLeft;
	}
	
	public synchronized Hashtable<Integer, Double> getRight(){
		return outRight;
	}
	
	public synchronized double getValue(int row){
		return values.getOrDefault(row, 0.0);
	}
	
	public synchronized void setLeft(Hashtable<Integer, Double> right){
		outLeft = right;
	}
	
	public synchronized void setRight(Hashtable<Integer, Double> left){
		outRight= left;
	}
	
}
