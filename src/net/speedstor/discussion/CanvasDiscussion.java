package net.speedstor.discussion;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.speedstor.control.Log;
import net.speedstor.control.Settings;
import net.speedstor.main.Cache;
import net.speedstor.main.TokenHandler;
import net.speedstor.network.Network;
import net.speedstor.websocket.WebSocketHandler;

public class CanvasDiscussion implements Runnable{
	Log log;
	String url;
	Clock clock;
	TokenHandler tokenHandler;
	String backupToken;
	WebSocketHandler websocketHandler;
	Cache cache;
	ArrayList<String> participantList = new ArrayList<String>();
	boolean running;
	
	HashMap<String, Integer> previousAmount = new HashMap<>();
	
	JSONObject discussionJson = new JSONObject();	
	Boolean initialized = false;
	
	JSONObject newContent = new JSONObject();

    JSONParser parser = new JSONParser();
		
	public CanvasDiscussion(Log log, String url, Clock clock, TokenHandler tokenHandler, String backupToken, WebSocketHandler websocketHandler, Cache cache) {
		this.log = log;
		this.url = url.replace("/view", "");
		this.clock = clock;
		this.tokenHandler = tokenHandler;
		this.backupToken = backupToken; //first user's server token
		this.websocketHandler = websocketHandler;
		this.cache = cache;

		discussionJson.put("topic", "");
		discussionJson.put("title", "");
		discussionJson.put("online", new JSONArray());
		/*
		try {
			discussionJson = (JSONObject) parser.parse(lackedJson);
			log.log(discussionJson.toJSONString());
			log.warn(""+((JSONObject) discussionJson.get("view")).containsKey("4480"));
			previousAmount.put("numOfTopics", 34);
		} catch (ParseException e) {
			e.printStackTrace();
		}*/
	}

	public boolean inUpdating = false;
	
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		running = true;
		long bufferTime = 0;
		while(running) {
			//get canvas discussion json and check for change
				//check number of discussion topics
				//check number of replies in each topic 
			//check every 5 seconds
			if(clock.millis() - bufferTime > Settings.SYNC_CANVAS_FREQ) {
				bufferTime = clock.millis();
				
				//will not update json if there is a new topic or reply being added
				while(inUpdating) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						log.error("Error in sleeping thread");
					}
				}
				
