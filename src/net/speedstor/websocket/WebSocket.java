package net.speedstor.websocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.speedstor.main.DiscussionHandler;
import net.speedstor.main.Log;
import net.speedstor.main.Server;
import net.speedstor.main.TokenHandler;

public class WebSocket{
	
	Socket client;
	BufferedReader inFromClient;
	BufferedOutputStream outToClient;
	Log log;
	TokenHandler tokenHandler;
	Clock clock;
	private Boolean running;
	String socketId;
	private String discussionUrl;
	Server server;
	
	//JSON parsing
    JSONParser parser = new JSONParser();
	
	InStream inStream;
	OutStream outStream;

	public WebSocket(Log log, Clock clock, String socketId, String discussionUrl, Server server, TokenHandler tokenHandler) {
		this.log = log;
		this.clock = clock;
		this.socketId = socketId;
		this.discussionUrl = discussionUrl;
		this.server = server;
		this.tokenHandler = tokenHandler;
	}
	
	public void initSetup(Socket client, BufferedReader inFromClient, BufferedOutputStream outToClient) {
		this.client = client;
		this.inFromClient = inFromClient;
		this.outToClient = outToClient;
		
		inStream = new InStream(this, client, inFromClient, outToClient, clock, log);
		Thread inStreamThread = new Thread(inStream);
		inStreamThread.start();
		
		outStream = new OutStream(this, client, inFromClient, outToClient, clock, log);
		Thread outStreamThread = new Thread(outStream);
		outStreamThread.start();
	}
	
	void sendSyncUpdate(String username, String content) {
		DiscussionHandler discussionBoard = server.runningDiscussionBoards.get(discussionUrl);
		//while(discussionBoard.inUpdating);
		discussionBoard.sendSync(username, content);
	}

	void postTopicMessage(String content) {
		DiscussionHandler discussionBoard = server.runningDiscussionBoards.get(discussionUrl);
		String response = sendPost(discussionUrl.replace("view", "")+"entries"+"?access_token="+tokenHandler.get(socketId), "{\"message\": \""+content+"\"}");
		
		discussionBoard.sendNewTopic(response);
	}
	
	void replyToTopic(String targetTopic, String content) {
		DiscussionHandler discussionBoard = server.runningDiscussionBoards.get(discussionUrl);
		String response = sendPost(discussionUrl.replace("view", "")+"entries/"+targetTopic+"/replies?access_token="+tokenHandler.get(socketId), "{\"message\": \""+content+"\"}");

		//discussionBoard
		discussionBoard.sendNewReply(targetTopic, response);
	}
	
	String readMessage(DataInputStream in, int firstByte) {
		try {
			String frameHeaderBin = Integer.toBinaryString(firstByte);
			frameHeaderBin += Integer.toBinaryString(in.read()); //2 bytes - frame header
			
			boolean finBit = frameHeaderBin.substring(0, 1) == "1" ? false: true;
			if(frameHeaderBin.substring(1, 4).contains("1")) log.warn("one of the rsv bits in websocket recieved packages is 1");
			int opcode = Integer.parseInt(frameHeaderBin.substring(4, 8), 2);
						
			boolean mask = frameHeaderBin.substring(8, 9) == "1" ? false: true;
			if(!mask) log.warn("client package is not masked");
			
			int payloadLength = Integer.parseInt(frameHeaderBin.substring(9, 16), 2);
			if(payloadLength <= 125) {
				
			}else if(payloadLength == 126) {
				String payloadBit = Integer.toBinaryString(in.read());
				payloadBit += String.format("%8s", Integer.toBinaryString(in.read())).replace(' ', '0');; // 2 bytes - 16 bits
				
				payloadLength = Integer.parseInt(payloadBit, 2);
			}else if(payloadLength == 127){
				String payload64Bit = Integer.toBinaryString(in.read());
				payload64Bit += String.format("%8s", Integer.toBinaryString(in.read())).replace(' ', '0');
				payload64Bit += String.format("%8s", Integer.toBinaryString(in.read())).replace(' ', '0');
				payload64Bit += String.format("%8s", Integer.toBinaryString(in.read())).replace(' ', '0');
				payload64Bit += String.format("%8s", Integer.toBinaryString(in.read())).replace(' ', '0');
				payload64Bit += String.format("%8s", Integer.toBinaryString(in.read())).replace(' ', '0');
				payload64Bit += String.format("%8s", Integer.toBinaryString(in.read())).replace(' ', '0');
				payload64Bit += String.format("%8s", Integer.toBinaryString(in.read())).replace(' ', '0'); // 8 bytes - 64 bits
				
				payloadLength = Integer.parseInt(payload64Bit, 2);
			}else {
				log.warn("websocket payload length is more than 64 bit");
			}
			
			byte[] maskKey = new byte[4];
			if(in.read(maskKey, 0, 4) == -1) {
				log.error("retrieving mask key error");
			}
						
			byte[] encodedPayload = new byte[payloadLength];
			if(in.read(encodedPayload, 0, payloadLength) == -1) {
				log.error("error retrieving encoded payload data on websocket");
			}
			
			//decoding
			String message = decode(encodedPayload, maskKey);
			

			if(opcode == 9) {
				//ping, send pong
				sendUnmaskPong(message);
				return "pong-speedstor";
			}else if(opcode == 8){
				//disconnect socket
				disconnectSocket();
			}else if(opcode == 10) {
				//pong frame
			}else if(opcode != 0 && opcode != 1 && opcode != 2) {
				//not a standard frame, ignore
				return "empty-speedstor";
			}
			

			return message;
		}catch(IOException e) {
			log.error("Websocket listening for message error");
		}
		
		return "error-speedstor";
	}
	
	public void sendUnmaskThread(String message) {
		//want to offload to thread 
		outStream.send(message);
	}
	
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
	
	public int disconnectSocket() {
		running = false;
		sendCloseFrame();
		return 1;
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
	
	public String getUrl() {
		return discussionUrl;
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
	
	//https://stackoverflow.com/questions/5683486/how-to-combine-two-byte-arrays
	public static byte[] addAll(final byte[] array1, byte[] array2) {
	    byte[] joinedArray = Arrays.copyOf(array1, array1.length + array2.length);
	    System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
	    return joinedArray;
	}
	
	private String decode(byte[] encoded, byte[] key) {
		
		byte[] decoded = new byte[encoded.length];
		//example - byte[] encoded = new byte[] { (byte) 198, (byte) 131, (byte) 130, (byte) 182, (byte) 194, (byte) 135 };
		//example - byte[] key = new byte[] { (byte) 167, (byte) 225, (byte) 225, (byte) 210 };
		for (int i = 0; i < encoded.length; i++) {
			decoded[i] = (byte) (encoded[i] ^ key[i & 0x3]);
		}
		
		String finalResponse = new String(decoded);
		return finalResponse;
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
	

	public String sendPost(String url, String bodyJson) {
		try {
			URL urlObj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json; utf-8");
			con.setRequestProperty("Accept", "application/json");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			
			con.setDoOutput(true);
			
			try(OutputStream os = con.getOutputStream()) {
			    byte[] input = bodyJson.getBytes("utf-8");
			    os.write(input, 0, input.length);           
			}
			
			int responseCode = con.getResponseCode();
			//if (responseCode == HttpURLConnection.HTTP_OK) {
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
	
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
	
				// print result
				return response.toString();
			//} else {
			//	log.error("GET request not worked");
			//	return "error: get response code error";
			//}
		}catch(IOException e){
			//e.printStackTrace();
			return "error: catch error";
		}
	}

}
