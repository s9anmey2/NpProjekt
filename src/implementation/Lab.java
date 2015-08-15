package implementation;

public class Lab {
	private int maxIt, maxCon, minCon, invokesCon, invokesIt, sum, forAverage;
	
	public Lab(){
		maxCon = 0;
		maxIt = 0;
		minCon = 300;
		invokesCon = 0; 
		invokesIt = 0;
		sum = 0;
		forAverage=0;
	}

	public void print(){
		System.out.println("Abbrüche wegen lokaler Konvergenz: " + invokesCon);
		System.out.println("maximale Anzahl lokaler Iterationen bis zur lokalen Konvergenz: " + maxCon);
		System.out.println("minimale Anzahl lokaler Iterationen bis zur lokalen Konvergenz: " + minCon);
		System.out.println("durschnittliche Anzahl lokaler Iterationen bis zur lokalen Konvergenz: " + sum/forAverage);
		System.out.println("Vollständige Schleifendurchläufe: " + invokesIt);
		System.out.println("maximale Anzahl lokaler Iterationen ohne Erreichen der lokalen Konvergenz: " + maxIt);
	}
	
	public synchronized void setBreak(int i){
		/**wird aufgerufen, wenn lokal konvergiert wurde.**/
		invokesCon++;
		if(i>maxCon)
			maxCon = i;
		else if(i<minCon && i != 0)
			minCon = i;
		sum = sum + i;
		if(i!=0)
			forAverage++;
	}
	
	public synchronized void setNoBreak(int i){
		invokesIt++;
		if(i>maxIt)
			maxIt = i;
	}
	
}
