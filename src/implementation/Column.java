package implementation;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;





import np2015.GraphInfo;
import np2015.Neighbor;

public class Column extends Thread {
	
	private Hashtable<Integer, Double> values;			// kein Zugriff von außen
	
	private Hashtable<Integer, Double> outLeft;			// geschützt durch explizites Lock, oder besser durch implizites, weil da jeder der das Objekt kennt Zugiff hat? TODO 
	private Hashtable<Integer, Double> outRight;		// geschützt durch explizites Lock TODO 
	
	private Hashtable<Integer, Double> oldvalues;		// kein Zugriff von außen
	private double valueDifference;						// kein Zugriff von außen
	private double deleteFlag;							// kein Zugriff von außen, ferner liefen: ganz am schluss wenn noch zeit ist.-
	private double sigma;								// kein Zugriff von außen, die summe der quadrate der akku werte
	private GraphInfo graph;		//graph.getRateForTarget(x,y,<Neighbor>) Neighbor:={Left, Right, Top, Bottom}
	private Grid grid; /**ueber das grid kommt die column mit grid.getLOcals an die locale schrittzahl ran.**/
	private int me;
	private final double crit;

	public Column(GraphInfo graph, Grid grid, int y) {
		double square;
		this.graph = graph;
		this.grid = grid;
		this.me = y; //y ist die spaltennummer
		square = graph.epsilon/graph.width;
		this.crit = square*square;
		// TODO Auto-generated constructor stub
	}

	@Override
	public synchronized void run() {
		
		/**berechnet den akku und den horizontalen outflow knotenweise.**/
		int localIterations = grid.getLocals();
		for (int i=0; i<localIterations; i++){
			sigma = 0.0;
			Iterator<Entry<Integer, Double>> knoten = values.entrySet().iterator();
			Hashtable<Integer, Double> akku = new Hashtable<>();

			while(knoten.hasNext()){
				/** hier ist keine ordnung definiert, also muss immer mit geprueft werden, ob es an der Stelle schon einen Knoten gibt.**/
				
				Entry<Integer, Double> dummy= knoten.next();
				double outflowLeft,outflowRight, outFlowTop, outFlowDown; //outflowTop (von n nach n-1) outFlowDown (von n nach n+1)
				double val = dummy.getValue();
				int currentPos = dummy.getKey();
				
				/**value wird jetzt mit den outflow rate verrechnet, dabei muss **/
				
				outflowLeft = graph.getRateForTarget(currentPos, me, Neighbor.Left);
				outflowRight= graph.getRateForTarget(currentPos, me, Neighbor.Right);
				outFlowTop  = graph.getRateForTarget(currentPos, me, Neighbor.Top);
				outFlowDown = graph.getRateForTarget(currentPos, me, Neighbor.Bottom);
				
				/**die summe des outflows wird jetzt vom akku der aktuellen position abgezogen und gesetzt.**/
				
				val = -(outflowLeft + outflowRight + outFlowTop + outFlowDown);
				/**der korrespondierende akku eintrag wird aktualisiert/angelegt. **/
				addOrReplaceEntry(akku, currentPos, akku.getOrDefault(currentPos,0.0) + val);
			
				/**die akkus von vorgaenger und nachfolger muessen geaendert werden. Zwei Faelle: *sessor gibt es noch nicht -> machen und mit outflowtop/bottom 
				 initialisieren. *sessor gibt es schon -> alten wert mit outflowtop bottom verrechnen und setzen.
				 Die Fallunterscheidung is in der der Methode addOrReplaceEntry implementiert.**/

				if(currentPos != 0)	//if fuer randfall 0	
					addOrReplaceEntry(akku, currentPos -1, akku.getOrDefault(currentPos -1,0.0) + outFlowTop);
			
				if(currentPos != graph.height)//if fuer randfall max
					addOrReplaceEntry(akku, currentPos +1, akku.getOrDefault(currentPos +1, 0.0) + outFlowDown);
				
				if(me>0) //if fuer randfall, spalte = 0
					addOrReplaceEntry(outLeft, currentPos, outLeft.getOrDefault(currentPos, 0.0) + outflowLeft);
				
				if(me<graph.width)
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
			
	}
	
	private synchronized void addOrReplaceEntry(Hashtable<Integer, Double> map, int key, double val){
		
		/**aktualisiert oder ergaenzt eintraege in hashtables**/
		if(map.containsKey(key))
			map.replace(key, val);
		else
			map.put(key, val);
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
			}//while zu+
			
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
	
	public void setValue(int pos, double val){
		values.put(pos, val);
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






















