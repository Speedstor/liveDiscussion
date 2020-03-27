package net.speedstor.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DiscussionHandler implements Runnable{
	Log log;
	String url;
	Clock clock;
	TokenHandler tokenHandler;
	String backupToken;
	WebSocketHandler websocketHandler;
	ArrayList<String> participantList = new ArrayList<String>();
	boolean running;
	
	HashMap<String, Integer> previousAmount = new HashMap<>();
	
	JSONObject discussionJson = new JSONObject();	
	
	JSONObject newContent = new JSONObject();

    JSONParser parser = new JSONParser();
	
	long canvasUpdateFreq = 13000;
	
	public DiscussionHandler(Log log, String url, Clock clock, TokenHandler tokenHandler, String backupToken, WebSocketHandler websocketHandler) {
		this.log = log;
		this.url = url;
		this.clock = clock;
		this.tokenHandler = tokenHandler;
		this.backupToken = backupToken; //first user's token
		this.websocketHandler = websocketHandler;
		
		
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
	
	@Override
	public void run() {
		running = true;
		long bufferTime = 0;
		while(running) {
			//get canvas discussion json and check for change
				//check number of discussion topics
				//check number of replies in each topic 
			//check every 5 seconds
			if(clock.millis() - bufferTime > canvasUpdateFreq) {
				bufferTime = clock.millis();
				inUpdating = true;
				updateJson();
				
				
				//----   done through direct functions update
				//listen for posting new discussion
				//updateJson
				
				//listen for new half messages from each websocket
				//-----
				
				//dispatch changes to clients
					//new discussions
					//new replies
					//new unfinished messages
				//only dispatch new changes --> my approach, maybe better to send whole new 			
				if(!newContent.isEmpty()) {
					log.log("newContent: "+newContent.toJSONString());
					for(int i=0; i < participantList.size(); i++) {
						websocketHandler.get(participantList.get(i)).sendUnmask(newContent.toJSONString());
					}
					//update
					newContent = new JSONObject();
				
				}
				

				inUpdating = false;
			}		
			
		}
	}
	
	public void sendSync(String username, String content) {
		/*sendToAll("{\"sync\": ["
				+ "{\"username\": \""+username+"\", "
				 + "\"content\": \""+content+"\"}]}");
				 */
		
		sendToAll("sync "+username+" "+content+"");
	}

	public void sendToAll(String payload) {
		inUpdating = true;
		for(int i=0; i < participantList.size(); i++) {
			websocketHandler.get(participantList.get(i)).sendUnmask(payload);
		}
		inUpdating = false;
	}
	
	public void callUpdateJson() {
		updateJson();
	}
	
	public void newTopics(String postResponse) {
		//sendToAll("{\"topics\": ["+postResponse+"]}");
	}
	
	public void newReply(String targetTopic, String postResponse) {
		//sendToAll("{\"newReplies\": {\""+targetTopic+"\":["+postResponse+"]}");
	}
	
	@SuppressWarnings("unchecked")
	private void updateJson() {

		String randomToken;
		if(participantList.size() > 0) {
			randomToken = tokenHandler.get(participantList.get((int) (Math.random() * participantList.size())));
		}else {
			randomToken = tokenHandler.get(backupToken);
		}
		//get the discussion board json from a random user's token
		String discussionJsonString = sendGet(url+"?include_new_entries=1&order_by=recent_activity&include_enrollment_state=1&include_context_card_info=1&access_token="+randomToken);
		
		if(discussionJsonString != "error: catch error") {

			JSONObject convJson;
			try {
	            convJson = (JSONObject) parser.parse(discussionJsonString);
	            
	            if(!discussionJson.containsKey("participants")) {
	            	JSONArray participantsSub;
	            	if((participantsSub = (JSONArray) convJson.get("participants")) != null) {
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
							viewJson.put(((JSONObject) x).get("id"), x);
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
										newContentJson.add(x);
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
							newContent.put("newReplies", topicWrap);
						}
					}
				}
			}catch (ParseException e) {
				System.out.println("Error with parsing json string");
			}
			
		}
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
	
	public int stopBoard() {
		running = false;
		return 1;
	}
	
	public String getUrl() {
		return url;
	}
	
	public int addParticipant(String token) {
		participantList.add(token);
		log.log("DiscussionBoardSize: "+participantList.size() + "; url="+url);
		//websocketHandler.get(token).sendUnmask(discussionJson.toJSONString());;
		return 1;
	}
	
	String lackedJson = "{\r\n" + 
			"  \"unread_entries\": [\r\n" + 
			"    4527,\r\n" + 
			"    4528,\r\n" + 
			"    4530,\r\n" + 
			"    4533,\r\n" + 
			"    4565,\r\n" + 
			"    4567,\r\n" + 
			"    4568,\r\n" + 
			"    4573,\r\n" + 
			"    4575,\r\n" + 
			"    4591,\r\n" + 
			"    4596,\r\n" + 
			"    4597,\r\n" + 
			"    4598,\r\n" + 
			"    4599,\r\n" + 
			"    4600,\r\n" + 
			"    4609,\r\n" + 
			"    4611,\r\n" + 
			"    4612,\r\n" + 
			"    4613,\r\n" + 
			"    4614,\r\n" + 
			"    4615,\r\n" + 
			"    4621,\r\n" + 
			"    4622,\r\n" + 
			"    4623,\r\n" + 
			"    4624,\r\n" + 
			"    4625,\r\n" + 
			"    4626,\r\n" + 
			"    4627\r\n" + 
			"  ],\r\n" + 
			"  \"forced_entries\": [],\r\n" + 
			"  \"entry_ratings\": {},\r\n" + 
			"  \"participants\": [\r\n" + 
			"    {\r\n" + 
			"      \"id\": 144,\r\n" + 
			"      \"display_name\": \"Steven Duxbury\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/144\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": false,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 217,\r\n" + 
			"      \"display_name\": \"Ishika Kanakath\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/217\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 225,\r\n" + 
			"      \"display_name\": \"Tuong Dao Cat Nguyen\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/225\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 236,\r\n" + 
			"      \"display_name\": \"Sebastian Giraldo\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/236\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 240,\r\n" + 
			"      \"display_name\": \"Makaila Glynn\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/240\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 244,\r\n" + 
			"      \"display_name\": \"Olga Harrison\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/244\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 263,\r\n" + 
			"      \"display_name\": \"Alora Lindsey\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/263\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 357,\r\n" + 
			"      \"display_name\": \"Martand Bhagavatula\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/357\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 369,\r\n" + 
			"      \"display_name\": \"Yeun Gye Choi\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/369\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 371,\r\n" + 
			"      \"display_name\": \"Amna Shafi\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/371\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 410,\r\n" + 
			"      \"display_name\": \"Ryan Roth\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/410\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 416,\r\n" + 
			"      \"display_name\": \"Nkemdilim Obiamalu\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/416\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 428,\r\n" + 
			"      \"display_name\": \"Myra McCants\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/428\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 444,\r\n" + 
			"      \"display_name\": \"Isabella Park\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/444\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 446,\r\n" + 
			"      \"display_name\": \"Jayu Patel\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/446\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 450,\r\n" + 
			"      \"display_name\": \"Yan Shing Cheung\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/450\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 491,\r\n" + 
			"      \"display_name\": \"Anneliese Silvestre\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/491\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 498,\r\n" + 
			"      \"display_name\": \"Christine Hoang\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/498\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 549,\r\n" + 
			"      \"display_name\": \"Sophia Lander\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/549\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 579,\r\n" + 
			"      \"display_name\": \"Emmaly Nguyen\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/579\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 590,\r\n" + 
			"      \"display_name\": \"Jennifer Cresap\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/590\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 591,\r\n" + 
			"      \"display_name\": \"Dean Alamy\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/591\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 595,\r\n" + 
			"      \"display_name\": \"Natalie Bui\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/595\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 598,\r\n" + 
			"      \"display_name\": \"Sophia Jansen\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/598\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 601,\r\n" + 
			"      \"display_name\": \"Harishri Savdharia\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/601\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 605,\r\n" + 
			"      \"display_name\": \"Hitakshi Savdharia\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/605\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 607,\r\n" + 
			"      \"display_name\": \"Alexandria Kim\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/607\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 608,\r\n" + 
			"      \"display_name\": \"Earl Takeuchi\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/608\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 641,\r\n" + 
			"      \"display_name\": \"Aidan LeGar Daigle\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/641\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 644,\r\n" + 
			"      \"display_name\": \"Heather Anne George\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/644\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 646,\r\n" + 
			"      \"display_name\": \"Nicholas Mikhail\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/646\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 649,\r\n" + 
			"      \"display_name\": \"Kaylin Ong\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/649\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 651,\r\n" + 
			"      \"display_name\": \"Siya Patel\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/651\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 654,\r\n" + 
			"      \"display_name\": \"Lauryn Pettus\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/654\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 657,\r\n" + 
			"      \"display_name\": \"Ved Shivade\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/657\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"is_student\": true,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"id\": 781,\r\n" + 
			"      \"display_name\": \"Test Student\",\r\n" + 
			"      \"avatar_image_url\": \"https://fairmontschools.instructure.com/images/messages/avatar-50.png\",\r\n" + 
			"      \"html_url\": \"https://fairmontschools.instructure.com/courses/232/users/781\",\r\n" + 
			"      \"pronouns\": null,\r\n" + 
			"      \"fake_student\": true,\r\n" + 
			"      \"is_student\": false,\r\n" + 
			"      \"course_id\": \"232\"\r\n" + 
			"    }\r\n" + 
			"  ],\r\n" + 
			"  \"view\": {\r\n" + 
			"    \"4439\": {\r\n" + 
			"      \"id\": 4439,\r\n" + 
			"      \"user_id\": 781,\r\n" + 
			"      \"parent_id\": null,\r\n" + 
			"      \"created_at\": \"2020-03-18T15:49:34Z\",\r\n" + 
			"      \"updated_at\": \"2020-03-18T15:49:34Z\",\r\n" + 
			"      \"rating_count\": null,\r\n" + 
			"      \"rating_sum\": null,\r\n" + 
			"      \"message\": \"<p>Make sure to hit the reply button below the prompt in order to get to the text box to leave and post a reply. </p>\",\r\n" + 
			"      \"replies\": [\r\n" + 
			"        {\r\n" + 
			"          \"id\": 4514,\r\n" + 
			"          \"user_id\": 144,\r\n" + 
			"          \"parent_id\": 4439,\r\n" + 
			"          \"created_at\": \"2020-03-18T17:28:53Z\",\r\n" + 
			"          \"updated_at\": \"2020-03-18T17:28:53Z\",\r\n" + 
			"          \"rating_count\": null,\r\n" + 
			"          \"rating_sum\": null,\r\n" + 
			"          \"message\": \"<p>Well-stated response, Myra...just a note though- the Dawes Plan did NOT reduce the amount owed by Germany. The Young Plan attempted to do that in 1929, but it never became official due to the Great Depression.</p>\"\r\n" + 
			"        }\r\n" + 
			"      ]\r\n" + 
			"    },\r\n" + 
			"    \"4447\": {\r\n" + 
			"      \"id\": 4447,\r\n" + 
			"      \"user_id\": 498,\r\n" + 
			"      \"parent_id\": null,\r\n" + 
			"      \"created_at\": \"2020-03-18T16:09:46Z\",\r\n" + 
			"      \"updated_at\": \"2020-03-18T16:09:46Z\",\r\n" + 
			"      \"rating_count\": null,\r\n" + 
			"      \"rating_sum\": null,\r\n" + 
			"      \"message\": \"<p><i>Initial Discussion Question:</i></p>\\n<p>To what extent did the Locarno Conference ease tensions in 1920s Europe?</p>\\n<ul>\\n<li> More nations in Europe felt at ease as Germany was treated much better at the Locarno conference. Germany had actually agreed and contributed to the agreement rather than being force upon in like the Treaty of Versailles. The signing of the Locarno Pact showed that Germany was starting to be treated as an equal partner in foreign affairs. Also, there was a time of international cooperation, where the Allies had evacuated part of the Rhineland and Germany (Gustav Stresemann) offered to pay France and Belgium for the return of some territory in exchange for all the Allied troops to leave the Rhineland. Even the foreign ministers (Chamberlain, Braind and Stresemann) had received Nobel Peace Prizes. But towards the end of the 1920s, the “spirt of Locarno,�? where things were starting to look up, had dissolved as the foreign ministers had different viewpoints on the Locarno Pact. The Locarno Conference was able to ease the tensions in Europe for a short amount of time, but the three nations (Britain, France and Germany) wanted different things that the nations could not 100% agree upon.<span class=\\\"Apple-converted-space\\\"> </span>\\n</li>\\n</ul>\\n<p> </p>\\n<p><i>Follow Up Discussion Question</i>:</p>\\n<p>To what extent did Gustav Stresemann play a significant role in the mid-to-late 1920s?</p>\\n<ul>\\n<li>He played a significant role in this time period as he was able to contribute to an agreement that was more favorable to the German people. His goal in the Locarno Pact was to improve the relationship between Germany and Britain + France, and to make revisions to the harsh Treaty of Versailles. He was able to achieve some of his goals such as Allied troops withdrawing from the Rhineland. This allowed for moderate Germans to ensure their confidence in Stresemann and the Weimar Republic. But extremist parties like the Nazis and Communists majorly disagreed with the Republic, and saw the Locarno Pact as a betrayal of Germany.<span class=\\\"Apple-converted-space\\\"> </span>\\n</li>\\n</ul>\",\r\n" + 
			"      \"replies\": [\r\n" + 
			"        {\r\n" + 
			"          \"id\": 4450,\r\n" + 
			"          \"user_id\": 144,\r\n" + 
			"          \"parent_id\": 4447,\r\n" + 
			"          \"created_at\": \"2020-03-18T16:18:41Z\",\r\n" + 
			"          \"updated_at\": \"2020-03-18T16:18:41Z\",\r\n" + 
			"          \"rating_count\": null,\r\n" + 
			"          \"rating_sum\": null,\r\n" + 
			"          \"message\": \"<p>You're right Christine...it was a short-lived period of international cooperation. Unfortunately, the Great Depression really destroyed the Spirit of Locarno. But, as you mentioned, their individual agendas didn't help either.</p>\"\r\n" + 
			"        }\r\n" + 
			"      ]\r\n" + 
			"    },\r\n" + 
			"    \"4468\": {\r\n" + 
			"      \"id\": 4468,\r\n" + 
			"      \"user_id\": 444,\r\n" + 
			"      \"parent_id\": null,\r\n" + 
			"      \"created_at\": \"2020-03-18T16:32:19Z\",\r\n" + 
			"      \"updated_at\": \"2020-03-18T16:32:19Z\",\r\n" + 
			"      \"rating_count\": null,\r\n" + 
			"      \"rating_sum\": null,\r\n" + 
			"      \"message\": \"<p><span style=\\\"font-weight: 400;\\\">To what extent did the Locarno Conference ease tensions in 1920s Europe?</span></p>\\n<ul>\\n<li style=\\\"font-weight: 400;\\\"><span style=\\\"font-weight: 400;\\\">The Locarno Conference was a meeting of Britain, France, Germany, Italy, and Belgium. Foreign minister Gustav Stressman believed that this conference could ease tensions between Germany and the other powers as well as slowly getting rid of the strict rules of the Treaty of Versailles. This treaty had three aims: 1. Secure the borders of Europe 2. Demilitarization of the Rhineland 3. Allowing Germany into the League of Nations. Stresemann also believed that by signing this pact, military conflict will lessen. As a result Germany was treated as equal again and was allowed to join the League of nations. This alone already eased tensions between the countries of Europe and Germany. Germany is now seen as equal rather than being dictated like in the Treaty of Versailles. This was also a period of cooperation between the countries. However, the Locarno Conference eased tensions to an extent. Britain and Italy’s role of the guardator was ill defined and there were still some wariness of the countries. Britain was still wary about giving France support in case of another German attack. France was also still insecure and worried about its country. According to British historian, Rush Henig, “From the French point of view...Locarno was a worrying agreement.�? Coming out of the conference, each foreign minister had a different idea of the Locarno. This meant that they weren’t on the same page. </span></li>\\n</ul>\\n<p><span style=\\\"font-weight: 400;\\\"> </span></p>\\n<p><span style=\\\"font-weight: 400;\\\">To what extent did Gustav Stressemann play a significant role in the mid-to-late 1920s?</span></p>\\n<ul>\\n<li style=\\\"font-weight: 400;\\\"><span style=\\\"font-weight: 400;\\\">I believe that Gustav Stressemann played a significant role in the mid-to-late 1920s. One bad thing that came out of the war was hyperinflation. He came up with a new currency called the Rentenmark (backed by gold). Because it's backed by gold, it was actually able to hold its value. The economy improved, helping out the lower and middle class significantly. People were able to get jobs again. This sent Germany into what's known as the Golden Age of the Weimar Republic. Streseman took a similar approach to late Otto Von Bismark. Going away from Weltpolitik, he followed Realpolitik. As a foreign minister, he tried to ease tensions with Europe. He set up the Locarno conference. In addition, he implemented the Dawes plan. The total amount of reparation owed was reduced and extended for another 10 years till 1988. America also gave out loans to Germany and this allowed them to pay off their reparations. German reparations paid to Allies and the Allies paid America. So it's a continuing cycle that helped. He was also awarded the Nobel Peace Prize. So overall Stressemann played a significant role. Other than improving the economy and tensions between the war, he also lowered the views of the extremists. There were more moderants in congress.</span></li>\\n</ul>\\n<p> </p>\",\r\n" + 
			"      \"replies\": [\r\n" + 
			"        {\r\n" + 
			"          \"id\": 4519,\r\n" + 
			"          \"user_id\": 144,\r\n" + 
			"          \"parent_id\": 4468,\r\n" + 
			"          \"created_at\": \"2020-03-18T17:34:59Z\",\r\n" + 
			"          \"updated_at\": \"2020-03-18T17:34:59Z\",\r\n" + 
			"          \"rating_count\": null,\r\n" + 
			"          \"rating_sum\": null,\r\n" + 
			"          \"message\": \"<p>Well-stated response, Bella...good comment pertaining to France and their continued behavior of worrying about Germany and their security on the Continent. You mentioned that the Dawes Plan reduced the amount owed by Germany, but remember that is not the case...the Young Plan attempted to do that, but it never became official due to the Great Depression.</p>\"\r\n" + 
			"        }\r\n" + 
			"      ]\r\n" + 
			"    },\r\n" + 
			"    \"4480\": {\r\n" + 
			"      \"id\": 4480,\r\n" + 
			"      \"user_id\": 491,\r\n" + 
			"      \"parent_id\": null,\r\n" + 
			"      \"created_at\": \"2020-03-18T16:43:21Z\",\r\n" + 
			"      \"updated_at\": \"2020-03-18T16:43:21Z\",\r\n" + 
			"      \"rating_count\": null,\r\n" + 
			"      \"rating_sum\": null,\r\n" + 
			"      \"message\": \"<p>Initial: </p>\\n<p>The Locarno Conference was able to significantly ease tensions during the 1920s in Europe. The Locarno Pact was an agreement between Britain, France, Belgium, Italy, and Germany. The Pact had a few main strategies: securing the borders of countries of Europe, ensuring the permanent demilitarisation of the Rhineland, and allowing Germany into the League of Nations, among other things. Gustav Stresemann, the foreign secretary, signed the Pact as he thought it would reduce military conflict in Europe. The signing of the Locarno Pact displayed that Germany was able to be treated as an equal partner in foreign affairs. The agreement was not like the Treaty of Versailles were it was forced upon Germany. The Locarno Springs pact was much more democratic. </p>\\n<p>Follow-up:</p>\\n<p>Gustav Stresemann played a very significant role in the mid-to-late 1920s. He was the one that believed that a security Pact between Britain and France would pave the way to a more unified Europe. His goals revolved around the fact that is he was able to stabilize the economy and against respect for Germany in regards to foreign affairs, the Germans would be satisfied with the Weimar republic. This would then lead to a surge of support for the moderate party and reduce the level of support for extremists. Through his strategy of reducing hyperinflation, agreeing on the Dawes Plan - which reduces the reparation payments and gave loans to Germany from the US - and calling off passive resistance to the French occupation of the Ruhr - which leads to German economy recovering - he was able to stabilize Germany, reassure ther Germans, and improve the German economy. </p>\",\r\n" + 
			"      \"replies\": [\r\n" + 
			"        {\r\n" + 
			"          \"id\": 4530,\r\n" + 
			"          \"user_id\": 144,\r\n" + 
			"          \"parent_id\": 4480,\r\n" + 
			"          \"created_at\": \"2020-03-18T17:47:10Z\",\r\n" + 
			"          \"updated_at\": \"2020-03-18T17:47:10Z\",\r\n" + 
			"          \"rating_count\": null,\r\n" + 
			"          \"rating_sum\": null,\r\n" + 
			"          \"message\": \"<p>Nice job, Anneliese...pretty amazing accomplishments for Stresemann- he obviously made a huge impact on international relations in the mid-to-late 1920s. </p>\"\r\n" + 
			"        }\r\n" + 
			"      ]\r\n" + 
			"    },\r\n" + 
			"    \"4568\": {\r\n" + 
			"      \"id\": 4568,\r\n" + 
			"      \"user_id\": 608,\r\n" + 
			"      \"parent_id\": null,\r\n" + 
			"      \"created_at\": \"2020-03-18T19:52:01Z\",\r\n" + 
			"      \"updated_at\": \"2020-03-18T19:52:01Z\",\r\n" + 
			"      \"rating_count\": null,\r\n" + 
			"      \"rating_sum\": null,\r\n" + 
			"      \"message\": \"<p><strong><i>Initial Discussion Question:</i></strong></p>\\n<p><strong><i>To what extent did the Locarno Conference ease tensions in 1920s Europe?</i></strong></p>\\n<p><span style=\\\"font-weight: 400;\\\"> </span></p>\\n<p><span style=\\\"font-weight: 400;\\\">Overall, the Locarno Conference was a perfect arrangement for Germany, France, and Britain to gain an equal relationship. This is due to the formation of defined borders for Germany ,with Rhineland demilitarized, and with Britain and France acting as guarantors thus, creating a fair balance. Although each peace prize recipient, being Austen Chamberlain, Aristide Briand, and Gustav Stressman, sought to utilize the Locarno Conference to bolster their respective nations. The arrangement successfully gave Germany a voice in the League of Nations and secured borders to ease Britain and France. </span></p>\\n<p> </p>\\n<p><strong>Follow Up Discussion Question:</strong></p>\\n<p><strong>To what extent did Gustav Stresemann play a significant role in the mid-to-late 1920s?</strong></p>\\n<p> </p>\\n<p><span style=\\\"font-weight: 400;\\\">Gustav Stresemann was an important figure in Germany's resurgence and in establishing secure international relations. Stresemann’s Strategy brought an end to hyperinflation in Germany, created an economic relationship between Germany and America, as well as boosted Germany’s confidence, reducing support from extremists. Throughout 1923-1929, Stressmenn was able to revive Germany’s economy through the creation of Reichsmark. Further, with financial support from the US in the Dawes Plan, Germany was able to rebuild itself again.Moreover, Stresemann not only played a significant role in Germany's economy but as well as their international relations. He was able to place Germany as a head figure in the League of Nations council and form allegiances with other European powers in the Locarno Pact. This allowed Germany to change from being under pressure from the other powers but to now be more included. Therefore, his role under the Weimar Republic rebuilt Germany back into a leading power in the world.</span></p>\",\r\n" + 
			"      \"replies\": [\r\n" + 
			"        {\r\n" + 
			"          \"id\": 4613,\r\n" + 
			"          \"user_id\": 144,\r\n" + 
			"          \"parent_id\": 4568,\r\n" + 
			"          \"created_at\": \"2020-03-18T20:30:53Z\",\r\n" + 
			"          \"updated_at\": \"2020-03-18T20:30:53Z\",\r\n" + 
			"          \"rating_count\": null,\r\n" + 
			"          \"rating_sum\": null,\r\n" + 
			"          \"message\": \"<p>NIce job, Chandler...you made a good point about Stresemann's policies reducing the support of extremists in Germany. That is until the dreaded Great Depression sets in.  :(</p>\"\r\n" + 
			"        }\r\n" + 
			"      ]\r\n" + 
			"    },\r\n" + 
			"    \"4573\": {\r\n" + 
			"      \"id\": 4573,\r\n" + 
			"      \"parent_id\": null,\r\n" + 
			"      \"created_at\": \"2020-03-18T19:59:29Z\",\r\n" + 
			"      \"updated_at\": \"2020-03-18T20:00:35Z\",\r\n" + 
			"      \"editor_id\": 579,\r\n" + 
			"      \"rating_count\": null,\r\n" + 
			"      \"rating_sum\": null,\r\n" + 
			"      \"deleted\": true\r\n" + 
			"    },\r\n" + 
			"    \"4596\": {\r\n" + 
			"      \"id\": 4596,\r\n" + 
			"      \"user_id\": 607,\r\n" + 
			"      \"parent_id\": null,\r\n" + 
			"      \"created_at\": \"2020-03-18T20:08:05Z\",\r\n" + 
			"      \"updated_at\": \"2020-03-18T20:08:05Z\",\r\n" + 
			"      \"rating_count\": null,\r\n" + 
			"      \"rating_sum\": null,\r\n" + 
			"      \"message\": \"<p><i><span style=\\\"font-weight: 400;\\\">To what extent did the Locarno Conference ease tensions in 1920s Europe?</span></i></p>\\n<p><span style=\\\"font-weight: 400;\\\">The Locarno Conference played an extremely important role in easing tensions in 1920s Europe for one primary reason, “securing international cooperation.�? There were six main terms of the treaty which included, allowing Germany to have a seat in the League of Nations, allowing Britain and France to be the peacemakers, and Germany having the security of its borders.  Even though Germany did not have their entire goals met, they still had a say in negotiations towards peace treaties. Lastly, the Locarno Conference had many results in the end. For example, Germany was an equal again and in April of 1926, Stresemann signed the Treaty of Berlin with the USSR for the Soviet’s reassurance and that both Germany and the USSR would remain neutral in a situation where either was under attack. </span></p>\\n<p><br><br></p>\\n<p><i><span style=\\\"font-weight: 400;\\\">To what extent did Gustav Stresemann play a significant role in the mid-to-late 1920s?</span></i></p>\\n<p><span style=\\\"font-weight: 400;\\\">Gustav Stresemann’s main goal was to improve relations between France, Britain, and Germany during the mid-to-late 1920s. The three countries were key players in the help of peace negotiations so addressing them was a vital step towards peace. Stresemann implemented the Dawes Plan by helping Germany. <span>Through this plan, the reparations that Germany owed was reduced and their payment due date was also extended.</span> He set up the Stresemann which created the Rentenmark currency which helped recovered the German economy and its hyperinflation. Lastly, his behavior also led to stop the strain between modernists and extremists. </span></p>\",\r\n" + 
			"      \"replies\": [\r\n" + 
			"        {\r\n" + 
			"          \"id\": 4623,\r\n" + 
			"          \"user_id\": 144,\r\n" + 
			"          \"parent_id\": 4596,\r\n" + 
			"          \"created_at\": \"2020-03-18T21:07:39Z\",\r\n" + 
			"          \"updated_at\": \"2020-03-18T21:07:39Z\",\r\n" + 
			"          \"rating_count\": null,\r\n" + 
			"          \"rating_sum\": null,\r\n" + 
			"          \"message\": \"<p>Good work, Alexandria...you made some solid points pertaining to the \\\"peace\\\" that was achieved by the Locarno Pact and how influential Stresemann was in the process. </p>\"\r\n" + 
			"        }\r\n" + 
			"      ]\r\n" + 
			"    },\r\n" + 
			"    \"4616\": {\r\n" + 
			"      \"id\": 4616,\r\n" + 
			"      \"user_id\": 225,\r\n" + 
			"      \"parent_id\": null,\r\n" + 
			"      \"created_at\": \"2020-03-18T20:44:14Z\",\r\n" + 
			"      \"updated_at\": \"2020-03-18T20:44:14Z\",\r\n" + 
			"      \"rating_count\": null,\r\n" + 
			"      \"rating_sum\": null,\r\n" + 
			"      \"message\": \"<p class=\\\"p1\\\"><strong>Initial Discussion Question:</strong></p>\\n<p class=\\\"p1\\\"><strong>To what extent did the Locarno Conference ease tensions in 1920s Europe?</strong></p>\\n<p class=\\\"p2\\\"> </p>\\n<p class=\\\"p1\\\">The Locarno Conference, by allowing Germany and other Allies Power to come to an agreement with demilitarizing Rhineland and securing the Western Borders among France, Belgium, and Germany, significantly eased the immediate tensions in 1920s Europe. First, the conference reassured France as Germany agreed on negotiating pacifically in time of conflicts instead of using force, and that France would be backed up by Poland, Czechoslovakia, and Britain in time of needs. Second, the conference benefited Germany by including her in the League of Nations and one of the major powers. In addition, demilitarization of Rheinland boosted the spirit of German society, stabilizing the nation. Yet, due to the long history of conflicts between France and Germany, suspicion creeped up inside both nations, leading to the construction of the French Maginot Line to further secure the border and the continuum of the Treaty of Rapallo between Germany and Russia to resume military activities. The Locarno Conference was a successful emergency placebo pill to ease the tensions, yet did not completely deal with the conflicts among nations.</p>\\n<p class=\\\"p2\\\"> </p>\\n<p class=\\\"p1\\\"><strong>Follow Up Discussion Question:</strong></p>\\n<p class=\\\"p1\\\"><strong>To what extent did Gustav Stresemann play a significant role in the mid-to-late 1920s?</strong></p>\\n<p class=\\\"p2\\\"> </p>\\n<p class=\\\"p1\\\">Stresemann, with his long-term vision and moderate style of leading, significantly influenced the state of Western foreign affairs and revamped the spirit of German society. First, he understood the core problem of the economy and accepted the opportunity cost of securing German society by sacrificing time and space. The creation of the Rentenmark and Reichsmark showed his understanding of money value, slowing down inflation. The Dawes Plan and the Young Plan, by extending the reparation period to 59 years and allowing input into the economy via American loans, lowered taxes, improved citizens’ life conditions, and drove the movement of money, boosting the economy. This contributed to the optimistic German spirit, society trusting the government, stabilization of the nation, and so Germany could finally enter the game of global power again. Stresemann eased the tensions among Allies powers via the Locarno Pact, increasing the value and trust of Germany from other countries. He also pushed Germany to become a member of the League of Nations, proving to other countries and German citizens that Germany was becoming great again.</p>\"\r\n" +  
			"    },\r\n" + 
			"    \"4663\": {\r\n" + 
			"      \"id\": 4663,\r\n" + 
			"      \"user_id\": 236,\r\n" + 
			"      \"parent_id\": null,\r\n" + 
			"      \"created_at\": \"2020-03-19T00:52:09Z\",\r\n" + 
			"      \"updated_at\": \"2020-03-19T00:52:09Z\",\r\n" + 
			"      \"rating_count\": null,\r\n" + 
			"      \"rating_sum\": null,\r\n" + 
			"      \"message\": \"<p>Initial Question:</p>\\n<p>The Locarno Conference did manage to ease up some tensions in Europe, but only on the Western side of Germany. The solidification of Germany's borders and bringing in Britain and Italy as guarantors gave a great sense of peace for many countries.  As well as how Germany was included within the conference and later invited to the league of nations also gave hope that the conflict with Germany would soon end. However, the conference didn't ease up on all of Europe's tensions. France still felt threatened and insecure, so they construct a military defense line along the border of Germany. The conference also never address Germany's eastern borders. Lastly, the Locarno conference devalued the purpose of the League of Nations.</p>\\n<p> </p>\\n<p>Follow up question:</p>\\n<p>Gustav Stresemann played a vital role in the mid-to-late 1920s by how he helps rebuild German's economy. H<span>e helped to end Germany's hyper inflation by creating the new currency, Rentenmark, which later turned into, Reichsmark which had actual value. Stresemann also agreed to the Dawes plan, which reduced Germany's reparations and had America give loans to help its economy. This help reassures the German people. Stresemann not only helps solve Germany's economic issue but improve foreign relations as well. By creating the Locarno Pact, Stresemann basically convinced the League of Nations to invite them.  Stresemann was awarded for all his achievements a noble peace prize.</span></p>\"\r\n" + 
			"    }\r\n" + 
			"  }\r\n" + 
			"}";
}
