package net.speedstor.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.speedstor.control.Credentials;
import net.speedstor.control.Log;
import net.speedstor.control.Settings;
import net.speedstor.network.Network;

public class TokenHandler {
	Log log;
	JSONParser parser = new JSONParser();
	
	//serverToken, [access_token(canvasToken), refresh_token, id, name]
	private HashMap<String, String[]> tokenDB = new HashMap<>();
	
	//new every time
	//socketId, serverToken
	//v two socketId can have the same server Token == two browsers at the same time
	private HashMap<String, String> socketList = new HashMap<>();
	
	private ScheduledExecutorService scheduleExecutor;
	private ScheduledFuture<?> tokenSchedule = null;	

	public TokenHandler(Log log) {
		this.log = log;
		tokenDB = initRetrieveDatabase();
		scheduleExecutor = Executors.newSingleThreadScheduledExecutor();
		resetTokenSchedulerTimer();
	}
	
	public TokenHandler(Log log, boolean periodicUpdate) {
		this.log = log;
		tokenDB = initRetrieveDatabase();
		if(periodicUpdate) {
			scheduleExecutor = Executors.newSingleThreadScheduledExecutor();
			resetTokenSchedulerTimer();
		}else {
			socketList_put("7RlCGp63FFFdZFE", "Orm3rtf0E9FNhle");
		}
	}
	
	//#region Token Manipulation
	public void resetTokenSchedulerTimer() {
		if(tokenSchedule == null) {
			tokenSchedule = scheduleExecutor.scheduleAtFixedRate(new TokenScheduler(this), 0, 58, TimeUnit.MINUTES);
		}else {
			tokenSchedule.cancel(true);
			tokenSchedule = scheduleExecutor.scheduleAtFixedRate(new TokenScheduler(this), 1*60*60*1000, 58, TimeUnit.MINUTES);			
		}
	}
	
	public void updateTokens() {
		resetTokenSchedulerTimer();
		updateTokensPeriodic();
	}
	
	public void updateTokensPeriodic() {
		log.log("--updating all tokens...");
		for(String key : tokenDB.keySet()) {
			updateToken(key);
		}
		log.log("--finish updating tokens");
	}
	
	public void updateToken(String serverToken) {
		String refreshToken = tokenDB.get(serverToken)[1];
		if(!refreshToken.equals("null") && !refreshToken.equals("")) {
			String canvasTokenJson = Network.sendPost("https://fairmontschools.beta.instructure.com/login/oauth2/token?refresh_token="+refreshToken+"&grant_type=refresh_token&client_id="+Credentials.CLIENT_ID+"&client_secret="+Credentials.CLIENT_SECRET);
			try {
				JSONObject canvasTokens = (JSONObject) parser.parse(canvasTokenJson);
				tokenDB.get(serverToken)[0] = (String) canvasTokens.get("access_token");
			} catch (ParseException e) {
				log.error("refreshing canvas token error");
			}
		}
	}
	
	public void appendToken(String serverToken, String canvasToken, String refreshToken, String id, String userName) {
		addToken(serverToken, canvasToken, refreshToken, id, userName);
	}
	
	public void addToken(String serverToken, String canvasToken, String refreshToken, String id, String userName) {
		tokenDB.put(serverToken, new String[]{canvasToken, refreshToken, id, userName});
		
		//write in file:
		//serverToken, [access_token(canvasToken), refresh_token, id, name], strDate
		try {
			File f = new File(Settings.DOCS_LOC);
			if(!f.exists()) System.out.println("File Storage does not exsist --Creating storage folder: "+(new File(Settings.DOCS_LOC)).mkdirs());
			
			Date date = Calendar.getInstance().getTime();
			String strDate = (new SimpleDateFormat("yyyy-mm-dd=hh.mm.ss")).format(date);
			
			FileWriter fr = new FileWriter(new File(Settings.DOCS_LOC, Settings.TOKENDB_FILE_NAME), true);
			fr.write("\n"+serverToken+":"+canvasToken+":"+refreshToken+":"+id+":"+userName+":"+strDate);
			fr.close();
			
			log.log("New User: " + userName);
			
		}catch(IOException e){
			log.error("server writing error");
		}
	}
	//#endregion
	
	
	//#region socketId - socketList
	public void socketList_put(String socketId, String serverToken) {
		socketList.put(socketId, serverToken);
	}
	
	public boolean socketList_containValue(String serverToken) {
		return socketList.containsValue(serverToken);
	}
	
	public boolean socketList_containKey(String socketId) {
		return socketList.containsKey(socketId);
	}
	
	public String socketList_get(String socketId) {
		return socketList.get(socketId);
	}
	//#endregion
	
	//#region tokenDB
	public String[] tokenDB_get(String key) {
		return tokenDB.get(key);
	}
	
	public String tokenDB_getCanvasTokenFromSocket(String socketId) {
		return tokenDB.get(socketList.get(socketId))[0];
	}
	
	public boolean tokenDB_containKey(String key) {
		return tokenDB.containsKey(key);
	}
	//#endregion
	
	private HashMap<String, String[]> initRetrieveDatabase() {
		try {
			File file = new File(Settings.DOCS_LOC, Settings.TOKENDB_FILE_NAME); 
			if(!file.exists()) {
				return new HashMap<String, String[]>();
			}
			
			HashMap<String, String[]> returnTokenList = new HashMap<>();
			
			Scanner sc = new Scanner(file);
			String line;
			while (sc.hasNextLine()) {
				 line = sc.nextLine();
				 if(line.length() > 1) {
					 line = line.substring(0, line.length()-1);
					 String[] entry;
					 if(line.contains(":")) {
						 entry = line.split(":");
						 if(entry.length > 4) {
							 returnTokenList.put(entry[0], new String[] {entry[1], entry[2], entry[3], entry[4]});
						 }else {
							 log.warn("token database corrupted");
						 }
					 }else {
						 log.warn("token database corrupted");
					 }
				 }
			}
			return returnTokenList;
		}catch (IOException e) {
			log.error("error parsing and retriving token Hashmap");
		}
		return new HashMap<String, String[]>();
	}
	
	//from https://stackoverflow.com/questions/7888004/how-do-i-print-escape-characters-in-java -- for debuggin
	private static String unEscapeString(String s){
	    StringBuilder sb = new StringBuilder();
	    for (int i=0; i<s.length(); i++)
	        switch (s.charAt(i)){
	            case '\n': sb.append("\\n"); break;
	            case '\t': sb.append("\\t"); break;
	            // ... rest of escape characters
	            default: sb.append(s.charAt(i));
	        }
	    return sb.toString();
	}
}

class TokenScheduler implements Runnable{
	TokenHandler tokenHandler;
	public TokenScheduler(TokenHandler tokenHandler) {
		this.tokenHandler = tokenHandler;
	}
	@Override
	public void run() {
		tokenHandler.updateTokensPeriodic();
	}
}
