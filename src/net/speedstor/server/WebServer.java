package net.speedstor.server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.time.Clock;
import java.util.Date;
import net.speedstor.control.Log;
import net.speedstor.control.Settings;

public class WebServer implements Runnable{
	Log log;
	public boolean running = false;
	int port;
	
	ServerSocket serverSocket;

	Clock clock;
	double startTime;
	
	public WebServer(Log log, int port, Clock clock, double startTime) {
		this.log = log;
		this.port = port;

		this.clock = clock;
		this.startTime = startTime;
	}
	
	public void run() {
	    try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e1) {
			log.error("(web) port: "+port+" taken, try another one");
			return;
		}
		running = true;
	    log.log("Web server open on port: ("+port+"); started: "+(clock.millis() - startTime)+"ms");


		while(running) {
			//Receive requests
	        try {
	        	Socket client = serverSocket.accept();
	        	
	        	//move to a new thread for dealing with request
	        	(new ConnectionThread(client, log, this, clock)).start();
	        }catch (java.io.InterruptedIOException e ) {
	        	//timeout socket
	        	log.error("Server accept socket timed out");
	        }catch (IOException e) {
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

class ConnectionThread extends Thread{
	Socket client;
	Log log;
	WebServer server;
	Clock clock;
	
	public ConnectionThread(Socket client, Log log, WebServer server, Clock clock) {
		this.client = client;
		this.log = log;
		this.server = server;
		this.clock = clock;
	}
	
	public void run() {
		try {
	        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			PrintWriter headerOut = new PrintWriter(client.getOutputStream());
	        BufferedOutputStream outToClient = new BufferedOutputStream(client.getOutputStream());
	        
	        String clientRequest = inFromClient.readLine();
	        clientRequest = clientRequest.split(" ")[1]; //failing condition handled by exception for simplicity
	        if(clientRequest.equals("/")) clientRequest = "/index.html";
	        if(clientRequest.contains("?")) clientRequest = clientRequest.substring(0, clientRequest.indexOf("?"));
	        
	        File path = new File(Settings.WEB_FOLDER_PATH+clientRequest);
	        
	        if(path.exists()) {	        	
	        	String contentType;
	        	int fileTypeSeperatorLoc = clientRequest.lastIndexOf('.');
	        	String requestFileType;
	        	if(fileTypeSeperatorLoc > 0 && fileTypeSeperatorLoc > clientRequest.lastIndexOf('/'))
	        		requestFileType = clientRequest.substring(fileTypeSeperatorLoc + 1);
	        	else requestFileType = "html";
	        		        	
	        	switch(requestFileType) {
        		default:
        			contentType = "text/plain";
        			break;
	        	case "html":
        			contentType = "text/html";
	        		break;
	        	case "png":
        			contentType = "image/png";
	        		break;
	        	case "gif":
        			contentType = "image/gif";
	        		break;
	        	case "js":
	        		contentType = "text/javascript";
	        		break;
	        	case "css":
	        		contentType = "text/css";
	        		break;
	        	case "ico":
	        		contentType = "image/x-icon";
	        		break;
	        	}
	        	
				headerOut.println("HTTP/1.1 200 OK");
				headerOut.println("Server: Canvas Web Server");
				headerOut.println("Date: " + new Date());
				headerOut.println("Content-type: " + contentType);
				headerOut.println("Content-length: " + path.length());
				headerOut.println("Access-Control-Allow-Origin: *");
				headerOut.println(); // blank line between headers and content, very important !
				headerOut.flush(); // flush character output stream buffer
		        
				outToClient.write(Files.readAllBytes(path.toPath()), 0, (int)path.length());
				outToClient.flush();
	        }else {
	        	byte[] returnResponse = "404. File Not Found, Sorry :(".getBytes();
	        	headerOut.println("HTTP/1.1 404 Not Found");
				headerOut.println("Server: Canvas Web Server");
				headerOut.println("Date: " + new Date());
				headerOut.println("Content-length: " + returnResponse.length);
				headerOut.println(); // blank line between headers and content, very important !
				headerOut.flush();
				
				outToClient.write(returnResponse, 0, returnResponse.length);
				outToClient.flush();
	        }
	        
			client.close();
		}catch(Exception e) {
			log.error("exception error at web server");
		}
	}
}
