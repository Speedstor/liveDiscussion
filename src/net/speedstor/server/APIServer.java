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

public class APIServer implements Runnable{
	Log log;
	Cache cache;
	public boolean running = false;

	ServerSocket serverSocket;
	TokenHandler tokenHandler;
	DiscussionHandler discussionHandler;
	APIFunctions apiFunctions;
	int port;
	
	WebSocketHandler websocketHandler;
	
	Clock clock;
	double startTime;
	
	public APIServer(Log log, Clock clock, int port, double startTime, WebSocketHandler websocketHandler, TokenHandler tokenHandler, DiscussionHandler discussionHandler, Cache cache) {
		this.log = log;
		this.port = port;
		this.clock = clock;
		this.startTime = startTime;
		this.tokenHandler = tokenHandler;
		this.websocketHandler = websocketHandler;
		this.discussionHandler = discussionHandler;
		this.cache = cache;
		
		apiFunctions = new APIFunctions(log, this, websocketHandler, tokenHandler, discussionHandler, clock, cache);
	}
	
	public void run() {
	    try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e1) {
			log.error("(api) port: "+port+" taken, try another one");
			return;
		}
		running = true;
	    log.log("Api server open on port: ("+port+"); started: "+(clock.millis() - startTime)+"ms");

		while(running) {
			//Receive requests
	        try {
	        	Socket client = serverSocket.accept();
	        	
	        	//move to a new thread for dealing with request
	        	Thread apiServerThread = new APIServerThread(client, log, this, websocketHandler, clock, tokenHandler, discussionHandler, apiFunctions, cache);
	        	apiServerThread.start();
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
