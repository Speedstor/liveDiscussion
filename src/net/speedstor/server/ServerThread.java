package net.speedstor.server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.speedstor.control.Credentials;
import net.speedstor.control.Log;
import net.speedstor.control.Settings;
import net.speedstor.discussion.CanvasDiscussion;
import net.speedstor.discussion.DiscussionHandler;
import net.speedstor.main.Cache;
import net.speedstor.main.ThreadScripts;
import net.speedstor.main.TokenHandler;
import net.speedstor.network.Network;
import net.speedstor.server.Server;
import net.speedstor.websocket.WebSocket;
import net.speedstor.websocket.WebSocketHandler;

public class ServerThread extends Thread{
	Socket client;
	Log log;
	Cache cache;
	Server server;
	WebSocketHandler websocketHandler;
	TokenHandler tokenHandler;
	DiscussionHandler discussionHandler;
	Clock clock;
	
	//token for communication between server and client, not using canvas token
	int TOKEN_LENGTH = 15; //do not change !!!
	
	JSONParser parser = new JSONParser();
	
	public ServerThread(Socket client, Log log, Server server, WebSocketHandler websocketHandler, Clock clock, TokenHandler tokenHandler, DiscussionHandler discussionHandler, Cache cache) {
		this.client = client;
		this.log = log;
		this.server = server;
		this.websocketHandler = websocketHandler;
		this.tokenHandler = tokenHandler;
		this.discussionHandler = discussionHandler;
		this.clock = clock;
		this.cache = cache;
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
		        String urlParameter = "";
		        if(urlSeperatorLoc > 0) {
		        	requestedPath = request[1].substring(0, urlSeperatorLoc);
		        	urlParameter = request[1].substring(urlSeperatorLoc + 1);
		        }else {
		        	requestedPath = request[1];
		        }
		        switch(requestedPath) {
		        case "/initDiscussion":
		        	returnClientRequest = initDiscussion(urlParameter);
		        	break;
		        case "/view":
		        	returnClientRequest = requestDiscussionJson(urlParameter);
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
					returnClientRequest = getLoginUrl();
					break;
				case "/updateImage":
					returnClientRequest = updateImage(urlParameter);
					break;
				case "/oauth_complete":
					returnClientRequest = oauthComplete(urlParameter);
					contentType = "text/html";
					break;
				case "/checkLogin":
					returnClientRequest = checkLogin(urlParameter);
					break;
				case "/tokenLogin":
					returnClientRequest = tokenLogin(urlParameter);
					break;
				case "/listDiscussions":
					returnClientRequest = listDiscussions(urlParameter);
					break;
		        case "/initDiscu":
		        	returnClientRequest = loginUser(request[1].substring(urlSeperatorLoc + 1));
		        	break;
		        case "/send":
		        	returnClientRequest = sendMessage(urlParameter, inFromClient);
		        	break;
		        case "/reply":
		        	returnClientRequest = sendReply(urlParameter, inFromClient);
		        	break;
				case "/favicon.ico":
		        	//doesn't work yet
					returnByte = getFavicon();
					contentType = "image/vnd";
					//contentType = "text/plain";
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
			client.close();
			
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//#region oauth login
	private String checkLogin(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null) return "0-corrupted parameters or server error";
		
		if(parameters.containsKey("accountId") && parameters.containsKey("socketId")) {			
			if(tokenHandler.tokenDB_containKey(parameters.get("accountId"))) {  //is logged in system
				if(!websocketHandler.containSocket(parameters.get("socketId"))) { //is not socketId had already connected on another session||tab					
					if(parameters.get("accountId").equals( tokenHandler.socketList_get(parameters.get("socketId")) )) { //is socketId update to current server session
						return "true";
					}
				}
				//else
				String socketId = getAlphaNumericString(TOKEN_LENGTH);
				while(tokenHandler.socketList_containKey(socketId))  socketId = getAlphaNumericString(TOKEN_LENGTH);
				
				tokenHandler.socketList_put(socketId, parameters.get("accountId"));
				return "outdatedSocket "+socketId;
			}else {
				return "false";
			}
		}
		
		if(parameters.containsKey("accountId")) {
			if(tokenHandler.tokenDB_containKey(parameters.get("accountId"))) {
				return "true";
			}else {
				return "false";
			}
		}
		
		if(parameters.containsKey("socketId")) {
			if(tokenHandler.socketList_containKey(parameters.get("socketId"))) {
				return "true";
			}else {
				return "false";
			}
		}
		
		return "0-Error";
	}

	private String oauthComplete(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null) return "0-corrupted parameters or server error";
		
		String returnHtml = "0-Error";
		
		if(parameters.containsKey("code") && parameters.containsKey("state")) {
			String canvasCode = parameters.get("code");
			String serverToken = parameters.get("state");
			//POST request to https://fairmontschools.beta.instructure.com/login/oauth2/token
			String canvasTokenJson = Network.sendPost("https://fairmontschools.beta.instructure.com/login/oauth2/token?code="+canvasCode+"&grant_type=authorization_code&client_id="+Credentials.CLIENT_ID+"&client_secret="+Credentials.CLIENT_SECRET+"&redirect_uri=http://http.speedstor.net:40/oauth_complete");
			
			try {
				JSONObject canvasResponse = (JSONObject) parser.parse(canvasTokenJson);
				
				String canvasToken = (String) canvasResponse.get("access_token");
				String refreshToken = (String) canvasResponse.get("refresh_token");
				String id = ""+((JSONObject) canvasResponse.get("user")).get("id");
				String userName = (String) ((JSONObject) canvasResponse.get("user")).get("name");
				
				tokenHandler.appendToken(serverToken, canvasToken, refreshToken, id, userName);
				
				returnHtml = "<script>window.location.href='"+Settings.LOGIN_DESTINATION+"'</script><h3>hello, you are not supposed to be seeing this, if trying it again does not work<br/>please contact: aldrin@speedstor.net</h3>";
			} catch (ParseException e) {
				log.error("parse canvas response error");
				returnHtml = "<h3 style='color: darkred;'>Sever parse canvas response error, if trying it again does not work<br/>please contact: aldrin@speedstor.net</h3>";
			}
		}else {
			return "0-must need code & state";
		}
		
		return returnHtml;
	}

