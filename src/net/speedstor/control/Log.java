package net.speedstor.control;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Clock;
import java.time.format.DateTimeFormatter;

public class Log {
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
	
	Clock clock;
	public Log(Clock clock) {
		this.clock = clock;
	}

	public void log(String message) {
		print("(1)LOG: "+ message);
	}
	
	public void list(String message) {
		print("(--)LIST: "+ message);
	}
	
	public void response(String message) {
		print("(2)RESPONSE: "+ message);
	}
	
	public void error(String message) {
		String className = Thread.currentThread().getStackTrace()[2].getClassName();
		print("(-1)ERROR: @"+className.substring(className.lastIndexOf(".") + 1)+"-"+Thread.currentThread().getStackTrace()[2].getMethodName()+"(): " + message);
	}
	
	public void warn(String message) {
		String className = Thread.currentThread().getStackTrace()[2].getClassName();
		print("(0)WARN: @"+className.substring(className.lastIndexOf(".") + 1)+"-"+Thread.currentThread().getStackTrace()[2].getMethodName()+"(): " + message);
	}

	public void special(String message) {
		String className = Thread.currentThread().getStackTrace()[2].getClassName();
		print("(444)SPECIAL: @"+className.substring(className.lastIndexOf(".") + 1)+"-"+Thread.currentThread().getStackTrace()[2].getMethodName()+"(): " + message);
	}
	
	public void debug(String message) {
		String className = Thread.currentThread().getStackTrace()[2].getClassName();
		Thread.currentThread().getStackTrace()[2].getLineNumber();
		print("(3)DEBUG: @"+className.substring(className.lastIndexOf(".") + 1)+"-"+Thread.currentThread().getStackTrace()[2].getMethodName()+"()["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"]: " + message);
	}
	
	public void logStop(String message) {
		print("(4)STOP: " + message);
	}
	
	public int programStop() {
		print("------end------");
		return 1;
	}
	
	private void print(String output) {
		String print = clock.instant().toString().replace("T", " ").substring(0, 19) + "  " + output;
		System.out.println(print);
		
		FileWriter fr;
		try {
			fr = new FileWriter(new File(Settings.DOCS_LOC, "log_"+clock.instant().toString().substring(0, 10))+".txt", true);
			fr.write("\n"+print);
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}


	
}
