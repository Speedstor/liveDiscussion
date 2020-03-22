package net.speedstor.main;

import java.time.Clock;
import java.time.format.DateTimeFormatter;

public class Log {
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
	
	Clock clock;
	public Log(Clock clock) {
		this.clock = clock;
	}

	public void log(String message) {
		print("(1)LOG: " + message);
	}
	
	public void error(String message) {
		print("(-1)ERROR: "+message);
	}
	
	public void warn(String message) {
		print("(0)WARN: "+message);
	}
	
	public void special(String message) {
		print("(444)SPECIAL: " + message);
	}
	
	public void logStop(String message) {
		print("(3)STOP: " + message);
	}
	
	public int programStop() {
		print("------end------");
		return 1;
	}
	
	private void print(String output) {
		System.out.println(clock.instant().toString().replace("T", " ").substring(0, 19) + "  " + output);
	}

	
}
