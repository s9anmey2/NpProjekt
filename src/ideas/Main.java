package ideas;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Grid grid = new Grid();
		
		/**if barrier.isHit();
			then exchange();**/
			
			/**problematisch: mache Spalten existieren noch nicht und manche spalten können verschwinden
			 Also: wie behält der Overseer im Blick, wer noch existiert und wer nicht mehr?**/
		
		/**
		 * Was ist bis hierhin passiert? Alle Threads haben ihre lokalen Iterationen abgeschlossen, 
		 * Barrier benachrichtigt, und bevors in die nächste iteration geht, soll jetzt:
		 * 
		 * 1a. outflow zwischen den Spalten ausgetauschen: Wie?
		 *	Wir führen neue Klasse grid ein. Siehe Grid. Dann nimmt der Exchanger über die Grid die columns
		 *  vertauscht die flows. Wenn es eine column nicht gibt wird eine neue erzeugt. Dieser Vorgang wird durch 
		 *  implizites Lock geschützt. Sollten zwei Exchanger gleichzeitig auf eine zu erschaffende Column zugreifen 
		 *  wollen und versuchen sie zu erschaffen, dann wartet der eine bis der andere mit erschaffen fertig ist 
		 *  und dann geht es weiter mit set und get.
		 *
		 * 1b. Falls eine Spalte nicht existiert, muss sie erschaffen werden und values werden als leere 
		 * hashtable initialisiert. Dann outflow gesetzt und in run() werden dann ja im nächsten iter schritt,
		 * wird mit run der aktuell value berechnet.
		 * 
		 * 2a. ruft column.computeNewValues() auf;
		 * 
		 * 2b. Falls eine Spalte nich mehr existieren sollte: dann überprüft er (column.getflag()== 0) if true, dann 
		 * den korrespondierenden Eintrag aus columns löschen. SOFERN der neue inflow^1/2 < epsilon
		 * 
		 * 2c. Falls für eine Spalte gilt: diff(oldvalues, newvalues):=akkusum + inflowRight +inflowLeft <= sigma
		 * dann soll dieser Thread in der nächten globalen Iteration nich mehr gestartet werden. 
		 * 			Achtung: Sigma und Epsilon. 
		 * 			Epsilon globale distanz zwischen allen werten in zeitpunkt t und allen in (t+1)
		 * 			Sigma: lokale Distanz zwischen allen Werten einer Spalte.
		 * 
		 * 3. Den Vergleichswert zu Epsilon berechnen. neues Feld double global; 
		 *  global on init: 0
		 *  global in t: die Summe der quadrrate aller differenezn (sum (diff)^2) in t 
		 *  das wird am Ende der globalen Iteration berechnet und dann mit epsilon^2 verglichen.
		 *  falls gloabl kleiner, dann break und fertig, sonst nächste iteration.
		 *  !!!!!!STATT WURZEL ZIEHEN EPSILON QUADRAT!!!!!!!!!!!!!!!!!!!!!!!!
		 *  
		 * 4. neuen sinnvollen Treshold für die Anzahl der maximalen lokalen Variablen mit epsilon abschätzen.
		 * 	was ist sinnvoll? Wir starten mit 100 z.B.
		 *  Für den Angfang : (ceil(gloabl/epsilon)), wobei int bla := Anzahl der lokalten Iterationen. Wird
		 *  mit run(local) übergeben.
		 *  
		 *  Es gilt: je kleiner der Quotient wird, desto geringer soll die Anzahl der lokalen Iterationen sein.
		 *  Wie skaliert man das jetzt sinnvoll? Idee: bla =  (ceil(global/epsilon)^2. D.h je größer der Quotient, 
		 *  desto mehr Iterationen. 
		 *  
		 *  Wir müssen halt mal schauen, was überhaupt sinnvoll ist für Epsilon anzunhmen. bzw. was so für global 
		 *  raus kommt.
		 *  Eine Lösung wäre auch das quadratische Wachstum durch eine Obergrenze zu beschränken.
		 *  
		 * 		 
		 * **/

						
	}

}