	private String getLoginUrl() {
		String accountId = getAlphaNumericString(TOKEN_LENGTH); //serverToken
		while(tokenHandler.tokenDB_containKey(accountId))  accountId = getAlphaNumericString(TOKEN_LENGTH);
		while(tokenHandler.socketList_containValue(accountId))  accountId = getAlphaNumericString(TOKEN_LENGTH);
		
		String socketId = getAlphaNumericString(TOKEN_LENGTH);
		while(tokenHandler.socketList_containKey(socketId))  socketId = getAlphaNumericString(TOKEN_LENGTH);
		
		tokenHandler.socketList_put(socketId, accountId);
				
		String url = "https://fairmontschools.beta.instructure.com/login/oauth2/auth?client_id="+Credentials.CLIENT_ID+"&response_type=code&redirect_uri=http://http.speedstor.net:40/oauth_complete&state=" + 
						accountId;
		
		return "{\"url\": \""+url+"\", \"socketId\": \""+socketId+"\", \"accountId\": \""+accountId+"\"}";
		//{"url": ...., "socketId": ....., "accountId": .......};
	}
	//#endregion
	
	//#region Discussion board
	@SuppressWarnings("unchecked")
	private String listDiscussions(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "0-parameter error";

		if(!parameters.containsKey("socketId"))  return "0-must need socketId";
		if(!tokenHandler.socketList_containKey(parameters.get("socketId"))) return "0-invalid socketId";
		
		String canvasToken = tokenHandler.tokenDB_getCanvasTokenFromSocket(parameters.get("socketId"));

		cache.defineCategory("discussionLists");
		if(cache.categoryContainKey("discussionLists", canvasToken)) {
			try {
				ThreadScripts.updateDiscussionLists(canvasToken, cache, log);
			}catch(Exception e){
				log.error("ThreadScrips error: "+e.getLocalizedMessage());
			}
			return cache.getCachedData("discussionLists", canvasToken);
		}
		//get list of courses
		String coursesResponse = Network.sendGetWithAuth(Settings.API_URL+"/courses", canvasToken);
				
		//get list of discussions
		JSONObject discussionList = new JSONObject();
		try {
			JSONArray coursesJson = (JSONArray) parser.parse(coursesResponse);
			coursesJson.forEach(courseItem -> {
				String courseId = ""+((JSONObject) courseItem).get("id");
				JSONObject discussionObject = new JSONObject();
				String discussionResponse = Network.sendGetWithAuth(Settings.API_URL+"/courses/"+courseId+"/discussion_topics", canvasToken);
				
				try {
					JSONArray discussionArray = (JSONArray) parser.parse(discussionResponse);
					discussionObject.put("name", (String) ((JSONObject) courseItem).get("name"));
					discussionObject.put("id", courseId);
					discussionObject.put("discussions", discussionArray);
				} catch (ParseException e) {
					log.error("parse json for listing discussion error");
				}

				discussionList.put(courseId, discussionObject);
			});
		} catch (ParseException e) {
			log.error("parse json for listing courses error");
			return "0-server parse json error";
		}
		
		cache.putData("discussionLists", canvasToken, discussionList.toJSONString());
		
		return discussionList.toJSONString();
	}

