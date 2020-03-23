package net.speedstor.main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;

public class Server implements Runnable{
	Log log;
	public boolean running = false;

	ServerSocket serverSocket;
	TokenHandler tokenHandler;
	int port;
	
	WebSocketHandler websocketHandler;
	
	Clock clock;
	double startTime;
	
	public HashMap<String, DiscussionHandler> runningDiscussionBoards = new HashMap<String, DiscussionHandler>();
	
	public HashMap<String, Integer> clientConnections = new HashMap<String, Integer>();
	
	public Server(Log log, Clock clock, int port, double startTime, WebSocketHandler websocketHandler, TokenHandler tokenHandler) {
		this.log = log;
		this.port = port;
		this.clock = clock;
		this.startTime = startTime;
		this.tokenHandler = tokenHandler;
		this.websocketHandler = websocketHandler;
		running = true;
	}
	
	public void run() {
	    try {
			serverSocket = new ServerSocket(port);
			log.log("Open on port: ("+port+")");
		} catch (IOException e1) {
			log.error("port: "+port+" taken, try another one");
			
			return;
			//debug
			//e1.printStackTrace();
		}
	    log.log("Server started: "+(clock.millis() - startTime)+"ms");

	    
		while(running) {
			//Receive requests
	        try {
	        	//System.out.println("setted up port");

	        	Socket client = serverSocket.accept();
	        	
	        	//move to a new thread for dealing with request
	        	Thread serverThread = new ServerThread(client, log, this, websocketHandler, clock, tokenHandler);
	        	serverThread.start();
	        	
	        }catch ( java.io.InterruptedIOException e ) {
	        	//timeout socket
	        	log.error("Server accept socket timed out");
	        } catch (IOException e) {
	        	log.error("Error with receiving requests");
	        	e.printStackTrace();
			}
		}
	}
	
	public boolean ifRunning() {
		return running;
	}
	public int setRunning(Boolean value) {
		running = value;
		return 1;
	}

}
