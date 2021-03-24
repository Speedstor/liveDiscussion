package net.speedstor.discussion;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import net.speedstor.control.Log;
import net.speedstor.control.Settings;
import net.speedstor.main.Cache;
import net.speedstor.main.TokenHandler;
import net.speedstor.websocket.WebSocketHandler;

public class Discussion {
	Log log;
	Clock clock;
	TokenHandler tokenHandler;
	WebSocketHandler websocketHandler;
	Cache cache;
	ArrayList<String> participantList = new ArrayList<String>();
	Boolean initialized = false;
	
	JSONObject discussionJson = new JSONObject();

    JSONParser parser = new JSONParser();
		
	public Discussion(Log log, Clock clock, TokenHandler tokenHandler, WebSocketHandler websocketHandler, Cache cache) {
		this.log = log;
		this.clock = clock;
		this.tokenHandler = tokenHandler;
		this.websocketHandler = websocketHandler;
		this.cache = cache;
		
		discussionJson.put("topic", "The thing you want the discussion to evolve around.");
		discussionJson.put("title", "Untitled Discussion Room");
		discussionJson.put("online", new JSONArray());
		discussionJson.put("participants", new JSONObject());
		discussionJson.put("view", new JSONObject());
		
		initialized = true;
	}

	//TODO: store the sync messages for new websockets
	public void sendToAllExclude(String excludeSocket, String payload) {
		if(excludeSocket != null) {
			for(int i=0; i < participantList.size(); i++) {
				String participant = participantList.get(i);
				if(!excludeSocket.equals(participant)) {
					websocketHandler.get(participant).sendUnmaskThread(payload);
				}
			}		
		}else {
			sendToAll(payload);
		}
	}
	
	public void sendToAll(String payload) {
		for(int i=0; i < participantList.size(); i++) {
			websocketHandler.get(participantList.get(i)).sendUnmaskThread(payload);
		}
	}


	
	public void sendSync(String excludeSocket, String jsonString) {
		/*{
		 * 	"sync": {
		 *				"replyTo": "n"/"4339",
		 *				"senderId": "450",
		 *				"content": messageHtmlString
		 *          }
		 * }*/
		
		sendToAllExclude(excludeSocket, jsonString);
	}
		
	public String getDiscussionJson() {
		return initialized ? discussionJson.toJSONString() : null;
	}

	@SuppressWarnings("unchecked")
	public String sendNewMessage(String socketId, String content) {
		JSONObject entryJson = new JSONObject();
		int postId = (int) (Math.random()*8999 + 1000);
		while(((JSONObject) discussionJson.get("view")).containsKey(""+postId)) postId = (int) (Math.random()*8999 + 1000);
		entryJson.put("id", postId);
		entryJson.put("updated_at", Instant.now().toString());
		entryJson.put("created_at", Instant.now().toString());
		entryJson.put("user_id", tokenHandler.tokenDB_get(tokenHandler.socketList_get(socketId))[2]);
		entryJson.put("message", content);
		entryJson.put("rating_count", null);
		entryJson.put("rating_sum", null);
		entryJson.put("parent_id", null);
		
		((JSONObject) discussionJson.get("view")).put(postId+"", entryJson);				
		sendToAllExclude(socketId, "{\"post\": "+entryJson.toJSONString()+"}");
		return entryJson.toJSONString();
	}

	@SuppressWarnings("unchecked")
	public String sendNewReply(String socketId, String replyTo, String content) {
		JSONObject entryJson = new JSONObject();
		int replyId = (int) (Math.random()*8999 + 1000);
		
		//check if repyId exists
		if(((JSONObject) ((JSONObject) discussionJson.get("view")).get(replyTo)).containsKey("replies")) {
			JSONArray replyArray = ((JSONArray) ((JSONObject) ((JSONObject) discussionJson.get("view")).get(replyTo)).get("replies"));
			ArrayList<Boolean> validId = new ArrayList<>();
			validId.add(false);
			while(validId.get(0)) {
				validId.remove(0);
				validId.add(false);
				final int replyIdTemp = replyId;
				replyArray.forEach((replyObject) -> {
					if((replyIdTemp+"").equals(((JSONObject) replyObject).get("id")+"")) {
						validId.remove(0);
						validId.add(true);
					}
				});
				if(validId.get(0)) break;
				replyId = (int) (Math.random()*8999 + 1000);
			}
		}
		
		entryJson.put("id", replyId);
		entryJson.put("updated_at", Instant.now().toString());
		entryJson.put("created_at", Instant.now().toString());
		entryJson.put("user_id", tokenHandler.tokenDB_get(tokenHandler.socketList_get(socketId))[2]);
		entryJson.put("message", content);
		entryJson.put("rating_count", null);
		entryJson.put("rating_sum", null);
		entryJson.put("parent_id", replyTo);

		if(((JSONObject) ((JSONObject) discussionJson.get("view")).get(replyTo)).containsKey("replies")) {
			((JSONArray) ((JSONObject) ((JSONObject) discussionJson.get("view")).get(replyTo)).get("replies")).add(entryJson);	
		}else {
			JSONArray newReply = new JSONArray();
			newReply.add(entryJson);
			((JSONObject) ((JSONObject) discussionJson.get("view")).get(replyTo)).put("replies", newReply);
		}
		sendToAllExclude(socketId, "{\"reply\": "+entryJson.toJSONString()+"}");
		return entryJson.toJSONString();
	}
	