	private String initDiscussion(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "0-parameter error";

		if(!parameters.containsKey("socketId"))  return "0-must need socketId";
		if(!parameters.containsKey("id"))  return "0-must need discussion id";
		if(!tokenHandler.socketList_containKey(parameters.get("socketId"))) return "0-invalid socketId";
		
		String socketId = parameters.get("socketId");
		String serverToken = tokenHandler.socketList_get(socketId);
		String canvasToken = tokenHandler.tokenDB_get(serverToken)[0];
		
		String[] discussionIds = parameters.get("id").split("v");
		String courseId;
		String discussionId;
		if(discussionIds.length == 2) {
			courseId = discussionIds[0];
			discussionId = discussionIds[1];
		}else {
			return "0-invalid discussion id";
		}
	
		String discussionUrl = Settings.API_URL+"/courses/"+courseId+"/discussion_topics/"+discussionId;
		
		//check accessibility of user
		String testDiscussionResponse = Network.sendGet(discussionUrl+"?access_token="+canvasToken);
		if(testDiscussionResponse == "error: catch error") {
			return "0-lack permission to discussion";
		}

		discussionUrl += "/view";
		
		String reconstructedId = courseId+"v"+discussionId;
		if(discussionHandler.canvas_containKey(reconstructedId)) {
			//discussionHandler.canvas_get(reconstructedId).addParticipant(socketId);
		}else {
			CanvasDiscussion canvasDiscussion = new CanvasDiscussion(log, discussionUrl, clock, tokenHandler, serverToken, websocketHandler, cache);
			//discussionHandler.addParticipant(token);
			
			try {
				JSONObject discussion = (JSONObject) parser.parse(testDiscussionResponse);
				canvasDiscussion.setTopic((String) discussion.get("message"));
				canvasDiscussion.setTitle((String) discussion.get("title"));
			} catch (ParseException e) {
				log.error("Error parsing discussion");
			}
			
			Thread discussionHandlerThread = new Thread(canvasDiscussion);
			discussionHandlerThread.start();
			discussionHandler.canvas_put(reconstructedId, canvasDiscussion);
			
			log.log("New DBoard: " + canvasDiscussion.toString() + "; Total Boards: " + discussionHandler.canvas_getSize() + "; id: " + reconstructedId);
		}
		
		WebSocket newWebsocket = new WebSocket(log, clock, reconstructedId, server, tokenHandler, socketId, discussionHandler, websocketHandler, tokenHandler.tokenDB_get(serverToken)[2]);
		
		websocketHandler.addWebsocket(socketId, newWebsocket);
		
		return "true "+tokenHandler.tokenDB_get(serverToken)[2];
	}
	
