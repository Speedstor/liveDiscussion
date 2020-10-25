package net.speedstor.control;

import org.json.simple.parser.JSONParser;

public class Settings {
	//public static String CANVAS_DOMAIN = "https://fairmontschools.beta.instructure.com";
	public static String CANVAS_DOMAIN = "https://fairmontschools.instructure.com";
	public static String API_PATH = "/api/v1";
	public static String API_URL = CANVAS_DOMAIN + API_PATH;

	//public static String LOGIN_DESTINATION = "http://localhost/liveDiscussion/choose.html";
	public static String LOGIN_DESTINATION = "http://http.speedstor.net/liveDiscussion/choose.html";
	
	public static boolean DISCONNECT_FROM_CANVAS = true;
	
	public static JSONParser parser = new JSONParser();

	public static long LISTEN_SPEED_INTERVAL = 600;
	public static long CMD_LISTEN_SPEED_INTERVAL = 2000;
	public static long SEND_SPEED_INTERVAL = 120;
	
	public static String DOCS_LOC = "./canvasBot-Docs";
	public static String CACHE_FILE_NAME = "cache.txt";
	public static String TOKENDB_FILE_NAME = "tokensDB.txt";
}
