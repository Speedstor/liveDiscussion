package net.speedstor.server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import org.json.simple.parser.JSONParser;
import net.speedstor.control.Log;
import net.speedstor.discussion.DiscussionHandler;
import net.speedstor.main.Cache;
import net.speedstor.main.TokenHandler;
import net.speedstor.server.APIServer;
import net.speedstor.websocket.WebSocket;
import net.speedstor.websocket.WebSocketHandler;

public class APIServerThread extends Thread{
	Socket client;
	Log log;
	Cache cache;
	APIServer apiServer;
	WebSocketHandler websocketHandler;
	TokenHandler tokenHandler;
	DiscussionHandler discussionHandler;
	APIFunctions apiFunctions;
	Clock clock;
	
	//token for communication between server and client, not using canvas token
	final static int TOKEN_LENGTH = 15;
	
	JSONParser parser = new JSONParser();
	
	public APIServerThread(Socket client, Log log, APIServer apiServer, WebSocketHandler websocketHandler, Clock clock, TokenHandler tokenHandler, DiscussionHandler discussionHandler, APIFunctions apiFunctions, Cache cache) {
		this.client = client;
		this.log = log;
		this.apiServer = apiServer;
		this.websocketHandler = websocketHandler;
		this.tokenHandler = tokenHandler;
		this.discussionHandler = discussionHandler;
		this.clock = clock;
		this.cache = cache;
		
		this.apiFunctions = apiFunctions;
	}
	