	private String requestDiscussionJson(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "0-parameter error";

		if(!parameters.containsKey("socketId"))  return "0-must need token";
		if(!parameters.containsKey("id")) return "0-must need discussionUrl or id";
		
		//WARN: thrown legacy support for getting json from url, must from id
		//requestUrl = Settings.API_URL+parameters.get("url").substring(parameters.get("url").indexOf("/courses/"))+"/view";
		
		if(tokenHandler.socketList_containKey(parameters.get("socketId"))) {
			//had already logged in
			CanvasDiscussion focusedDiscussionHandler = discussionHandler.canvas_get(parameters.get("id"));
			String responseJson = null;
			while(responseJson == null) {
					responseJson = focusedDiscussionHandler.getDiscussionJson();
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) { 
						e.printStackTrace();
					}
			}
			return responseJson;
		}else {
			return "0-authorization needed";
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
			HashMap<String, String>parameters = parseUrlParameter(parametersString);
			if(!parameters.containsKey("socketId")) return -1; //need socket id
			
			//handshake
			headerOut.println("HTTP/1.1 101 Switching Protocols");
			headerOut.println("Connection: Upgrade");
			headerOut.println("Upgrade: WebSocket");
			headerOut.println("Sec-WebSocket-Accept: "+ Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((socketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8"))));

			headerOut.println(); // blank line between headers and content, very important !
			headerOut.flush(); // flush character output stream buffer
			
			//continued in websocket
			WebSocket websocket = websocketHandler.get(parameters.get("socketId"));
			websocket.initSetup(client, inFromClient, outToClient);  
			
			discussionHandler.canvas_get(websocket.getDiscussionId()).addParticipant(parameters.get("socketId"));
		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			log.error("writing websocket header error");
		}
		return 1;
	}
	
	private String sendMessage(String urlParameter, BufferedReader inFromClient) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "0-parameter error";
		
		if(!parameters.containsKey("socketId")) return "0-need socketId";
		if(!parameters.containsKey("discussionId")) return "0-need discussionId";
		
		String message;
		try {
			//rest of the headers, no need for now
			while(inFromClient.readLine().length() != 0); //when the receiving line is 0, it means that it is a empty line, seperating it from the body of the content
				
			StringBuilder payloadString = new StringBuilder();
			while(inFromClient.ready()) {
				payloadString.append((char) inFromClient.read());
			}
			//log.special(payloadString.toString());
			JSONObject payload = (JSONObject) parser.parse(payloadString.toString());
			if(!payload.containsKey("message")) return "0-body need message";
			message = (String) payload.get("message");
		}catch(ParseException e) {
			return "0-body json parse error";
		} catch (IOException e) {
			log.error("/send page inFromClient IOException");
			return "0-server connection error";
		}
		
		CanvasDiscussion canvasDiscussion = discussionHandler.canvas_get(parameters.get("discussionId"));		
		return canvasDiscussion.sendNewMessage(parameters.get("socketId"), message);
	}
	
	private String sendReply(String urlParameter, BufferedReader inFromClient) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "0-parameter error";
		
		if(!parameters.containsKey("socketId")) return "0-need socketId";
		if(!parameters.containsKey("discussionId")) return "0-need discussionId";
		
		String message;
		String replyTo;
		try {
			//rest of the headers, no need for now
			while(inFromClient.readLine().length() != 0); //when the receiving line is 0, it means that it is a empty line, seperating it from the body of the content
				
			StringBuilder payloadString = new StringBuilder();
			while(inFromClient.ready()) {
				payloadString.append((char) inFromClient.read());
			}
		
			log.debug(payloadString.toString());
			JSONObject payload = (JSONObject) parser.parse(payloadString.toString());
			if(!payload.containsKey("message")) return "0-body need message";
			if(!payload.containsKey("replyTo")) return "0-body need message id to reply to";
			message = (String) payload.get("message");
			replyTo = payload.get("replyTo")+"";
		}catch(ParseException e) {
			log.log(e.getMessage());
			return "0-body json parse error";
		} catch (IOException e) {
			log.error("/send page inFromClient IOException");
			return "0-server connection error";
		}
		
		CanvasDiscussion canvasDiscussion = discussionHandler.canvas_get(parameters.get("discussionId"));		
		return canvasDiscussion.sendNewReply(parameters.get("socketId"), replyTo, message);
	}
	
