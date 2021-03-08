package net.speedstor.discussion;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map.Entry;

import net.speedstor.control.Log;
import net.speedstor.main.TokenHandler;
import net.speedstor.websocket.WebSocketHandler;

public class DiscussionHandler {
	Log log;
	Clock clock;
	TokenHandler tokenHandler;
	WebSocketHandler websocketHandler;
	
	HashMap<String, Discussion> discussionBoard = new HashMap<>();
	
	public DiscussionHandler(Log log, Clock clock, TokenHandler tokenHandler, WebSocketHandler websocketHandler) {
		this.log = log;
		this.clock = clock;
		this.tokenHandler = tokenHandler;
		this.websocketHandler = websocketHandler;
	}
	
	public void put(String key, Discussion discussion) {
		discussionBoard.put(key, discussion);
	}

	public Discussion get(String key) {
		return discussionBoard.get(key);
	}
	
	public CanvasDiscussion canvas_getFromUrl(String url) {
		int stringCutIndex = url.indexOf("/discussion");
		String discussionId = url.substring(url.indexOf("/courses/") + 9, stringCutIndex)+"v"+url.substring(stringCutIndex+18, stringCutIndex+18+4);
		return (CanvasDiscussion) discussionBoard.get(discussionId);
	}
	
	public CanvasDiscussion canvas_getFromId(String id) {
		String[] discussionIds = id.split("v");
		if(discussionIds.length == 2) {
			return (CanvasDiscussion) discussionBoard.get(id);
		}else {
			return null;
		}
	}
	
	public String[] listDiscussionBoards() {
		String[] returnArray = new String[discussionBoard.size()];
		int index = 0;
		for(Entry<String, Discussion> item : discussionBoard.entrySet()) {
			String appendString = "";
			Discussion canvasDiscussion = item.getValue();
			String sockets = String.join(", ", canvasDiscussion.getParticipantList());
			if(item.getKey().length() == 8) appendString += "Board: Canvas-"+item.getKey()+";  ";
			else  appendString += "Board: Discussion-"+item.getKey()+";  ";
			appendString += "Size: "+canvasDiscussion.getPariticipantSize()+";  ";
			appendString += "Sockets: ["+sockets+"];  ";
			
			returnArray[index] = appendString;
			index++;
		}
		return returnArray;
	}
	
	public boolean containKey(String key) {
		return discussionBoard.containsKey(key);
	}
	
	public int getSize() {
		return discussionBoard.size();
	}
}
