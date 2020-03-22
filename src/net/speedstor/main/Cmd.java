package net.speedstor.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Arrays;

public class Cmd implements Runnable{
	BufferedReader in;
	Main main;
	Log log;
	Server Server;
	WebSocketHandler websocketHandler;

	public Cmd(Main main, Log log, Server server, WebSocketHandler websocketHandler) {
		in = new BufferedReader(new InputStreamReader(System.in));
		this.main = main;
		this.websocketHandler = websocketHandler;
	}
	
	public void run() {
		outerloop:
		while(true) {
			try {
				if(in.ready()) {
					String[] commandLine = in.readLine().split("\\s+");
					switch(commandLine[0]) {
					case "":
						break;
					case "sendAllMsg":
						if(commandLine.length > 1) {
							websocketHandler.sendToAll(commandLine[1]);
						}else {
							log.error("need a second parameter for send command");
						}
						break;
					case "sendAllBinary":
						if(commandLine.length > 1) {
							websocketHandler.sendToAllBinary(commandLine[1]);
						}else {
							log.error("need a second parameter for send command");
						}
						break;
					case "sendDefault":
						websocketHandler.sendAllDefault();
						break;
					case "testCodeBlock":
						testCodeBlock();
						break;
					case "stop":
					case "quit":
					case "exit":
						break outerloop;
					}
					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		main.stop();
	}
	
	public void testCodeBlock() {
		
	}
	

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}

}