	public String updateImage(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "0-parameter error";

		if(!parameters.containsKey("socketId")) return "0-need socketId";
		if(!parameters.containsKey("imageUrl")) return "0-need imageUrl";
		
		String socketId = parameters.get("socketId");
		if(!websocketHandler.containSocket(socketId)) return "0-invalid socketId";
		
		CanvasDiscussion canvasDiscussion = discussionHandler.canvas_getFromId(websocketHandler.get(socketId).getDiscussionId());
		String userId = tokenHandler.tokenDB_get(tokenHandler.socketList_get(socketId))[2];
		
		cache.defineCategory("profileImgs");
		cache.putData("profileImgs", userId, parameters.get("imageUrl"));
		
		log.log(parameters.get("imageUrl"));
		
		canvasDiscussion.updateProfileImage(userId, parameters.get("imageUrl"));
		canvasDiscussion.sendToAll("{\"profileImage\": {\"user_id\": \""+userId+"\", \"url\": \""+parameters.get("imageUrl")+"\"}}");
		
		return "success";
	}
	//#endregion
	
	//#region legacy outdated
	private String tokenLogin(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		
		if(!parameters.containsKey("canvasToken")) return "0-need canvas token";

		String canvasToken = parameters.get("canvasToken");
		String accountUserId = "";
		
		//check token valid && also get id of user
		String testApiFetch = Network.sendGet("https://fairmontschools.instructure.com/api/v1/courses?access_token="+canvasToken);
		String errorResponseString = "error: catch error";
		if(testApiFetch == errorResponseString) {
			log.warn("Invalid login attempt");
			return "0-invalid token";
		}else {
			try {
				JSONArray courses = (JSONArray) parser.parse(testApiFetch);
				accountUserId = ((JSONObject) ((JSONArray) ((JSONObject) courses.get(0)).get("enrollments")).get(0)).get("user_id")+"";
			} catch (ParseException e) {
				log.error("parse account course json error");
				//e.printStackTrace();
				return "0-server error";
			}
		}
		
		String accountId = getAlphaNumericString(TOKEN_LENGTH); //serverToken
		while(tokenHandler.tokenDB_containKey(accountId))  accountId = getAlphaNumericString(TOKEN_LENGTH);
		while(tokenHandler.socketList_containValue(accountId))  accountId = getAlphaNumericString(TOKEN_LENGTH);
		
		String socketId = getAlphaNumericString(TOKEN_LENGTH);
		while(tokenHandler.socketList_containKey(socketId))  socketId = getAlphaNumericString(TOKEN_LENGTH);

		tokenHandler.socketList_put(socketId, accountId);
		tokenHandler.addToken(accountId, canvasToken, "null", accountUserId, "directTokenLogin");
		
		return "{\"url\": \""+Settings.LOGIN_DESTINATION+"\", \"socketId\": \""+socketId+"\", \"accountId\": \""+accountId+"\"}";
	}
	
