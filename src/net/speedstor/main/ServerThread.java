package net.speedstor.main;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.Icon;

import org.json.simple.JSONObject;

import net.speedstor.main.DiscussionHandler;
import net.speedstor.main.Log;
import net.speedstor.main.Server;
import net.speedstor.websocket.WebSocket;
import net.speedstor.websocket.WebSocketHandler;

public class ServerThread extends Thread{
	Socket client;
	Log log;
	Server server;
	WebSocketHandler websocketHandler;
	TokenHandler tokenHandler;
	Clock clock;
	
	public ServerThread(Socket client, Log log, Server server, WebSocketHandler websocketHandler, Clock clock, TokenHandler tokenHandler) {
		this.client = client;
		this.log = log;
		this.server = server;
		this.websocketHandler = websocketHandler;
		this.tokenHandler = tokenHandler;
		this.clock = clock;
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
		        byte[] returnByte;
		        
		        //create response
		        int urlSeperatorLoc = request[1].indexOf("?");
		        String requestedPath;
		        if(urlSeperatorLoc > 0) {
		        	requestedPath = request[1].substring(0, urlSeperatorLoc);
		        }else {
		        	requestedPath = request[1];
		        }
		        switch(requestedPath) {
		        case "/initDiscu":
		        	returnClientRequest = loginUser(request[1].substring(urlSeperatorLoc + 1));
		        	break;
		        case "/view":
		        	returnClientRequest = requestDiscussionJson(request[1].substring(urlSeperatorLoc + 1));
		        	break;
		        case "/socket":
		        	int success = upgradeToWebSocket(request[1].substring(urlSeperatorLoc + 1), requestHeader, requestHeaderString, inFromClient, headerOut, outToClient);
		        	if(success == 1) {
		        		log.log("Web Socket Established");
		        	}else {
		        		log.error("Web Socket Failed");
		        	}
		        	return;
				case "/favicon.ico":
		        	//doesn't work yet
		        	try {
		        		File file = new File(getClass().getResource("/resource/favicon.ico").getFile());
		        	
						int fileLength = (int) file.length();
						
						FileInputStream fileInputStream = null;
						byte[] fileData = new byte[fileLength];
						
						try {
							fileInputStream = new FileInputStream(file);
							fileInputStream.read(fileData);
						} finally {
							if (fileInputStream != null) 
								fileInputStream.close();
						}
						
						
						contentType = "image/vnd";
						//contentType = "text/plain";
						returnByte = fileData;
					}catch (IOException e){
			        	returnClientRequest = "error";
			        	log.error("Requested for favicon.ico, still didn't implement");		        		
		        	}
					break;
		        case "":
		        case "/":
		        	returnClientRequest = "this is a canvas server for live discussion";
		        	break;
		        default:
					returnClientRequest = "404 - Not Found";
					break;
		        }
		        
		        if(returnClientRequest != "") {
		        	returnByte = returnClientRequest.getBytes();
		        }else {
		        	returnByte = new byte[0];
		        }
		        
				// send HTTP Headers
				headerOut.println("HTTP/1.1 200 OK");
				headerOut.println("Server: Java HTTP Server from SSaurel : 1.0");
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
	        
	        /*try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        */
			client.close();
			
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String requestDiscussionJson(String parametersString) {
		String[] parametersArray = parametersString.split("&");
		HashMap<String, String> parameters = new HashMap<>();
		for(int i = 0; i < parametersArray.length; i++) {
			int equalLoc = parametersArray[i].indexOf("=");
			if(equalLoc > 0) {
				String key = parametersArray[i].substring(0, equalLoc);
				String value = parametersArray[i].substring(equalLoc + 1);
				
				parameters.put(key, value);
			}else {
				return "0-parameter error";
			}
		}

		if(!parameters.containsKey("socketId")) {
			return "0-must need token";
		}

		if(!parameters.containsKey("url")) {
			return "0-must need discussionUrl";
		}
		
		WebSocket userSocket = websocketHandler.get(parameters.get("socketId"));
		if(userSocket != null) {
			String requestUrl = "https://fairmontschools.instructure.com/api/v1"+parameters.get("url").substring(parameters.get("url").indexOf("/courses/"))+"/view";
			log.special(userSocket.getUrl());
			log.special(requestUrl);
			if(userSocket.getUrl().equals(requestUrl)) {
				//had already logged in 
				String responseJson = server.runningDiscussionBoards.get(requestUrl).getDiscussionJson();
				log.log(responseJson);
				return responseJson;
			}else {
				return "0-authorization needed";				
			}
		}else {
			return "0-authorization needed";
		}
	}

	private int upgradeToWebSocket(String parametersString, HashMap<String, String> clientRequest, String requestHeaderString, BufferedReader inFromClient, PrintWriter headerOut, BufferedOutputStream outToClient) {

		try {
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
			
			
			log.log(clientRequest.toString());
			String socketKey = clientRequest.get("Sec-WebSocket-Key");
			
			if(server.clientConnections.containsKey(client.getInetAddress().toString())) {
				//add one to the count
				int value = server.clientConnections.get(client.getInetAddress().toString());
				
				if(value > 17) {
					//return 0;
				}
				server.clientConnections.replace(client.getInetAddress().toString(), value++);
			}else {
				server.clientConnections.put(client.getInetAddress().toString(), 1);
			}

			
			//handshake
			
			headerOut.println("HTTP/1.1 101 Switching Protocols");
			headerOut.println("Connection: Upgrade");
			headerOut.println("Upgrade: WebSocket");
			headerOut.println("Sec-WebSocket-Accept: "+ Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((socketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8"))));

			headerOut.println(); // blank line between headers and content, very important !
			headerOut.flush(); // flush character output stream buffer
			
			
			//get socket id
			String[] parametersArray = parametersString.split("&");
			JSONObject parameters = new JSONObject();
			for(int i = 0; i < parametersArray.length; i++) {
				int equalLoc = parametersArray[i].indexOf("=");
				if(equalLoc > 0) {
					String key = parametersArray[i].substring(0, equalLoc);
					String value = parametersArray[i].substring(equalLoc + 1);
					
					parameters.put(key, value);
				}else {
					return -1;
				}
			}
			if(!parameters.containsKey("socketId")) return -1; //need socket id
			
			//continued in websocket
			WebSocket websocket = websocketHandler.get((String) parameters.get("socketId"));
			((WebSocket) websocket).initSetup(client, inFromClient, outToClient);
			
			server.runningDiscussionBoards.get(((WebSocket) websocket).getUrl()).addParticipant((String) parameters.get("socketId"));

		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			log.error("writing websocket header error");
		}
		return 1;
	}

	private String loginUser(String parametersString) {
		String[] parametersArray = parametersString.split("&");
		JSONObject parameters = new JSONObject();
		for(int i = 0; i < parametersArray.length; i++) {
			int equalLoc = parametersArray[i].indexOf("=");
			if(equalLoc > 0) {
				String key = parametersArray[i].substring(0, equalLoc);
				String value = parametersArray[i].substring(equalLoc + 1);
				
				parameters.put(key, value);
			}else {
				return "0-parameter error";
			}
		}
		
		if(!parameters.containsKey("token")) {
			return "0-must need token";
		}
		
		if(!parameters.containsKey("url")) {
			return "0-must need url";
		}
		
		String canvasToken = (String) parameters.get("token");
		String testApiFetch = sendGet("https://fairmontschools.instructure.com/api/v1/courses?access_token="+canvasToken);
		String errorResponseString = "error: catch error";
		if(testApiFetch == errorResponseString) {
			log.warn("Invalid login attempt");
			return "0-invalid token";
		}
		
		//token for communication between server and client, not using canvas token
		int tokenLength = 15; //do not change !!!
		String token;

		if(tokenHandler.containValue(canvasToken)) {
			String[] tokenPossibilities = tokenHandler.getKeyFromValue(canvasToken);
			log.log("tokenPossiblities: "+Arrays.toString(tokenPossibilities));
			if(tokenPossibilities.length <= 0) {
				//do the same code as it does not have the value
				token = getAlphaNumericString(tokenLength);
				while(tokenHandler.contains(token)) {
					token = getAlphaNumericString(tokenLength);				
				}
				tokenHandler.addToken(token, canvasToken);
			}else if(tokenPossibilities.length == 1) {
				token = tokenPossibilities[0];
			}else {
				token = tokenPossibilities[0];
				log.warn("repeated canvas Id in id system");
			}
		}else {
			
			token = getAlphaNumericString(tokenLength);
			
			//make sure no repeat
			while(tokenHandler.contains(token)) {
				token = getAlphaNumericString(tokenLength);				
			}
			
			//write in file: serverToken - CanvasToken

			tokenHandler.addToken(token, canvasToken);
		}
		

		String discussionUrl = (String) parameters.get("url");
		if(discussionUrl.contains("?")) {
			discussionUrl = discussionUrl.substring(0, discussionUrl.indexOf("?"));
		}
		
		if(discussionUrl.contains("discussion_topics") && discussionUrl.contains("fairmontschools.instructure.com/courses/")) {
			discussionUrl = "https://fairmontschools.instructure.com/api/v1"+discussionUrl.substring(discussionUrl.indexOf("/courses/"))+"/view";
			
			String testDiscussionResponse = sendGet(discussionUrl+"?include_new_entries=1&include_enrollment_state=1&include_context_card_info=1&access_token="+canvasToken);
			if(testDiscussionResponse == "error: catch error") {
				return "0-noAccessDisu";
			}
			
			if(server.runningDiscussionBoards.containsKey(discussionUrl)) {
				//server.runningDiscussionBoards.get(discussionUrl).addParticipant(token);
			}else {
				DiscussionHandler discussionHandler = new DiscussionHandler(log, discussionUrl, clock, tokenHandler, token, websocketHandler);
				//discussionHandler.addParticipant(token);
				Thread discussionHandlerThread = new Thread(discussionHandler);
				discussionHandlerThread.start();
				
				
				server.runningDiscussionBoards.put(discussionUrl, discussionHandler);
				
				log.log("New DBoard: " + discussionHandler.toString() + "; Total Boards: " + server.runningDiscussionBoards.size() + "; url: " + discussionUrl.substring(40));
			}
			
			

			String socketId = getAlphaNumericString(tokenLength);
			//make sure no repeat
			while(websocketHandler.containSocket(socketId)) {
				socketId = getAlphaNumericString(tokenLength);
			}
			
			log.log("New Socket: "+ socketId);
			
			WebSocket newWebsocket = new WebSocket(log, clock, token, discussionUrl, server, tokenHandler, socketId);
			
			websocketHandler.addWebsocket(socketId, newWebsocket, token);
			
			token += "-" + socketId;
			
		}else {
			token = "0-urlNotDiscussion"; //needs to be a discussion topic
		}
		
		
		return token;
	}
	

	// from  https://www.geeksforgeeks.org/generate-random-string-of-given-size-in-java/
    // function to generate a random string of length n 
    private static String getAlphaNumericString(int n) { 
  
        // chose a Character random from this String 
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                    + "0123456789"
                                    + "abcdefghijklmnopqrstuvxyz"; 
  
        // create StringBuffer size of AlphaNumericString 
        StringBuilder sb = new StringBuilder(n); 
  
        for (int i = 0; i < n; i++) { 
  
            // generate a random number between 
            // 0 to AlphaNumericString variable length 
            int index 
                = (int)(AlphaNumericString.length() 
                        * Math.random()); 
  
            // add Character one by one in end of sb 
            sb.append(AlphaNumericString 
                          .charAt(index)); 
        } 
  
        return sb.toString(); 
    } 
	
    
    
	public String sendGet(String url) {
		try {
			URL urlObj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
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
