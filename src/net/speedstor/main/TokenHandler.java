package net.speedstor.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class TokenHandler {
	Log log;
	private HashMap<String, String> tokens = new HashMap<>();
	//serverToken, canvasToken
	
	String databaseLoc = "../canvasBot-Docs";

	public TokenHandler(Log log) {
		this.log = log;
		initRetrieveDatabase();
	}
	
	public boolean containValue(String canvasToken) {
		log.special(tokens.toString());
		log.special(""+tokens.containsValue(canvasToken));
		return tokens.containsValue(canvasToken);
	}
	
	public String[] getKeyFromValue(String canvasToken) {
		ArrayList<String> store = new ArrayList<String>();
		tokens.forEach((k,v) -> {
			if(v.equals(canvasToken)) {
				store.add(k);
			}
		});
		String[] returnArray = store.toArray(new String[0]);
		return returnArray;
	}
	
	public String get(String serverToken) {
		return tokens.get(serverToken);
	}
	
	public boolean contains(String serverToken) {
		return tokens.containsKey(serverToken);
	}
	
	public void addToken(String serverToken, String canvasToken) {
		tokens.put(serverToken, canvasToken);
		

		//write in file: serverToken - CanvasToken
		try {
			File f = new File(databaseLoc);
			if(!f.exists()) System.out.println("File Storage does not exsist --Creating storage folder: "+(new File(databaseLoc)).mkdirs());
			
			File file = new File(databaseLoc, "tokens.txt");
			FileWriter fr = new FileWriter(file, true);
			fr.write("\n"+serverToken+":"+canvasToken);
			fr.close();
			
			log.log("New User: " + serverToken);
			
		}catch(IOException e){
			log.error("server writing error");
		}
	}
	
	private HashMap<String, String> initRetrieveDatabase() {
		try {
			File file = new File(databaseLoc+"/tokens.txt"); 
			if(!file.exists()) {
				tokens = new HashMap<>();
				return tokens;
			}
			

			Scanner sc = new Scanner(file);
			String line;
			while (sc.hasNextLine()) {
				 line = sc.nextLine();
				 String[] entry;
				 if(line.contains(":")) {
					 entry = line.split(":");
					 tokens.put(entry[0], entry[1].substring(0, 69));
				 }
			};
			
		}catch (IOException e) {
			log.error("error parsing and retriving token Hashmap");
		}
		tokens = new HashMap<>();
		return tokens;
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
