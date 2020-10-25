package net.speedstor.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import net.speedstor.discussion.DiscussionHandler;
import net.speedstor.main.Main;
import net.speedstor.main.TokenHandler;
import net.speedstor.server.Server;
import net.speedstor.websocket.WebSocketHandler;

public class Cmd implements Runnable{
	BufferedReader in;
	Main main;
	Log log;
	Server Server;
	WebSocketHandler websocketHandler;
	DiscussionHandler discussionHandler;
	TokenHandler tokenHandler;

	public Cmd(Main main, Log log, Server server, WebSocketHandler websocketHandler, TokenHandler tokenHandler, DiscussionHandler discussionHandler) {
		in = new BufferedReader(new InputStreamReader(System.in));
		this.main = main;
		this.websocketHandler = websocketHandler;
		this.tokenHandler = tokenHandler;
		this.log = log;
		this.discussionHandler = discussionHandler;
	}
	
	public void run() {
		outerloop:
		while(true) {
			String[] commandLine = new String[0];
			try {
				if(in.ready()) {
					commandLine = in.readLine().split("\\s+");
					switch(commandLine[0]) {
					case "":
						break;
					case "webSocket-sendAllMsg":
						if(commandLine.length > 1) {
							websocketHandler.sendToAll(commandLine[1]);
						}else {
							log.error("need a second parameter for send command");
						}
						break;
					case "webSocket-sendAllBinary":
						if(commandLine.length > 1) {
							websocketHandler.sendToAllBinary(commandLine[1]);
						}else {
							log.error("need a second parameter for send command");
						}
						break;
					case "webSocket-sendAllDefault":
						websocketHandler.sendAllDefault();
						break;
					case "webSocket-listSockets":
						String[] websocketList = websocketHandler.listWebsockets();
						log.list("/--- Size: "+websocketHandler.getSize()+" ------------------------------------------------------------------------------");
						for(int i = 0; i < websocketList.length; i++) {
							log.list("  "+websocketList[i]);
						}
						break;
					case "webSocket-listUsers":
						String[] userList = websocketHandler.listUserConnections();
						log.list("/--- Size: "+userList.length+" ------------------------------------------------------------------------------");
						for(int i = 0; i < userList.length; i++) {
							log.list("  "+userList[i]);
						}
						break;
					case "updateTokensAll":
						tokenHandler.updateTokens();
						break;
					case "getDiscussionName":
						if(commandLine.length > 1) {
							log.response("["+String.join(" ", commandLine)+"] "+discussionHandler.canvas_getFromId(commandLine[1]).getName());
						}else {
							log.error("need a second parameter for send command");
						}
						break;
					case "listDiscussions":
						String[] discussionList = discussionHandler.listDiscussionBoards();
						log.list("/--- Size: "+discussionHandler.canvas_getSize()+" ------------------------------------------------------------------------------");
						for(int i = 0; i < discussionList.length; i++) {
							log.list("  "+discussionList[i]);
						}
						break;
					case "updateToken":
						if(commandLine.length > 1) {
							for(int i = 1; i < commandLine.length; i++) {
								tokenHandler.updateToken(commandLine[i]);
							}
						}else {
							log.error("need a second parameter for send command");
						}
						break;
					case "testCodeBlock":
						testCodeBlock();
						break;
					case "stop":
					case "quit":
					case "exit":
						break outerloop;
					default:
						log.warn("::no command found  ["+String.join(" ", commandLine)+"]");
						break;
					}
					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}catch (Exception e){
				e.printStackTrace();
				log.error("Command Exception Thrown!! ["+String.join(" ", commandLine)+"]");
			}

	        try {
				Thread.sleep(Settings.CMD_LISTEN_SPEED_INTERVAL);
			} catch (InterruptedException e) {
				//e.printStackTrace();
				log.error("outStream thread sleep 500ms interrupted");
			}
		}
		
		main.stop();
	}
	
	public void testCodeBlock() {
		
	}
	

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}

}
