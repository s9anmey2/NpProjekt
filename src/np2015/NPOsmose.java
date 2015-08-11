package np2015;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import sequentiell.*;
//import utility.Logger;

import com.google.gson.Gson;

public class NPOsmose {

	public static void main(String[] args) throws IOException, InterruptedException {
	/*	Logger logger = new Logger();
		logger.addSingleLine("halalalal", "whatever");
		logger.print();*/
	/**/Gson gson = new Gson();
		String json = "";
		// read data in
		if (args.length != 0) {
			Path path = Paths.get(args[1]);
			try {
				json = new String(Files.readAllBytes(path)); 
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("You must provide the serialized file as the first argument!");
		}
		GraphInfo ginfo = gson.fromJson(json, GraphInfo.class);
		// Your implementation can now access ginfo to read out all important values
		ImageConvertible graph = new Sequentiell(ginfo).compute(); //new Grid(ginfo); <--- you should implement ImageConvertible to write the graph out
		ginfo.write2File("./result.txt", graph);/**/
	} 

}
