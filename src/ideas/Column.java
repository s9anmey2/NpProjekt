package ideas;

import java.util.Hashtable;

public class Column extends Thread{
	
	/**
	 * IDEE: statt der DeleteFlag machen wir einen bufferedIntchan. Wenn gilt column vertical sum == 0, dann 
	 * sendet nodeeval die id der Spalte in den chan. Am Ende eines globalen iterationsschrittes iteriert der 
	 * supervisor über den chan, und löscht jeden thread mit der empfangenen id. Der chan sollte nach Lifo aus-
	 * geräumt werden und intchan[0]= -1, dann muss nur jede id auf -1 verglichen werden und der supervisor weiß,
	 * wann er fertig ist.
	**/
	Hashtable<Integer, Double> values = new Hashtable<Integer,Double>();
	Hashtable<Integer, Double> outputLeft = new Hashtable<Integer,Double>();
	Hashtable<Integer, Double> outputRight = new Hashtable<Integer,Double>();
	Hashtable<Integer, Double> akku = new Hashtable<Integer,Double>();
	double akkusum; /**akkusum ist die summer aller akku werte nach ALLEN bisherigen iterschritten**/ 
	double flag; /**summe aller values on run**/
	double sigma; /**das epsilon der spalten. Wie berechnet man das sinnvoll? 
					sigma = f(epsilon) = id(epsilon) für den Anfang, müssen wir ein bisschen experimentieren**/
	
	public double getFlag(){
		/**ruft der Exchanger auf, falls das Ding 0 is oder 0.0000001 oder so, dann lässt er diesen Thread 
		löschen.**/
		return flag;
	}

	public synchronized Hashtable<Integer,Double> getLeft(){
		return outputLeft;
	}
	
	public synchronized Hashtable<Integer,Double> getRight(){
		return outputRight;
	}
	
	public synchronized void setLeft(Hashtable<Integer,Double> inputLeft){
		outputLeft = inputLeft;
	}
	
	public synchronized void setRight(Hashtable<Integer,Double> inputRight){
		outputRight = inputRight;
	}
	
	private void computeLeft(){
		/**analog für rechts**/
		double dummy = 0.1;
		double acc;
		int i;
		/**for (Iterator<> iter = values.Iterator)
		acc = iter(value)*dummy;
		outputLeft.put(iter(key),acc);
		akku.put(iter(key),(akk.get(key) - acc));

		**/
	}
	
	private void computeRight(){
	}
	
	public void computeNewValues(){
		/**
		 * Wird am Ende der globalen Iteration vom Exchanger nach Austausch des Flows aufgerufen. 
		 * 
		 * Wenn sich hier raus stellt, dass alle values 0 sind? (Wo überprüfen wir das und wie?)
		 * Hier müssen also alle neuen Werte (on Run init) berechnet werden aus values = values + outoutLeft +
		 * outRight. Dabei dann immer neuer invoke von run: flag = 0; for x to y:  flag = flag + value(i);
		 * GANZ WICHTIG: ALLE WERTE IN outputLEft, Right auf 0 setzten!
		 * 
		 * **/
	}
	
	public void run(int bla){
		boolean condition = true;
	
		 	for(int  j =0; j<bla; j++)
				computeLeft();
				computeRight();
				computeVertical();
				double check = clearAccu();
				if (condition = check < sigma){
					
					while(true){
						wait(); /**hat die barrier erreicht und wartet auf die andern(plus break bedingung des overseer)**/
						barrier.signal(); /**sagt dem overseer, dass dieser thread wartet. Wenn alle warten, nächste iteration **/
					}
				}else {
					barrier.signal();
			}
		}
	}
	private void computeVertical(){
		double accOben, accUnten;
		int key = 2;
		akku.put(akku(key+1),(akku.get(key) + accOben));
		akku.put(akku(key-1),(akku.get(key) + accUnten));
		akku.put(akku(key),(akku.get(key) -( accOben+ accUnten)));



	}
	
	private double clearAccu(){
		double check;
		/**values werden mit akku verrechnet.currentvalue:= aktuelle einträge des akkus**/
		for blabla
		check= check +currentvalue; /**check ist die summe aller akku werte in EINEM lokalen iterationsschritt**/
		akkusum = akkusum + check;/**akkusum ist die summer aller akku werte nach ALLEN bisherigen iterschritten**/
			
	

}
