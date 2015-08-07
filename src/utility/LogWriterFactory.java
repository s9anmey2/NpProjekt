package utility;

public class LogWriterFactory {
	private Logger log; 
	
	public LogWriterFactory(Logger log){
		this.log = log;
	}
	
	public void print(int modus, String spec, String line){ //mode 2!
		LogWriter l = new LogWriter(modus,log, spec,line);
		Thread lw = new Thread(l);
		lw.start();
	}
	
	public void print(int modus, double[] arr){
		LogWriter l = new LogWriter(modus, log, arr);
		Thread lw = new Thread(l);
		lw.start();
	}
}
