package net.speedstor.main;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import net.speedstor.control.Log;
import net.speedstor.control.Settings;
import net.speedstor.network.Network;

public class ThreadScripts {
	public static void updateDiscussionLists(String canvasToken, Cache cache, Log log) {
		Thread thread = new UpdateDiscussionList(canvasToken, cache, log);
		thread.start();
	}
}

class UpdateDiscussionList extends Thread{
	String canvasToken;
	Cache cache;
	Log log;
	public UpdateDiscussionList(String canvasToken, Cache cache, Log log) {
		this.canvasToken = canvasToken;
		this.cache = cache;
		this.log = log;
	}
	
	@SuppressWarnings("unchecked")
	public void run() {
		//get list of courses
		String coursesResponse = Network.sendGetWithAuth(Settings.API_URL+"/courses", canvasToken);
						
		//get list of discussions
		JSONObject discussionList = new JSONObject();
		try {
			JSONArray coursesJson = (JSONArray) Settings.parser.parse(coursesResponse);
			coursesJson.forEach(courseItem -> {
				String courseId = ""+((JSONObject) courseItem).get("id");
				JSONObject discussionObject = new JSONObject();
				String discussionResponse = Network.sendGetWithAuth(Settings.API_URL+"/courses/"+courseId+"/discussion_topics", canvasToken);
				
				try {
					JSONArray discussionArray = (JSONArray) Settings.parser.parse(discussionResponse);
					discussionObject.put("name", (String) ((JSONObject) courseItem).get("name"));
					discussionObject.put("id", courseId);
					discussionObject.put("discussions", discussionArray);
				} catch (ParseException e) {
					log.error("parse json for listing discussion error");
				}

				discussionList.put(courseId, discussionObject);
			});
			cache.putData("discussionLists", canvasToken, discussionList.toJSONString());
		} catch (ParseException e) {
			log.error("parse json for listing courses error");
		}
				
	}
}