	public int addParticipant(String userId, String userName) {
		log.debug("wroking till now");
		JSONObject participantObject = new JSONObject();
		participantObject.put("avatar_image_url", Settings.DEFAULT_USER_IMAGE_PATH);
		participantObject.put("course_id", "null");
		participantObject.put("display_name", userName);
		participantObject.put("html_url", "null");
		participantObject.put("id", userId);
		participantObject.put("is_student", false);
		participantObject.put("pronouns", null);
		
		((JSONObject) discussionJson.get("participants")).put(userId, participantObject);
		sendToAll("{\"newParticipant\": "+participantObject.toJSONString()+"}");
		return 1;
	}
	
	public int setOnline(String socketId) {
		participantList.add(socketId);
		String userId = tokenHandler.tokenDB_get(tokenHandler.socketList_get(socketId))[2];
		JSONArray online = (JSONArray) discussionJson.get("online");
		if(!online.contains(userId)) {
			((JSONArray) discussionJson.get("online")).add(userId);
			sendToAllExclude(socketId, "{\"online\": {\"user_id\": \""+userId+"\"}}");
		}
		log.log("DiscussionBoardSize: "+participantList.size() + ";");
		//websocketHandler.get(token).sendUnmask(discussionJson.toJSONString());;
		return 1;
	}
	
	public boolean setOffline(String socketId) {
		if(participantList.contains(socketId)) {
			while(participantList.remove(socketId));
			
			String userId = tokenHandler.tokenDB_get(tokenHandler.socketList_get(socketId))[2];
			boolean stillOnline = false;
			for(int i = 0; i < participantList.size(); i++) {
				if(tokenHandler.tokenDB_get(tokenHandler.socketList_get(participantList.get(i)))[2].equals(userId)) {
					stillOnline = true;
				}
			}
			if(!stillOnline) {				
				JSONArray online = (JSONArray) discussionJson.get("online");
				if(online.contains(userId)) {
					((JSONArray) discussionJson.get("online")).remove(userId);
					sendToAllExclude(socketId, "{\"offline\": {\"user_id\": \""+userId+"\"}}");
				}
			}
			return true;
		}else {
			return false;
		}
	}
	public int changeParticipantName(String userId, String newName) {
		if(!hasParticipant(userId)) return -1;
		((JSONObject) ((JSONObject) discussionJson.get("participants")).get(userId)).put("display_name", newName);
		sendToAll("{\"newParticipantName\": {\"user_id\": \""+userId+"\", \"display_name\": \""+newName+"\"}}");
		return 1;
	}
	
	public int removeParticipant(String userId) {
		((JSONObject) discussionJson.get("participants")).remove(userId);
		sendToAll("{\"removeParticipant\": \""+userId+"\"}");
		return 1;
	}
	
	public boolean hasParticipant(String userId) {
		return ((JSONObject) discussionJson.get("participants")).containsKey(userId);
	}
	
	public int getPariticipantSize() {
		return participantList.size();
	}
	
	public String[] getParticipantList() {
		return participantList.toArray(new String[0]);
	}
	
	public void setTopic(String topic) {
		discussionJson.put("topic", topic);
		sendToAll("{\"newTopic\": \""+topic+"\"}");
	}
	
	public void setTitle(String title) {
		discussionJson.put("title", title);
		sendToAll("{\"newTitle\": \""+title+"\"}");
	}
	
	public void updateProfileImage(String participantId, String imageUrl) {
		if(((JSONObject) discussionJson.get("participants")).containsKey(participantId)) {
			((JSONObject) ((JSONObject) discussionJson.get("participants")).get(participantId)).put("avatar_image_url", imageUrl);
		}
	}
		
	public String getName() {
		return getTitle();
	}
	
	public String getTitle() {
		return (String) discussionJson.get("title");
	}
	
	public JSONObject getJson() {
		return discussionJson;
	}
	

}
