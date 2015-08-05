package utility;

import java.util.LinkedList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;


/**
 * Utility Class for testing purposes. Allows to print a detailed Logs in .txt files to give a overlook about 
 * current results and computations. Currently only provides some core functionalites by implementing the out-
 * putStream to file. The exact output formatation shall be specified during the project. 
 * 
 * public methods: 
 * 
 * makeString: 
 * 
 * @args: array of double, int to allow us to match the output with certain phases of our program.
 * 
 * addSingleLine:
 * 
 * @args: String spec: more detailed meta information about the following the output. 
 * 		  String line: the said output. 
 * 
 * Idee: instead of System.out.printlns to console, we create a log file. I guess this will be a more clean method.
 * 
 * **/

public class Logger {
	private LinkedList<String> output = new LinkedList<String>();
	private static int id; 
	private File file;
	private boolean next = true;
	private final String nl = "\n";
	private final String onEnter = "//////////////////////NEW NEW NEW NEW NEW NEW NEW\\\\\\\\\\\\\\\\\\\\\\\\\\\\" + nl;
	private final String onLeave = "//////////////////////END END END END END END END\\\\\\\\\\\\\\\\\\\\\\\\\\\\" + nl; 
	private final String home= "../NpProjekt/main";

	public Logger(){
		String name = "/Logfile_No." + id + ".log.txt";
		this.file = new  File(home + name);
		Logger.id++;
		try{
			file.createNewFile();
			System.out.println("LogFile is da");
		}catch(Exception e){
			System.out.println("Logfile wurde nich gebaut.");
		}

	}
	
	synchronized public void addSingleLine(String spec, String line){
		try{
			while(!next)
				wait();
			next = false;	
			output.add(onEnter);
			output.add("Output at pos: " + spec + " is: \n");
			output.add(line + "\n");
			output.add(onLeave);
			next = true;
			notifyAll();
		}catch (Exception e){
			System.out.println("Logger kaputt >(");
		}
	}
	
	synchronized public void makeString(int run, double[] arr){
		try{
			while(!next)
				wait();
			next = false;
			output.add(onEnter);
			output.add("Run: " + run + " begins.\n");
			output.add("NEW VALUES\n");
			int i = 0;
			for (double dummy: arr){
				output.add("val: " + i + ": " + dummy + "\n");
				i++;
			}
			output.add("Run: " + run + "ends.\n");
			output.add(onLeave);
			next = true;
			notifyAll();
		}catch (Exception e){
			System.out.println("Logger kaputt :(");
		}
	}
	
	public void print(){
		FileOutputStream out = null;
		
		try {
			out = new FileOutputStream(file);
			
		/**nimmt sich die linkedlist und packt alles in einen einzelnen String. Macht dann aus dem String einen
		 * Byte array und schmeißt diesen in den output stream. Der schreibt den String ins LogFile.**/
			String content ="";
			for (String dummy: output)
				content = content +  dummy;
			byte[] arr = content.getBytes();
		
			out.write(arr);
			out.flush();
			System.out.println("log wurde geschrieben.");
		} catch (IOException e) {
			System.out.println("hmm probleme beim Schreiben des Logs.");
		}finally{
			try {
				if(out != null)
					out.close();
			} catch (IOException e) {
				System.out.println("hmm problem mit dem output stream.");
			}
		}
	}
	
}
