package lpool.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Logger {
	
	public static final String default_path = "./data/logs/";
	public static final String log_extension = ".log";
	public static final String datetime_header = "Timestamp";
	public static final String event_header = "Event";
	public static final String separator = "  ||  ";
	private static final String initialized_event = "Log initialized";
	private static final String log_print_start = "Previous events on current date:";
	
	private String logFileName;
	private Boolean validFile = true;
	private File logfile;
	
	public Logger() {
		String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		logFileName = default_path + date + log_extension;
		validFile = initialize();
		if(!validFile) {
			System.err.println("Unable to create log file");
		}
	}

	public Boolean initialize() {
		
		File theDir = new File(default_path);

		if (!theDir.exists()) {
		    System.out.println("creating directory: " + default_path);
		    boolean result = false;

		    try{
		        theDir.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se){
		        //handle it
		    }        
		    if(result) {    
		        System.out.println("DIR created");  
		    } else {
		    	return false;
		    }
		}
		
		logfile = new File(logFileName);
		
		if(!logfile.exists()) {
			try {
				if(!logfile.createNewFile())
					return false;
			} catch(IOException e) {
				e.printStackTrace();
				return false;
			}
			
			if(!insertHeader(logfile))
				return false;
		}
		
		return true;
	}
	
	private Boolean insertHeader(File logfile) {

		try {
		    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)));
		    out.println("Log format: " + datetime_header + separator + event_header);
		    out.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
		log(initialized_event);
		
		return true;
	}
	
	public void log(String event) {
		if(!validFile)
			return;
		
		try {
		    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)));
		    String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS").format(new Date());
		    out.println(date + separator + event);
		    out.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
	
	public String toString() {
		String result = "";
		
		try {
			Scanner scanner = new Scanner(logfile);
			if(scanner.hasNextLine())
				scanner.nextLine();
			
			if(scanner.hasNextLine()) {
				result += log_print_start + '\n';
			}
			
			while (scanner.hasNextLine()) {
				result += scanner.nextLine() + '\n';
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			   e.printStackTrace();
		}
		
		return result;
	}
	
	public void print() {
		System.out.println(toString());
	}
	
}
