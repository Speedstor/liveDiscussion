package net.speedstor.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.time.Clock;
import java.util.HashMap;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.speedstor.control.Credentials;
import net.speedstor.control.Log;
import net.speedstor.control.Settings;
import net.speedstor.discussion.CanvasDiscussion;
import net.speedstor.discussion.Discussion;
import net.speedstor.discussion.DiscussionHandler;
import net.speedstor.main.Cache;
import net.speedstor.main.ThreadScripts;
import net.speedstor.main.TokenHandler;
import net.speedstor.network.Network;
import net.speedstor.websocket.WebSocket;
import net.speedstor.websocket.WebSocketHandler;

public class APIFunctions {
	Log log;
	Cache cache;
	APIServer apiServer;
	WebSocketHandler websocketHandler;
	TokenHandler tokenHandler;
	Clock clock;
	DiscussionHandler discussionHandler;
	
	//token for communication between server and client, not using canvas token
	final static int TOKEN_LENGTH = 15;
	
	JSONParser parser = new JSONParser();
	
	public APIFunctions(Log log, APIServer apiServer, WebSocketHandler websocketHandler, TokenHandler tokenHandler, DiscussionHandler discussionHandler, Clock clock, Cache cache) {
		this.log = log;
		this.apiServer = apiServer;
		this.websocketHandler = websocketHandler;
		this.tokenHandler = tokenHandler;
		this.discussionHandler = discussionHandler;
		this.clock = clock;
		this.cache = cache;
	}
	
	//#region independent from canvas
	public String createDiscussion() {
		String discussionId;
		discussionId = ""+(100000+(new Random()).nextInt(899999));

		//create discussion board
		Discussion discussion = new Discussion(log, clock, tokenHandler, websocketHandler, cache);
		discussionHandler.put(discussionId, discussion);
		
		log.log("New DBoard: " + discussion.toString() + "; Total Boards: " + discussionHandler.getSize() + "; id: Discussion-" + discussionId);
		
		return discussionId;
	}
	
	public String newUser(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "{\"error\": \"parameter error\"}";
		
		if(!parameters.containsKey("discussionId")) return "{\"error\": \"noId\"}";
		if(!discussionHandler.containKey((parameters.get("discussionId")))) return "{\"error\": \"invalid id\"}";
		
		String socketId = getAlphaNumericString(TOKEN_LENGTH);
		while(tokenHandler.socketList_containKey(socketId))  socketId = getAlphaNumericString(TOKEN_LENGTH);

		String accountId;
		if(parameters.containsKey("accountId") && tokenHandler.tokenDB_get(parameters.get("accountId")) != null) {
			accountId = parameters.get("accountId");
		}else {			
			accountId = getAlphaNumericString(TOKEN_LENGTH); //serverToken
			while(tokenHandler.tokenDB_containKey(accountId))  accountId = getAlphaNumericString(TOKEN_LENGTH);
			while(tokenHandler.socketList_containValue(accountId))  accountId = getAlphaNumericString(TOKEN_LENGTH);
			tokenHandler.addToken(accountId, "null", "null", accountId, "independentFromCanvas");
		}
		
		tokenHandler.socketList_put(socketId, accountId);
		
		return "{\"socketId\": \""+socketId+"\", \"accountId\": \""+accountId+"\"}";
	}
	
	public String join(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "0-parameter error";

		if(!parameters.containsKey("socketId")) return "{\"error\": \"need socketId\"}";
		if(!parameters.containsKey("userName")) return "{\"error\": \"need userName\"}";
		
		String socketId = parameters.get("socketId");
		String serverId = websocketHandler.get(socketId).getUserId();
		String userName = parameters.get("userName");
		
		Discussion discussion = discussionHandler.get(websocketHandler.get(socketId).getDiscussionId());
		discussion.addParticipant(serverId, userName);		
		return "{\"success\": true}";
	}
	//#endregion
	
	//#region oauth login
	public String checkLogin(String urlParameter) {
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

	public String oauthComplete(String urlParameter) {
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

	public String getLoginUrl() {
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
	public String listDiscussions(String urlParameter) {
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
					discussionList.put(courseId, discussionObject);
				} catch (ParseException e) {
					log.error("parse json for listing discussion error");
				}

			});
		} catch (ParseException e) {
			log.error("parse json for listing courses error 2");
			return "0-server parse json error";
		}
		
		cache.putData("discussionLists", canvasToken, discussionList.toJSONString());
		
