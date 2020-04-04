package net.speedstor.websocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.Clock;

import net.speedstor.main.Log;

public class InStream implements Runnable{
	WebSocket websocket;
	Socket client;
	BufferedReader inFromClient;
	BufferedOutputStream outToClient;
	Clock clock;
	Log log;

	DataInputStream in;
	
	Boolean running = false;
	
	
	public InStream(WebSocket websocket, Socket client, BufferedReader inFromClient, BufferedOutputStream outToClient, Clock clock, Log log) {
		this.websocket = websocket;
		this.client = client;
		this.inFromClient = inFromClient;
		this.outToClient = outToClient;
		this.clock = clock;
		this.log = log;
		try {
			this.in = new DataInputStream(new BufferedInputStream(client.getInputStream()));
		} catch (IOException e) {
			log.error("Cannot get input stream for socket");
		}
		running = true;
		
	}

	@Override
	public void run() {

		//running = true;
		long bufferTime = 0;
		while(running) {
			if(clock.millis() - bufferTime > 1000) {
				bufferTime = clock.millis();
				try {
					int firstByte;
					if((firstByte = in.read()) != -1) {
						//on message
						String message = websocket.readMessage(in, firstByte);
						
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
								case "post":
									websocket.postTopicMessage(message.substring(indexOfFirstSpace+1));
									break;
								case "reply":
									secondSpace = message.indexOf(" ", indexOfFirstSpace);
									String targetTopic = message.substring(indexOfFirstSpace+1, secondSpace);
									websocket.replyToTopic(targetTopic, message.substring(secondSpace));
									break;
								case "sync":
									//"sync username the message"
									secondSpace = message.indexOf(" ", indexOfFirstSpace+1);
									String username = message.substring(indexOfFirstSpace+1, secondSpace);
									websocket.sendSyncUpdate(username, message.substring(secondSpace+1));
									break;
								}
							}
						}
						
					}
				} catch(IOException e) {
					
				}
			}
		}
		
	}
}