				updateJson();
				initialized = true;
				
				
				//dispatch changes to clients		
				if(!newContent.isEmpty()) {
					log.log("newContent: "+newContent.toJSONString());
					if(newContent.containsKey("topics")) {
						((JSONArray) newContent.get("topics")).forEach((messageObject) -> {							
							sendToAll("{\"post\": "+((JSONObject) messageObject).toJSONString()+"}");
						});
					}
					
					if(newContent.containsKey("newReplies")) {
						((JSONArray) newContent.get("newReplies")).forEach((replyObject) -> {							
							sendToAll("{\"reply\": "+((JSONObject) replyObject).toJSONString()+"}");
						});
					}
					//update
					newContent = new JSONObject();
				}				
			}		
			
			try {
				Thread.sleep((long) (Settings.LISTEN_SPEED_INTERVAL*2.5));
			} catch (InterruptedException e) {
				//e.printStackTrace();
				log.error("outStream thread sleep 2500ms interrupted");
			}
			
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
	
	public void callUpdateJson() {
		updateJson();
	}
	
	@SuppressWarnings("unchecked")
	public String sendNewMessage(String socketId, String content) {
		if(!Settings.DISCONNECT_FROM_CANVAS) {			
			inUpdating = true;
			String response = Network.sendPostWithBody(url+"/entries"+"?access_token="+tokenHandler.tokenDB_getCanvasTokenFromSocket(socketId), "{\"message\": \""+content+"\"}");
			try {
				JSONObject entryJson = (JSONObject) parser.parse(response);
				String postId = entryJson.get("id")+"";
				//send what is a new post
				if(postId != null) {
					((JSONObject) discussionJson.get("view")).put(postId, entryJson);			
					sendToAllExclude(socketId, "{\"post\": "+entryJson.toJSONString()+"}");
				}
				inUpdating = false;
				return response;
			} catch (ParseException e) {
				inUpdating = false;
				log.error("parse for new topic id error");
				return "0-server sync with Canvas error";
			}
		}else {
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
	}
	
	@SuppressWarnings("unchecked")
	public String sendNewReply(String socketId, String replyTo, String content) {
		inUpdating = true;

		String response;
		if(!Settings.DISCONNECT_FROM_CANVAS) {			
			response = Network.sendPostWithBody(url+"/entries/"+replyTo+"/replies?access_token="+tokenHandler.tokenDB_getCanvasTokenFromSocket(socketId), "{\"message\": \""+content+"\"}");
			try {
				JSONObject entryJson = (JSONObject) parser.parse(response);
				String postId = (String) entryJson.get("id")+"";
				//send what is a new post
				if(postId != null) {
					if(((JSONObject) ((JSONObject) discussionJson.get("view")).get(replyTo)).containsKey("replies")) {
						((JSONArray) ((JSONObject) ((JSONObject) discussionJson.get("view")).get(replyTo)).get("replies")).add(entryJson);	
					}else {
						JSONArray newReply = new JSONArray();
						newReply.add(entryJson);
						((JSONObject) ((JSONObject) discussionJson.get("view")).get(replyTo)).put("replies", newReply);
					}
					sendToAllExclude(socketId, "{\"reply\": "+entryJson.toJSONString()+"}");
				}
				inUpdating = false;
				return response;
			} catch (ParseException e) {
				inUpdating = false;
				log.error("parse for new topic id error");
				return "0-server sync with Canvas error";
			}
		}else {
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
	}
	
	public int stopBoard() {
		running = false;
		return 1;
	}
	
	public String getUrl() {
		return url;
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
	
	public int addParticipant(String socketId) {
		participantList.add(socketId);
		String userId = tokenHandler.tokenDB_get(tokenHandler.socketList_get(socketId))[2];
		JSONArray online = (JSONArray) discussionJson.get("online");
		if(!online.contains(userId)) {
			((JSONArray) discussionJson.get("online")).add(userId);
			sendToAllExclude(socketId, "{\"online\": {\"user_id\": \""+userId+"\"}}");
		}
		log.log("DiscussionBoardSize: "+participantList.size() + "; url="+url);
		//websocketHandler.get(token).sendUnmask(discussionJson.toJSONString());;
		return 1;
	}
	
	public boolean removeParticipant(String socketId) {
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
	
	public int getPariticipantSize() {
		return participantList.size();
	}
	
	public String[] getParticipantList() {
		return participantList.toArray(new String[0]);
	}
	
	public void setTopic(String topic) {
		discussionJson.put("topic", topic);
	}
	
	public void setTitle(String title) {
		discussionJson.put("title", title);
	}
	
	public void updateProfileImage(String participantId, String imageUrl) {
		if(((JSONObject) discussionJson.get("participants")).containsKey(participantId)) {
			((JSONObject) ((JSONObject) discussionJson.get("participants")).get(participantId)).put("avatar_image_url", imageUrl);
		}
	}

	@SuppressWarnings("unchecked")
	private void updateJson() {
	
		String randomToken;
		if(participantList.size() > 0) {
			randomToken = tokenHandler.tokenDB_getCanvasTokenFromSocket(participantList.get((int) (Math.random() * participantList.size())));
		}else {
			randomToken = tokenHandler.tokenDB_get(backupToken)[0];
		}
		//get the discussion board json from a random user's token
		while(inUpdating) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {
				log.warn("wait for inUpdating interrupted");
			}
		}
		inUpdating = true;
		String discussionJsonString = Network.sendGet(url+"/view?include_new_entries=1&order_by=recent_activity&include_enrollment_state=1&include_context_card_info=1&access_token="+randomToken);
		inUpdating = false;
		
		if(discussionJsonString != "error: catch error") {
			JSONObject convJson;
			try {
	            convJson = (JSONObject) parser.parse(discussionJsonString);
	            
	            if(!discussionJson.containsKey("participants")) {
	            	if(convJson.containsKey("participants")) {
	            		JSONObject participantsSub = new JSONObject();
	            		((JSONArray) convJson.get("participants")).forEach((participantObject) -> {
	            			JSONObject participantJSON = (JSONObject) participantObject;
	            			String participantId = ""+participantJSON.get("id");
	            			if(cache.categoryContainKey("profileImgs", participantId)) {
            					participantJSON.put("avatar_image_url", cache.getCachedData("profileImgs", participantId));
	            			}
	            			participantsSub.put(participantId, (JSONObject) participantJSON);
	            		});
	            		discussionJson.put("participants", participantsSub);
	            	}
	            }
	            
	            if(!discussionJson.containsKey("view")) {
	            	
	            }
			
				JSONArray viewSub;
				if((viewSub = (JSONArray) convJson.get("view")) != null) {
					//check size of topics and if it had increased
					if(!discussionJson.containsKey("view")) {
						JSONObject viewJson = new JSONObject();
						viewSub.forEach((x) -> {
							viewJson.put(""+((JSONObject) x).get("id"), x);
						});
						discussionJson.put("view", viewJson);
					}else {
					
						if(!previousAmount.containsKey("numOfTopics")) {
							previousAmount.put("numOfTopics", viewSub.size());
						}else {
							if(previousAmount.get("numOfTopics") != viewSub.size()) {
								//new stuff
								JSONArray newContentJson = new JSONArray();
								viewSub.forEach((x) -> {
									if(!((JSONObject) discussionJson.get("view")).containsKey(""+ ((JSONObject) x).get("id"))) {
										newContentJson.add(((JSONObject) x).get("id"));
										((JSONObject) discussionJson.get("view")).put(""+((JSONObject) x).get("id"), x);
									};
								});
								newContent.put("topics", newContentJson);
								previousAmount.put("numOfTopics", viewSub.size());
							}
						}
					
						JSONObject topicWrap = new JSONObject();
						//go through each topic and check if replies had increased
						viewSub.forEach((topicFromNew) -> {
							JSONArray newReplyForTopic = new JSONArray();
							
	
							JSONObject topicFromOld = (JSONObject) ((JSONObject) discussionJson.get("view")).get(""+((JSONObject) topicFromNew).get("id"));
							JSONArray repliesFromOld = (JSONArray) topicFromOld.get("replies");
							JSONArray repliesFromNew = (JSONArray) ((JSONObject) topicFromNew).get("replies");
							
							if(repliesFromNew != null) {
								if(repliesFromOld == null) {
									topicWrap.put(((JSONObject) topicFromNew).get("id"), repliesFromNew);
									((JSONObject) ((JSONObject) discussionJson.get("view")).get(""+((JSONObject) topicFromNew).get("id"))).put("replies", repliesFromNew);
								}else {
									if(repliesFromNew.size() != repliesFromOld.size()) {
										repliesFromNew.forEach((replyObject)->{
											int id = Math.toIntExact((Long) ((JSONObject) replyObject).get("id"));
											
											HashMap<String, Boolean> store = new HashMap<>();
											store.put("ifNew", true);
											
											repliesFromOld.forEach((oldReplyObject) -> {
												int oldId = Math.toIntExact(((Long) ((JSONObject) oldReplyObject).get("id")));
												if( oldId == id ) {
													store.put("ifNew", false);
												}
											});
											if(store.get("ifNew")) {
												newReplyForTopic.add(replyObject);
												//sorry i am too deep into this hole to climb out now
												((JSONArray) ((JSONObject) ((JSONObject) discussionJson.get("view")).get(""+((JSONObject) topicFromNew).get("id"))).get("replies")).add(replyObject);
											}
										});
			
										topicWrap.put(topicFromOld.get("id"), newReplyForTopic);
									}	
								}
							}
						});
					
						if(!topicWrap.isEmpty()) {
							ArrayList<String> topicIds = new ArrayList<String>();
							topicWrap.forEach((k, v) -> {
								topicIds.add((String) k);
							}); 
							newContent.put("newReplies", topicIds.toArray(new String[0]));
						}
					}
				}
			}catch (ParseException e) {
				System.out.println("Error with parsing json string");
			}
			
		}
	}
}
