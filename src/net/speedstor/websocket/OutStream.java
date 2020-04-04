package net.speedstor.websocket;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;

import net.speedstor.main.Log;

public class OutStream implements Runnable{
	WebSocket websocket;
	Socket client;
	BufferedReader inFromClient;
	BufferedOutputStream outToClient;
	Clock clock;
	Log log;

	DataInputStream in;
	
	Boolean running = false;
	
	
	public OutStream(WebSocket websocket, Socket client, BufferedReader inFromClient, BufferedOutputStream outToClient, Clock clock, Log log) {
		this.websocket = websocket;
		this.client = client;
		this.inFromClient = inFromClient;
		this.outToClient = outToClient;
		this.clock = clock;
		this.log = log;
		running = true;
		
	}
	
	ArrayList<String> messageList = new ArrayList<String>();
	
	@Override
	public void run() {
		while(running) {

			long bufferTime = 0;
			if(clock.millis() - bufferTime > 1000) {
				//log.log("-");
				while(messageList.size() > 0) {
					sendUnmask(messageList.get(0));
					messageList.remove(0);
				}
				bufferTime = clock.millis();
			}

	        if (Thread.interrupted()) {
	        	log.warn("interrupted");
	            return;
	        }
		}		
	}
	
	//send means to add to list to be sorted through
	void send(String payload) {
		messageList.add(payload);
	}
	
	//sendUnmask acutally sends it to client
	public void sendUnmask(String message) {
		String binaryInit = "10000001 0";
		
		/* <125 - 
		 * 126  - 16bits - 65,535
		 * 127  - 64bits - 9,223,372,036,854,775,807
		 */
		String payloadLenBin;
		if(message.length() <= 125) {
			String lengthBinary = Integer.toBinaryString(message.length());
			String fillerString = "";
			for(int i = 0; i < 7 - lengthBinary.length(); i++) fillerString += "0";
			payloadLenBin = fillerString + lengthBinary;			
		}else if(message.length() <= 65535) {
			String lengthBinary = Integer.toBinaryString(message.length());
			String fillerString = "";
			for(int i = 0; i < 16 - lengthBinary.length(); i++) fillerString += "0";
			payloadLenBin = "1111110" + fillerString + lengthBinary;
		}else {
			String lengthBinary = Integer.toBinaryString(message.length());
			String fillerString = "";
			for(int i = 0; i < 64 - lengthBinary.length(); i++) fillerString += "0";
			payloadLenBin = "1111111" + fillerString + lengthBinary;
		}
		
		byte[] payload;
		try {
			payload = message.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			log.error("parsing message to binary error");
			payload = new byte[] { (byte) 0x00};
		}
		
		byte[] returnByte = addAll(binaryToByte(binaryInit + payloadLenBin), payload);
		
		try {
			outToClient.write(returnByte, 0, returnByte.length);
			outToClient.flush();
		} catch (IOException e) {
			log.error("Error sending bytes on websocket");
		}
	}
	

	private byte[] binaryToByte(String binaryString) {
		binaryString = binaryString.replaceAll(" ", "");
		String[] binaryStringArray = binaryString.split(String.format("(?<=\\G.{%1$d})", 8));
		byte[] returnByte = new byte[binaryStringArray.length];

		for(int i = 0; i < binaryStringArray.length; i++) {
			String byteString = binaryStringArray[i];
			
			if(byteString.length() == 8) {
				returnByte[i] = (byte) (Integer.parseInt(byteString, 2) & 0xff);
			}else {
				//not a 8 number binary, lol
			}
		}
		
		return returnByte;
	}
	

	//https://stackoverflow.com/questions/5683486/how-to-combine-two-byte-arrays
	public static byte[] addAll(final byte[] array1, byte[] array2) {
	    byte[] joinedArray = Arrays.copyOf(array1, array1.length + array2.length);
	    System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
	    return joinedArray;
	}
	
	
}
