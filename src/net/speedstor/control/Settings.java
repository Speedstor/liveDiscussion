package net.speedstor.control;

import org.json.simple.parser.JSONParser;

public class Settings {
	public static String DOCS_LOC = "./canvasBot-Docs";
	public static String CACHE_FILE_NAME = "cache.txt";
	public static String TOKENDB_FILE_NAME = "tokensDB.txt";
	
	final public static int DISCUSSION_ID_SIMPLE_LENGTH = 6;
	
	//public static String CANVAS_DOMAIN = "https://fairmontschools.beta.instructure.com";
	public static String CANVAS_DOMAIN = "https://fairmontschools.instructure.com";
	public static String API_PATH = "/api/v1";
	public static String API_URL = CANVAS_DOMAIN + API_PATH;
	public static int API_SERVER_PORT = 40;
	public static int SYNC_CANVAS_FREQ = 1300;
	
	public static int WEB_SERVER_PORT = 80;	
	public static String WEB_FOLDER_PATH = DOCS_LOC+"/web";
	public static String WEB_ADDRESS_PATH = "http://localhost";

	public static String LOGIN_DESTINATION = WEB_ADDRESS_PATH + "/choose.html";
	public static String DEFAULT_USER_IMAGE_PATH = WEB_ADDRESS_PATH + "/img/defaultUserImg.png";
	//public static String LOGIN_DESTINATION = "http://http.speedstor.net/liveDiscussion/choose.html";
	
	public static boolean DISCONNECT_FROM_CANVAS = true;
	
	public static JSONParser parser = new JSONParser();

	public static long LISTEN_SPEED_INTERVAL = 600;
	public static long CMD_LISTEN_SPEED_INTERVAL = 2000;
	public static long SEND_SPEED_INTERVAL = 120;
}
