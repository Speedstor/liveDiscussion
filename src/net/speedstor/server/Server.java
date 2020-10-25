package net.speedstor.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Clock;
import net.speedstor.control.Log;
import net.speedstor.discussion.DiscussionHandler;
import net.speedstor.main.Cache;
import net.speedstor.main.TokenHandler;
import net.speedstor.websocket.WebSocketHandler;

public class Server implements Runnable{
	Log log;
	Cache cache;
	public boolean running = false;

	ServerSocket serverSocket;
	TokenHandler tokenHandler;
	DiscussionHandler discussionHandler;
	int port;
	
	WebSocketHandler websocketHandler;
	
	Clock clock;
	double startTime;
	
	public Server(Log log, Clock clock, int port, double startTime, WebSocketHandler websocketHandler, TokenHandler tokenHandler, DiscussionHandler discussionHandler, Cache cache) {
		this.log = log;
		this.port = port;
		this.clock = clock;
		this.startTime = startTime;
		this.tokenHandler = tokenHandler;
		this.websocketHandler = websocketHandler;
		this.discussionHandler = discussionHandler;
		this.cache = cache;
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
	        	Socket client = serverSocket.accept();
	        	
	        	//move to a new thread for dealing with request
	        	Thread serverThread = new ServerThread(client, log, this, websocketHandler, clock, tokenHandler, discussionHandler, cache);
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
