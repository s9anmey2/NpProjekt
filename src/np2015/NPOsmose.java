package np2015;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import implementation.Supervisor;
//import src.sequentiell.*;

import com.google.gson.Gson;

public class NPOsmose {

	public static void main(String[] args) throws IOException, InterruptedException {

		
		Gson gson = new Gson();
		String json = "";
		// read data in
		if (args.length != 0) {
			Path path = Paths.get(args[0]);
			try {
				json = new String(Files.readAllBytes(path)); 
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("You must provide the serialized file as the first argument!");
		}
		GraphInfo ginfo = gson.fromJson(json, GraphInfo.class);
		 //Your implementation can now access ginfo to read out all important values
		
		System.out.println("Start: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(new java.util.Date())); 
		ImageConvertible graph = new Supervisor(ginfo).computeOsmose(); // <--- you should implement ImageConvertible to write the graph out
		System.out.println("Ende: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(new java.util.Date())); 

		ginfo.write2File("./result.txt", graph);
	} 
	
}
