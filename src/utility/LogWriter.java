package utility;

/**
 * Der Konstruktor von Logwriter ist überladen, um verschiedene formate zu realisieren. Welches Format geloggt werden 
 * soll, wird vom Aufrufer festgelegt, dazu muss mode gesetzt werden. 
 * Mode 1 ist zur Zeit, dass ein double array in einen String verwandelt und dem Logger übergeben wird. 
 * Mode 2 ist zur Zeit, dass einige wenige, spezifizierte Zeilen geloggt werden. 
 * run() schaut, welcher Modus es sein soll, baut String falls nötig und übergibt sie dann dem Logger. 
 * **/

public class LogWriter implements Runnable{
	private int mode; 	//determines which printmode shall be used.
	private double[] arr;
	private String[] line = new String[2];
	private Logger log;
	private static int counter;
	
	public LogWriter(int mode, Logger log, double[] arr){
		this.mode = mode;
		this.arr = arr;
		this.log = log;
	}
	
	public LogWriter(int mode, Logger log, String details, String line){
		this.mode = mode;
		this.line[0] = details;
		this.line[1] = line;
		this.log = log;
	}
	/**im konstruktor wird einem Thread der Klasse logwriter gesagt, was geloggt werden soll**/
	@Override
	public void run() {
		switch(mode){ 
			case 1: log.addLines(counter, makeLines());
			
			case 2: log.addSingleLine(line[0], line[1]);
		}
	}
	
	
	/**baut eine String aus allen double werten die er hat, um sie dann dem logger uebergeben zu koennen. **/
	private String makeLines(){
		String ret = "";
		int i = 0;
		for (double dummy: arr){
			ret = ret + "val: " + i + ": " + dummy + "\n";
			i++;
		}
		return ret;
	}

}
