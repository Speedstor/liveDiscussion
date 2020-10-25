package net.speedstor.websocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import net.speedstor.control.Log;
import net.speedstor.main.TokenHandler;

public class WebSocketHandler {
	Log log;
	TokenHandler tokenHandler;
	private HashMap<String, WebSocket> websockets = new HashMap<String, WebSocket>();
	public HashMap<String, ArrayList<String>> userConnections = new HashMap<String, ArrayList<String>>(); //userId, num
	
	//client connections to check how many websockets each user has [client.getInetAddress()]
	public HashMap<String, Integer> clientConnections = new HashMap<String, Integer>();
	
	public WebSocketHandler(Log log, TokenHandler tokenHandler) {
		this.log = log;
		this.tokenHandler = tokenHandler;
	}
	
	public int addWebsocket(String socketId, WebSocket websocket) {
		websockets.put(socketId, websocket);
		String userId = tokenHandler.tokenDB_get(tokenHandler.socketList_get(socketId))[2];
		if(userConnections.containsKey(userId)) {
			userConnections.get(userId).add(socketId);
		}else {
			ArrayList<String> newRecord = new ArrayList<String>();
			newRecord.add(socketId);
			userConnections.put(userId, newRecord);
		}
		return 1;
	}
	
	public WebSocket get(String socketId) {
		if(websockets.containsKey(socketId)) {
			return websockets.get(socketId);
		}else {
			return null;
		}
	}
	
	public boolean remove(String socketId) {
		if(websockets.containsKey(socketId)) {
			websockets.remove(socketId);
			String userId = tokenHandler.tokenDB_get(tokenHandler.socketList_get(socketId))[2];
			if(userConnections.size() > 1) userConnections.get(userId).remove(socketId);
			else userConnections.remove(userId);
			return true;
		}
		return false;
	}
	
	public String[] listUserConnections() {
		String[] returnArray = new String[userConnections.size()];
		int index = 0;
		for(Entry<String, ArrayList<String>> item : userConnections.entrySet()) {
			String userId = item.getKey();
			String sockets = String.join(", ", item.getValue().toArray(new String[0]));
			String appendString = "";
			appendString += "UserId: "+userId+";  ";
			appendString += "NumOf: "+item.getValue().size()+";  ";
			appendString += "Name: "+tokenHandler.tokenDB_get(tokenHandler.socketList_get(item.getValue().get(0)))[3]+";  ";
			appendString += "SocketConn: ["+sockets+"];  ";
			//add name
			
			returnArray[index] = appendString;
			index++;
		}
		return returnArray;
	}
	
	public String[] listWebsockets() {
		String[] returnArray = new String[websockets.size()];
		int index = 0;
		for(Entry<String, WebSocket> item : websockets.entrySet()) {
			String socketId = item.getKey();
			WebSocket websocket = item.getValue();
			String appendString = "";
			appendString += "Websocket: "+socketId+";  ";
			appendString += "UserId: "+websocket.getUserId()+";  ";
			appendString += "Discussion: "+websocket.getDiscussionId()+";  ";
			appendString += "Name: "+tokenHandler.tokenDB_get(tokenHandler.socketList_get(socketId))[3]+";  ";
			if(websocket.client != null) {
				appendString += "Address: "+websocket.client.getInetAddress()+";  ";
			}
			
			returnArray[index] = appendString;
			index++;
		}
		return returnArray;
	}
	
	
	public boolean containSocket(String socketId) {
		return websockets.containsKey(socketId);
	}
	
	public void sendToAllBinary(String binaryString) {
		websockets.forEach((key, value) -> {
			value.sendBinary(binaryString);
		});
	}
	
	public void sendAllDefault() {
		websockets.forEach((key, value) -> {
			value.sendDefault();
		});
	}

	public void sendToAll(String msg) {

		websockets.forEach((key, value) -> {
			value.sendUnmask(msg);
		});
	}
	
	public void closeAll() {
		websockets.forEach((key, value) -> {
			value.disconnectSocket();
		});
	}

	public int getSize() {
		return websockets.size();
	}
}