	private String loginUser(String parametersString) {
		HashMap<String, String> parameters = parseUrlParameter(parametersString);
		
		if(!parameters.containsKey("token")) {
			return "0-must need token";
		}
		
		if(!parameters.containsKey("url")) {
			return "0-must need url";
		}
		
		String canvasToken = parameters.get("token");
		String accountUserId = "";
		
		String testApiFetch = Network.sendGet("https://fairmontschools.instructure.com/api/v1/courses?access_token="+canvasToken);
		String errorResponseString = "error: catch error";
		if(testApiFetch == errorResponseString) {
			log.warn("Invalid login attempt");
			return "0-invalid token";
		}else {
			try {
				JSONArray courses = (JSONArray) parser.parse(testApiFetch);
				accountUserId = ((JSONObject) ((JSONArray) ((JSONObject) courses.get(0)).get("enrollments")).get(0)).get("user_id")+"";
			} catch (ParseException e) {
				log.error("parse account course json error");
				//e.printStackTrace();
				return "0-server error";
			}
		}
		
		String token;
		token = getAlphaNumericString(TOKEN_LENGTH);
		//make sure no repeat
		while(tokenHandler.tokenDB_containKey(token)) {
			token = getAlphaNumericString(TOKEN_LENGTH);				
		}
		
		tokenHandler.addToken(token, canvasToken, "null", accountUserId, "null");
		
		String discussionUrl = parameters.get("url");
		if(discussionUrl.contains("?")) {
			discussionUrl = discussionUrl.substring(0, discussionUrl.indexOf("?"));
		}
		
		if(discussionUrl.contains("discussion_topics") && discussionUrl.contains("/courses/")) {
			discussionUrl = "https://fairmontschools.instructure.com/api/v1"+discussionUrl.substring(discussionUrl.indexOf("/courses/"));
			
			
			String testDiscussionResponse = Network.sendGet(discussionUrl+"?access_token="+canvasToken);
			if(testDiscussionResponse == "error: catch error") {
				return "0-noAccessDisu";
			}

			discussionUrl += "/view";
			

			int stringCutIndex = discussionUrl.indexOf("/dicussion");
			String discussionId = discussionUrl.substring(discussionUrl.indexOf("/courses/") + 9, stringCutIndex)+"v"+discussionUrl.substring(stringCutIndex+18);
			
			if(discussionHandler.canvas_containKey(discussionUrl)) {
				//server.runningDiscussionBoards.get(discussionUrl).addParticipant(token);
			}else {
				CanvasDiscussion canvasDiscussion = new CanvasDiscussion(log, discussionUrl, clock, tokenHandler, token, websocketHandler, cache);
				//discussionHandler.addParticipant(token);
				

				try {
					JSONObject discussion = (JSONObject) parser.parse(testDiscussionResponse);
					canvasDiscussion.setTopic((String) discussion.get("message"));
					canvasDiscussion.setTitle((String) discussion.get("title"));
				} catch (ParseException e) {
					log.error("Error parsing discussion");
				}
				
				
				Thread discussionHandlerThread = new Thread(canvasDiscussion);
				discussionHandlerThread.start();
				
				discussionHandler.canvas_put(discussionId, canvasDiscussion);
				
				log.log("New DBoard: " + discussionHandler.toString() + "; Total Boards: " + discussionHandler.canvas_getSize() + "; id: " + discussionId);
			}
			
			

			String socketId = getAlphaNumericString(TOKEN_LENGTH);
			//make sure no repeat
			while(websocketHandler.containSocket(socketId)) {
				socketId = getAlphaNumericString(TOKEN_LENGTH);
			}
			
			log.log("New Socket: "+ socketId);
			
			WebSocket newWebsocket = new WebSocket(log, clock, token, discussionUrl, server, tokenHandler, socketId, discussionHandler, websocketHandler, accountUserId);
			
			websocketHandler.addWebsocket(socketId, newWebsocket);
			
			token += "-" + socketId + "-" + accountUserId;
			
		}else {
			token = "0-urlNotDiscussion"; //needs to be a discussion topic
		}
		
		
		return token;
	}
	//#endregion
	
	//#region simple pages
	private byte[] getFavicon() {
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
			
			 return fileData;
		}catch (IOException e){
	    	log.error("Requested for favicon.ico, still didn't implement");		
	    	return "error".getBytes();        		
		}
	}
	//#endregion
	
	//#region helper functions
	private HashMap<String, String> parseUrlParameter(String urlParameter){
		String[] parametersArray = urlParameter.split("&");
		HashMap<String, String> returnParameters = new HashMap<>();
		for(int i = 0; i < parametersArray.length; i++) {
			int equalLoc = parametersArray[i].indexOf("=");
			if(equalLoc > 0) {
				String key = parametersArray[i].substring(0, equalLoc);
				String value = parametersArray[i].substring(equalLoc + 1);
				
				returnParameters.put(key, value);
			}else {
				return null;
			}
		}
		return returnParameters;
	}
	//#endregion
	
	
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
}
