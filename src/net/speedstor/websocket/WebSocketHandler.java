package net.speedstor.websocket;

import java.util.HashMap;

import net.speedstor.main.Log;

public class WebSocketHandler {
	Log log;
	private HashMap<String, WebSocket> websockets = new HashMap<String, WebSocket>();
	private HashMap<String, String> socketTokenMap = new HashMap<>();
	
	public WebSocketHandler(Log log) {
		this.log = log;
	}
	
	public int addWebsocket(String socketId, WebSocket websocket, String serverToken) {
		websockets.put(socketId, websocket);
		socketTokenMap.put(socketId, serverToken);
		return 1;
	}
	
	public WebSocket get(String socketId) {
		if(websockets.containsKey(socketId)) {
			return websockets.get(socketId);
		}else {
			return null;
		}
	}
	
	public String getServerToken(String socketId) {
		if(socketTokenMap.containsKey(socketId)) {
			return socketTokenMap.get(socketId);
		}else {
			return null;
		}		
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
}
