package net.speedstor.websocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.Clock;

import net.speedstor.control.Log;
import net.speedstor.control.Settings;

public class InStream implements Runnable{
	WebSocket websocket;
	Socket client;
	BufferedReader inFromClient;
	BufferedOutputStream outToClient;
	Clock clock;
	Log log;
	String userId;
	String socketId;

	DataInputStream in;
	
	Boolean running = false;
	
	OutStream outStream;
	
	DispatchThread dispatchThread;
	
	
	public InStream(WebSocket websocket, Socket client, BufferedReader inFromClient, BufferedOutputStream outToClient, Clock clock, Log log, OutStream outStream, String userId, String socketId) {
		this.websocket = websocket;
		this.client = client;
		this.inFromClient = inFromClient;
		this.outToClient = outToClient;
		this.clock = clock;
		this.log = log;
		this.userId = userId;
		this.socketId = socketId;
		this.outStream = outStream;
		
		dispatchThread = new DispatchThread(websocket, socketId, this);
		try {
			this.in = new DataInputStream(new BufferedInputStream(client.getInputStream()));
		} catch (IOException e) {
			log.error("Cannot get input stream for socket");
		}
		running = true;
		
	}
	
	String nextSend = null;

	@Override
	public void run() {
		//running = true;
		long bufferTime = 0;
		while(running) {
				try {
					int firstByte;
					if((firstByte = in.read()) != -1) {
						//on message
						String message = readMessage(in, firstByte);
						
						if(message == "error-speedstor") {
							
						}else if(message == "empty-speedstor") {
							
						}else if(message == "pong-speedstor") {
							
						}else {
							//log.log("Socket recieved: " + message);
							
							String command = "";
							if(message.contains(" ")) {
								int indexOfFirstSpace =  message.indexOf(" ");
								command = message.substring(0,indexOfFirstSpace);
								int secondSpace;
								//should only have ping
								switch(command) {
								case "ping":
									
									break;
								case "sync":
									//FORMAT: sync isReply("n")/replyToMessageId theMessage/content
									secondSpace = message.indexOf(" ", indexOfFirstSpace+1);
									if(secondSpace > 0) {										
										String replyTo = message.substring(indexOfFirstSpace+1, secondSpace);
										int replyToLength = replyTo.length();
										if(replyToLength == 1 || replyToLength == 4) {
											//replyTo is either "n" or "0000" (4-digit messageId)
											String content = message.substring(secondSpace+1);
											
											//if the inStream is still dispatching syncs, then just update the nextSend String variable
											 nextSend = "{\"sync\":{"+
												"\"replyTo\": \""+replyTo+"\","+
												"\"senderId\": \""+userId+"\","+
												"\"content\": \""+content.replace("\"", "\\\"")+"\""+
											"}}";
										}
									}
									break;
								case "endsync":
									//FORMAT: endsync isReply("n")/replyToMessageId
									secondSpace = message.indexOf(" ", indexOfFirstSpace+1);
									String replyTo;
									if(secondSpace > -1) {										
										replyTo = message.substring(indexOfFirstSpace+1, secondSpace);
									}else {
										replyTo = message.substring(indexOfFirstSpace+1);
									}
									nextSend = "{\"endsync\":{"+
										"\"replyTo\": \""+replyTo+"\","+
										"\"senderId\": \""+userId+"\""+
									"}}";
									break;
								case "resetsync":
									//FORMAT: resetsync
									websocket.sendSyncJson(socketId, "{\"resetsync\":{"+
										"\"senderId\": \""+userId+"\""+
									"}}");
									break;
								default:
									log.warn("websocket sent unexpected command [\""+message+"\"]");
									break;	
								}
							}
						}
								
						
					}
					if(nextSend != null) {
						if(dispatchThread.isRunning != true) {								
							dispatchThread.setSend(nextSend);
							Thread newThread = new Thread(dispatchThread);
							newThread.start();
						}
						//start sendThread
						//websocket.sendSyncJson(socketId, nextSend);
					}
				} catch(IOException e) {
					log.debug("instream read error !!!!!!!! --- !!! [if happens see if desconnect socket]");
					websocket.disconnectSocket();
				}

		        try {
					Thread.sleep(Settings.LISTEN_SPEED_INTERVAL/10);
				} catch (InterruptedException e) {
					//e.printStackTrace();
					log.error("inStream thread sleep 250ms interrupted");
				}
			
		}
	}
	
	public void stop() {
		running = false;
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
				outStream.sendUnmaskPong(message);
				return "pong-speedstor";
			}else if(opcode == 8){
				//disconnect socket
				websocket.disconnectSocketResponse();
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
	
	
}

class DispatchThread implements Runnable{
	
	public String send;

	WebSocket websocket;
	String socketId;
	InStream inStream;
	
	public boolean isRunning = false;
	
	public DispatchThread(WebSocket websocket, String socketId, InStream inStream){
		this.websocket = websocket;
		this.socketId = socketId;
		this.inStream = inStream;
		send = null;
	}
	
	@Override
	public void run() {
		isRunning = true;
		if(send != null) {
			websocket.sendSyncJson(socketId, send);
		}
		isRunning = false;
		inStream.nextSend = null;
		return;
	}
	
	public void setSend(String sendString) {
		this.send = sendString;
	}

}