		return discussionList.toJSONString();
	}
	
	public String changeUserName(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "{\"error\": \"parameter error\"}";
		if(!parameters.containsKey("accountId"))  return "{\"error\": \"need accountId\"}";
		if(!parameters.containsKey("userId"))  return "{\"error\": \"need discussionId\"}";
		if(!parameters.containsKey("newUserName"))  return "{\"error\": \"need newUserName\"}";
		
		String discussionId = parameters.get("discussionId");
		String userId = parameters.get("userId");
		Discussion focusedDiscussion = discussionHandler.get(discussionId);
		if(focusedDiscussion == null)  return "{\"error\": \"invalid discussionId\"}";
		if(!focusedDiscussion.hasParticipant(userId)) return "{\"error\": \"don't have access to discussion\"}";
		
		focusedDiscussion.changeParticipantName(userId, parameters.get("newUserName"));
		return "{\"success\": true}";
	}
	
	public String changeDiscussionTitle(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "{\"error\": \"parameter error\"}";
		if(!parameters.containsKey("accountId"))  return "{\"error\": \"need accountId\"}";
		if(!parameters.containsKey("discussionId"))  return "{\"error\": \"need discussionId\"}";

		String discussionId = parameters.get("discussionId");
		String userId = parameters.get("accountId");
		Discussion focusedDiscussion = discussionHandler.get(discussionId);
		if(focusedDiscussion == null)  return "{\"error\": \"invalid discussionId\"}";
		if(!focusedDiscussion.hasParticipant(userId)) return "{\"error\": \"don't have access to discussion\"}";
		
		try {
			focusedDiscussion.setTitle(URLDecoder.decode(parameters.get("newDiscussionTitle"), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "{\"success\": true}";		
	}
	
	public String changeDiscussionTopic(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "{\"error\": \"parameter error\"}";
		if(!parameters.containsKey("accountId"))  return "{\"error\": \"need accountId\"}";
		if(!parameters.containsKey("discussionId"))  return "{\"error\": \"need discussionId\"}";
		if(!parameters.containsKey("newDiscussionTopic"))  return "{\"error\": \"need newDiscussionTopic\"}";

		String discussionId = parameters.get("discussionId");
		String userId = parameters.get("accountId");
		Discussion focusedDiscussion = discussionHandler.get(discussionId);
		if(focusedDiscussion == null)  return "{\"error\": \"invalid discussionId\"}";
		if(!focusedDiscussion.hasParticipant(userId)) return "{\"error\": \"don't have access to discussion\"}";

		try {
			focusedDiscussion.setTopic(URLDecoder.decode(parameters.get("newDiscussionTopic"), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "{\"success\": true}";
	}
	
	public String leaveDiscussion(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "{\"error\": \"parameter error\"}";
		if(!parameters.containsKey("accountId"))  return "{\"error\": \"need accountId\"}";
		if(!parameters.containsKey("discussionId"))  return "{\"error\": \"need discussionId\"}";
		
		String discussionId = parameters.get("discussionId");
		String userId = parameters.get("accountId");
		Discussion focusedDiscussion = discussionHandler.get(discussionId);
		if(focusedDiscussion == null)  return "{\"error\": \"invalid discussionId\"}";
		if(!focusedDiscussion.hasParticipant(userId)) return "{\"error\": \"don't have access to discussion\"}";
		
		focusedDiscussion.removeParticipant(userId);
		return "{\"success\": true}";
	}

	//important, undescriptive name, this one is only for canvas discussions
	public String initDiscussion(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "{\"error\": \"parameter error\"}";

		if(!parameters.containsKey("socketId")) return "{\"error\": \"must need socketId\"}";
		if(!parameters.containsKey("id")) return "{\"error\": \"must need discussion id\"}";
		if(!tokenHandler.socketList_containKey(parameters.get("socketId"))) return "{\"error\": \"invalid socketId\"}";
		
		String socketId = parameters.get("socketId");
		String serverToken = tokenHandler.socketList_get(socketId);
		String canvasToken = tokenHandler.tokenDB_get(serverToken)[0];
		
		boolean hasParticipant = false;
		
		String discussionId = parameters.get("id");
		if(discussionId.contains("v")) {
			String[] discussionIds = parameters.get("id").split("v");
			if(discussionIds.length != 2) return "{\"error\": \"invalid discussion id\"}";
			String courseId = discussionIds[0];
			String canvasDiscussionId = discussionIds[1];

			String discussionUrl = Settings.API_URL+"/courses/"+courseId+"/discussion_topics/"+canvasDiscussionId;
			
			//check accessibility of user
			String testDiscussionResponse = Network.sendGet(discussionUrl+"?access_token="+canvasToken);
			if(testDiscussionResponse == "error: catch error") {
				return "{\"error\": \"lack permission to discussion || id doesn't exists on canvas server\"}";
			}
			discussionUrl += "/view";
			
			if(!discussionHandler.containKey(discussionId)) {
				CanvasDiscussion canvasDiscussion = new CanvasDiscussion(log, discussionUrl, clock, tokenHandler, serverToken, websocketHandler, cache);
				
				try {
					JSONObject discussion = (JSONObject) parser.parse(testDiscussionResponse);
					canvasDiscussion.setTopic((String) discussion.get("message"));
					canvasDiscussion.setTitle((String) discussion.get("title"));
				} catch (ParseException e) {
					log.error("Error parsing discussion");
				}
				
				Thread discussionHandlerThread = new Thread(canvasDiscussion);
				discussionHandlerThread.start();
				discussionHandler.put(discussionId, canvasDiscussion);
				
				log.log("New DBoard: " + canvasDiscussion.toString() + "; Total Boards: " + discussionHandler.getSize() + "; id: Canvas-" + discussionId);
			}
			hasParticipant = true;
		}else {
			if(!discussionHandler.containKey(discussionId)) return "{\"error\": \"invalid discussion id\"}";
			
			hasParticipant = discussionHandler.get(discussionId).hasParticipant(serverToken);
		}

		WebSocket newWebsocket = new WebSocket(log, clock, discussionId, apiServer, tokenHandler, socketId, discussionHandler, websocketHandler, tokenHandler.tokenDB_get(serverToken)[2]);		
		websocketHandler.addWebsocket(socketId, newWebsocket);
		
		return "{\"success\": true, \"userId\": \""+tokenHandler.tokenDB_get(serverToken)[2]+"\", \"hasParticipant\": "+hasParticipant+"}";
	}
	
	public String requestDiscussionJson(String urlParameter) {
		HashMap<String, String> parameters = parseUrlParameter(urlParameter);
		if(parameters == null)  return "0-parameter error";

		if(!parameters.containsKey("socketId")) return "0-must need token";
		if(!parameters.containsKey("id")) return "0-must need discussion id";
		
		if(tokenHandler.socketList_containKey(parameters.get("socketId"))) {
			//verified logged in
			Discussion focusedDiscussionHandler = discussionHandler.get(parameters.get("id"));
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

	public String sendMessage(String urlParameter, BufferedReader inFromClient) {
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
		
		if(parameters.get("discussionId").contains("v")) {
			CanvasDiscussion canvasDiscussion = (CanvasDiscussion) discussionHandler.get(parameters.get("discussionId"));		
			return canvasDiscussion.sendNewMessage(parameters.get("socketId"), message);
		}else {
			Discussion discussion = discussionHandler.get(parameters.get("discussionId"));		
			return discussion.sendNewMessage(parameters.get("socketId"), message);
		}
	}
	
	public String sendReply(String urlParameter, BufferedReader inFromClient) {
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
		

		if(parameters.get("discussionId").contains("v")) {
			CanvasDiscussion canvasDiscussion = (CanvasDiscussion) discussionHandler.get(parameters.get("discussionId"));		
			return canvasDiscussion.sendNewReply(parameters.get("socketId"), replyTo, message);
		}else {
			Discussion discussion = discussionHandler.get(parameters.get("discussionId"));		
			return discussion.sendNewReply(parameters.get("socketId"), replyTo, message);
		}
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
	public String tokenLogin(String urlParameter) {
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
				accountUserId = ((JSONObject) ((JSONArray) ((JSONObject) courses.get(1)).get("enrollments")).get(0)).get("user_id")+"";
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
	
	public String loginUser(String parametersString) {
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
			
			if(discussionHandler.containKey(discussionUrl)) {
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
				
				discussionHandler.put(discussionId, canvasDiscussion);
				
				log.log("New DBoard: " + discussionHandler.toString() + "; Total Boards: " + discussionHandler.getSize() + "; id: " + discussionId);
			}
			
			

			String socketId = getAlphaNumericString(TOKEN_LENGTH);
			//make sure no repeat
			while(websocketHandler.containSocket(socketId)) {
				socketId = getAlphaNumericString(TOKEN_LENGTH);
			}
			
			log.log("New Socket: "+ socketId);
			
			WebSocket newWebsocket = new WebSocket(log, clock, token, discussionUrl, apiServer, tokenHandler, socketId, discussionHandler, websocketHandler, accountUserId);
			
			websocketHandler.addWebsocket(socketId, newWebsocket);
			
			token += "-" + socketId + "-" + accountUserId;
			
		}else {
			token = "0-urlNotDiscussion"; //needs to be a discussion topic
		}
		
		
		return token;
	}
	//#endregion
	
	//#region simple pages
	public byte[] getFavicon() {
		try {
    		File path = new File(Settings.WEB_FOLDER_PATH+"/favicon.ico");

    		return Files.readAllBytes(path.toPath());
		}catch (Exception e){
	    	log.error("Requested for favicon.ico, file doesn't exists");		
	    	return "error".getBytes();        		
		}
	}
	//#endregion
	
	//#region helper functions
	public HashMap<String, String> parseUrlParameter(String urlParameter){
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