	public void run() {
		try {
	        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			PrintWriter headerOut = new PrintWriter(client.getOutputStream());
	        BufferedOutputStream outToClient = new BufferedOutputStream(client.getOutputStream());
	        
	        String clientRequest = inFromClient.readLine();
	        
	        HashMap<String, String> requestHeader = new HashMap<String, String>();
	        String requestHeaderString = "";       
	        
	        requestHeader.put("loc", clientRequest);
	        
	        String[] request;
	        
	        if(clientRequest != null) {
	        	log.log("Request: "+client+"; Path: "+clientRequest);
	        	request = clientRequest.split(" ");
	        
		        String returnClientRequest = "";
		        String contentType = "text/plain";
		        byte[] returnByte = null;
		        
		        //create response
		        int urlSeperatorLoc = request[1].indexOf("?");
		        String requestedPath;
		        String urlParameter = "";
		        if(urlSeperatorLoc > 0) {
		        	requestedPath = request[1].substring(0, urlSeperatorLoc);
		        	urlParameter = request[1].substring(urlSeperatorLoc + 1);
		        }else {
		        	requestedPath = request[1];
		        }
		        switch(requestedPath) {
		        case "/initDiscussion":
		        	returnClientRequest = apiFunctions.initDiscussion(urlParameter);
		        	break;
		        case "/view":
		        	returnClientRequest = apiFunctions.requestDiscussionJson(urlParameter);
		        	break;
		        case "/socket":
		        	int success = upgradeToWebSocket(urlParameter, requestHeader, requestHeaderString, inFromClient, headerOut, outToClient);
		        	if(success == 1) {
		        		log.log("Web Socket Established");
		        	}else {
		        		log.error("Web Socket Failed");
		        	}
		        	return;
				case "/getLoginUrl":
					returnClientRequest = apiFunctions.getLoginUrl();
					break;
				case "/updateImage":
					returnClientRequest = apiFunctions.updateImage(urlParameter);
					break;
				case "/oauth_complete":
					returnClientRequest = apiFunctions.oauthComplete(urlParameter);
					contentType = "text/html";
					break;
				case "/checkLogin":
					returnClientRequest = apiFunctions.checkLogin(urlParameter);
					break;
				case "/tokenLogin":
					returnClientRequest = apiFunctions.tokenLogin(urlParameter);
					break;
				case "/listDiscussions":
					returnClientRequest = apiFunctions.listDiscussions(urlParameter);
					break;
		        case "/initDiscu":
		        	returnClientRequest = apiFunctions.loginUser(request[1].substring(urlSeperatorLoc + 1));
		        	break;
		        case "/changeUserName":
		        	returnClientRequest = apiFunctions.changeUserName(urlParameter);
		        	break;
		        case "/changeDiscussionTitle":
		        	returnClientRequest = apiFunctions.changeDiscussionTitle(urlParameter);
		        	break;
		        case "/changeDiscussionTopic":
		        	returnClientRequest = apiFunctions.changeDiscussionTopic(urlParameter);
		        	break;
		        case "/leaveDiscussion":
		        	returnClientRequest = apiFunctions.leaveDiscussion(urlParameter);
		        	break;
		        case "/createDiscussion":
		        	returnClientRequest = apiFunctions.createDiscussion();
		        	break;
		        case "/newUser":
		        	returnClientRequest = apiFunctions.newUser(urlParameter);
		        	break;
		        case "/join":
		        	returnClientRequest = apiFunctions.join(urlParameter);
		        	break;
		        case "/send":
		        	returnClientRequest = apiFunctions.sendMessage(urlParameter, inFromClient);
		        	break;
		        case "/reply":
		        	returnClientRequest = apiFunctions.sendReply(urlParameter, inFromClient);
		        	break;
				case "/favicon.ico":
					returnByte = apiFunctions.getFavicon();
					contentType = "image/x-icon";
					break;
		        case "":
		        case "/":
		        	returnClientRequest = "this is a canvas server for live discussion";
		        	break;
		        default:
					returnClientRequest = "404 - Api Url Not Found";
					break;
		        }
		        
		        if(returnByte == null) {
		        	returnByte = returnClientRequest.getBytes();
		        }
		        
				// send HTTP Headers
				headerOut.println("HTTP/1.1 200 OK");
				headerOut.println("Server: Canvas Server");
				headerOut.println("Date: " + new Date());
				headerOut.println("Content-type: " + contentType);
				headerOut.println("Content-length: " + returnByte.length);
				headerOut.println("Access-Control-Allow-Origin: *");
				headerOut.println(); // blank line between headers and content, very important !
				headerOut.flush(); // flush character output stream buffer
		        
				outToClient.write(returnByte, 0, returnByte.length);
				outToClient.flush();
	
		        
		        /*StringBuilder stringBuilder = new StringBuilder();
		        String line;
		        while( (line = inFromClient.readLine()) != null) {
		        	stringBuilder.append(line+"\n");
		        }
		        String fullRequestHeader = stringBuilder.toString();
		        */
	        }
			client.close();
			
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int upgradeToWebSocket(String parametersString, HashMap<String, String> clientRequest, String requestHeaderString, BufferedReader inFromClient, PrintWriter headerOut, BufferedOutputStream outToClient) {
		try {
			//TODO make into function
			//get the socket key to establish connection
	        String line1;
	        int backup = 0;
	        while ((line1 = inFromClient.readLine()) != null) {
	            requestHeaderString += line1;
	        	if (!line1.isEmpty()) {
		        	int colonLoc = line1.indexOf(":");
		        	if(colonLoc > 0) {
		        		clientRequest.put(line1.substring(0, colonLoc), line1.substring(colonLoc+2));
		        	}else {
		        		clientRequest.put(""+backup, line1);
		        		backup++;
		        	}
	            }
	        	if(line1.contains("Sec-WebSocket-Key")) break;
	        }
			String socketKey = clientRequest.get("Sec-WebSocket-Key");
			
			//log how many connections the user has to the server, limits it to a certain number, right now 17 and doesn't block it
			if(websocketHandler.clientConnections.containsKey(client.getInetAddress().toString())) {
				//add one to the count
				int value = websocketHandler.clientConnections.get(client.getInetAddress().toString());
				
				if(value > 17) {
					//return 0;
				}
				websocketHandler.clientConnections.replace(client.getInetAddress().toString(), value++);
			}else {
				websocketHandler.clientConnections.put(client.getInetAddress().toString(), 1);
			}

			//get socket id
			HashMap<String, String>parameters = apiFunctions.parseUrlParameter(parametersString);
			if(!parameters.containsKey("socketId")) return -1; //need socket id
			WebSocket websocket = websocketHandler.get(parameters.get("socketId"));
			if(websocket == null) {
				return 0;
			}
			
			//handshake
			headerOut.println("HTTP/1.1 101 Switching Protocols");
			headerOut.println("Connection: Upgrade");
			headerOut.println("Upgrade: WebSocket");
			headerOut.println("Sec-WebSocket-Accept: "+ Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((socketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8"))));

			headerOut.println(); // blank line between headers and content, very important !
			headerOut.flush(); // flush character output stream buffer
			
			//continued in websocket
			websocket.initSetup(client, inFromClient, outToClient);
			
			discussionHandler.get(websocket.getDiscussionId()).setOnline(parameters.get("socketId"));
		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			log.error("writing websocket header error");
		}
		return 1;
	}
}
