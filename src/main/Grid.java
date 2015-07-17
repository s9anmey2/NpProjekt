package main;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

public class Grid {
	
	/**TODO Wir müssen aus Columns noch eine Unterklasse von Thread machen.**/
	
	
/**What am i good for? Es kapselt die columns**/
	
	private Hashtable<Integer, Column> columns = new Hashtable<Integer,Column>();
	int AnzahlDerLokalenIterationen;
	
	public void set(int i){
		AnzahlDerLokalenIterationen = i;
	}
	
	public synchronized Column getColumn(int index){
		if (columns.containsKey(index))
			return columns.get(index);
		else{
			columns.put(index, new Column());
			return columns.get(index);
		}
	}
	
	public void iterate(){
		Iterator<Entry<Integer,Column>> iter = columns.entrySet().iterator();
		this.iterate(iter);
;
		
	}

	private void iterate(Iterator<Entry<Integer,Column>> iter){
		if (iter.hasNext()){
			/**startet den jeweiligen thread und iteriert weiter bis zum nächsten und 
			 * nächsten bis keiner mehr da ist und wartet dann auf join**/
			Column temp = iter.next().getValue(); /**next ist ein paar aus id und value, wir 
												wollen ja nur den value**/
			temp.run(AnzahlDerLokalenIterationen);
			iterate(iter);
			temp.join();
		}
	}

}
