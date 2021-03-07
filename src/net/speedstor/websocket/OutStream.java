package net.speedstor.websocket;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;

import net.speedstor.control.Log;

public class OutStream{
	WebSocket websocket;
	Socket client;
	BufferedReader inFromClient;
	BufferedOutputStream outToClient;
	Clock clock;
	Log log;

	DataInputStream in;
	
	private Boolean inSending = false;
	
	public OutStream(WebSocket websocket, Socket client, BufferedReader inFromClient, BufferedOutputStream outToClient, Clock clock, Log log) {
		this.websocket = websocket;
		this.client = client;
		this.inFromClient = inFromClient;
		this.outToClient = outToClient;
		this.clock = clock;
		this.log = log;
		
	}
	
	ArrayList<String> messageList = new ArrayList<String>();
	
	//send means to add to list to be sorted through
	void send(String payload) {
		/*while(inSending) {
	        try {
				Thread.sleep(Settings.SEND_SPEED_INTERVAL);
			} catch (InterruptedException e) {
				//e.printStackTrace();
				log.error("outStream thread sleep 500ms interrupted");
			}
		}*/
		//TODO: skip to the end if it is the same message and command, endsync would override
		sendUnmask(payload);
	}
	
	//sendUnmask acutally sends it to client
	public void sendUnmask(String message) {
		inSending = true;
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
		inSending = false;
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
	
	public void sendCloseFrame() {
		String binaryInit = "1000 0101 0000 0000";
		byte[] returnByte = binaryToByte(binaryInit);
		try {
			outToClient.write(returnByte, 0, returnByte.length);
			outToClient.flush();
		} catch (IOException e) {
			log.error("Error sending close frame in bytes on websocket");
		}
	}
	
	public void sendDefault() {
		try {
			String binaryString = "10000001 00000101 01001000 01100101 01101100 01101100 01101111";
			//unmasked hello
	
	        byte[] returnByte = binaryToByte(binaryString);
			
			outToClient.write(returnByte, 0, returnByte.length);
			outToClient.flush();
		}catch(IOException e) {
			log.error("error sending data frame in web socket");
		}
	}

	public void sendBinary(String binaryString) {
		try {
			binaryString = binaryString.replaceAll(" ", "");
	        byte[] returnByte =  new BigInteger(binaryString, 2).toByteArray();
	
	        log.special(Arrays.toString(returnByte));
	        
			outToClient.write(returnByte, 0, returnByte.length);
			outToClient.flush();
		}catch (IOException e) {
			log.error("error with sending custom binary data frame for web socket");
		}
	}
	
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    String returnString = new String(hexChars);

		String[] hexStringArray = returnString.split(String.format("(?<=\\G.{%1$d})", 2));
		
		returnString = "";
		for(int i = 0; i < hexStringArray.length; i++) {
			returnString += " 0x" + hexStringArray[i];
		}
		return returnString.substring(1);
	}
	

	public void sendUnmaskPong(String message) {
		String binaryInit = "10001010 0";
		
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
	
		log.special(bytesToHex(returnByte));
		
		try {
			outToClient.write(returnByte, 0, returnByte.length);
			outToClient.flush();
		} catch (IOException e) {
			log.error("Error sending bytes on websocket");
		}
	}
	
	
	
}
