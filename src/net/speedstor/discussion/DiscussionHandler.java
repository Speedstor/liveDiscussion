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
	
	HashMap<String, CanvasDiscussion> canvas_discussionBoard = new HashMap<>();
	
	public DiscussionHandler(Log log, Clock clock, TokenHandler tokenHandler, WebSocketHandler websocketHandler) {
		this.log = log;
		this.clock = clock;
		this.tokenHandler = tokenHandler;
		this.websocketHandler = websocketHandler;
	}
	
	public void canvas_put(String key, CanvasDiscussion discussion) {
		canvas_discussionBoard.put(key, discussion);
	}
	
	public CanvasDiscussion canvas_get(String key) {
		return canvas_discussionBoard.get(key);
	}
	
	public CanvasDiscussion canvas_getFromUrl(String url) {
		int stringCutIndex = url.indexOf("/discussion");
		String discussionId = url.substring(url.indexOf("/courses/") + 9, stringCutIndex)+"v"+url.substring(stringCutIndex+18, stringCutIndex+18+4);
		return canvas_discussionBoard.get(discussionId);
	}
	
	public CanvasDiscussion canvas_getFromId(String id) {
		String[] discussionIds = id.split("v");
		if(discussionIds.length == 2) {
			String courseId = discussionIds[0];
			String discussionId = discussionIds[1];
	
			String key = courseId + "v" + discussionId;
	
			return canvas_discussionBoard.get(key);
		}else {
			return null;
		}
	}
	
	public String[] listDiscussionBoards() {
		String[] returnArray = new String[canvas_discussionBoard.size()];
		int index = 0;
		for(Entry<String, CanvasDiscussion> item : canvas_discussionBoard.entrySet()) {
			String appendString = "";
			CanvasDiscussion canvasDiscussion = item.getValue();
			String sockets = String.join(", ", canvasDiscussion.getParticipantList());
			appendString += "Board: Canvas-"+item.getKey()+";  ";
			appendString += "Size: "+canvasDiscussion.getPariticipantSize()+";  ";
			appendString += "Sockets: ["+sockets+"];  ";
			
			returnArray[index] = appendString;
			index++;
		}
		return returnArray;
	}
	
	public boolean canvas_containKey(String key) {
		return canvas_discussionBoard.containsKey(key);
	}
	
	public int canvas_getSize() {
		return canvas_discussionBoard.size();
	}
}
