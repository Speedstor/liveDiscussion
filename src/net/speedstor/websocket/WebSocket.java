package net.speedstor.websocket;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.net.Socket;
import java.time.Clock;
import org.json.simple.parser.JSONParser;
import net.speedstor.control.Log;
import net.speedstor.control.Settings;
import net.speedstor.discussion.CanvasDiscussion;
import net.speedstor.discussion.Discussion;
import net.speedstor.discussion.DiscussionHandler;
import net.speedstor.main.TokenHandler;
import net.speedstor.server.APIServer;

public class WebSocket{
	
	Socket client;
	BufferedReader inFromClient;
	BufferedOutputStream outToClient;
	Log log;
	TokenHandler tokenHandler;
	DiscussionHandler discussionHandler;
	WebSocketHandler websocketHandler;
	Clock clock;
	private Boolean running;
	private String discussionUrl;
	private String userId;
	APIServer aPIServer;
	
	String serverToken;
	String socketId;
	String discussionId;
	
	//JSON parsing
    JSONParser parser = new JSONParser();
	
	InStream inStream;
	OutStream outStream;
	

	public WebSocket(Log log, Clock clock, String discussionId, APIServer apiServer, TokenHandler tokenHandler, String socketId, DiscussionHandler discussionHandler, WebSocketHandler websocketHandler, String userId) {
		this(log, clock, tokenHandler.socketList_get(socketId), discussionId, apiServer, tokenHandler, socketId, discussionHandler, websocketHandler, userId);
	}

	public WebSocket(Log log, Clock clock, String serverToken, String discussionId, APIServer apiServer, TokenHandler tokenHandler, String socketId, DiscussionHandler discussionHandler, WebSocketHandler websocketHandler, String userId) {
		this.log = log;
		this.clock = clock;
		this.serverToken = serverToken;
		this.aPIServer = apiServer;
		this.tokenHandler = tokenHandler;
		this.discussionHandler = discussionHandler;
		this.websocketHandler = websocketHandler;
		this.socketId = socketId;
		this.userId = userId;
		
		if(discussionId.contains("v")) {			
			String[] discussionIds = discussionId.split("v");
			if(discussionIds.length >= 2) {		
				this.discussionId = discussionIds[0] + "v" + discussionIds[1];
				this.discussionUrl = Settings.API_URL+"/courses/"+discussionIds[0]+"/discussion_topics/"+discussionIds[1];
			}
		}else {
			this.discussionId = discussionId;
			this.discussionUrl = Settings.WEB_ADDRESS_PATH+"/discussion.html?id="+discussionId;	
		}
	}
	
	public void initSetup(Socket client, BufferedReader inFromClient, BufferedOutputStream outToClient) {
		this.client = client;
		this.inFromClient = inFromClient;
		this.outToClient = outToClient;
		
		outStream = new OutStream(this, client, inFromClient, outToClient, clock, log);
		
		inStream = new InStream(this, client, inFromClient, outToClient, clock, log, outStream, userId, socketId);
		Thread inStreamThread = new Thread(inStream);
		inStreamThread.start();
	}
	
	void sendSyncJson(String excludeSocket, String JsonString) {
		Discussion discussion = discussionHandler.get(discussionId);
		discussion.sendSync(excludeSocket, JsonString);
	}

	public void sendUnmaskThread(String message) {
		//want to offload to thread 
		outStream.send(message);
	}

	public int disconnectSocket() {
		outStream.sendCloseFrame();
		return disconnectSocketResponse();
	}
	
	public int disconnectSocketResponse() {
		running = false;
		inStream.stop();
		inStream = null;
		outStream = null;
		websocketHandler.remove(socketId);
		discussionHandler.get(discussionId).setOffline(socketId);
		return 1;
	}
	
	public void sendBinary(String binaryString) {
		outStream.sendBinary(binaryString);
	}
	
	public void sendDefault() {
		outStream.sendDefault();
	}
	
	public void sendUnmask(String message) {
		sendUnmaskThread(message);
	}

	public String getDiscussionId() {
		return discussionId;
	}
	
	public String getUserId() {
		return userId;
	}

	public String getUrl() {
		return discussionUrl;
	}

	public String getId() {
		return socketId;
	}
	

}
