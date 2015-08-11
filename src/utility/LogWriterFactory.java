package utility;

import java.util.concurrent.Executor;

public class LogWriterFactory implements Executor{
	private Logger log; 
	
	public LogWriterFactory(Logger log){
		this.log = log;
	}
	
	public void print(int modus, String spec, String line){ //mode 2!
		LogWriter l = new LogWriter(modus,log, spec,line);
		execute(l);
	}
	
	public void print(int modus, double[] arr){
		LogWriter l = new LogWriter(modus, log, arr);
		execute(l);
	}

	@Override
	public void execute(Runnable logwriter) {
		new Thread(logwriter).start();		
	}
}
