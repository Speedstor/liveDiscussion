package net.speedstor.outdated;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class LoopedCheck implements Runnable{
	Object[][] checkList;
	boolean running = false;
    JSONParser parser;
	
	public LoopedCheck(Object[][] checkList) {
		this.checkList = checkList;
		parser = new JSONParser();
	}
	

	@Override
	public void run() {
		running = true;
		System.out.println("checkLoopStarted");
		
		
		
		while(running) {
			
			for(int i = 0; i < checkList.length; i++) {
				String checkToken = (String) checkList[i][0];
				String conversationJsonString = sendGet("https://fairmontschools.instructure.com/api/v1/conversations?scope=inbox&filter_mode=and&include_private_conversation_enrollments=false"
														+"&access_token="+checkToken);

				JSONArray convJson;
				if(conversationJsonString != "error") {
					try {
			            convJson = (JSONArray) parser.parse(conversationJsonString);

			            System.out.println(convJson);
			            
			            
					}catch (ParseException e) {
						e.printStackTrace();
						System.out.println("Error with parsing json string");
					}
					
				}else {
					System.out.println("Cannot retreive json page on conversations for: " + checkToken);
				}
			}
		}
		
	}
	
	public String sendGet(String url) {
		try {
			URL urlObj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			int responseCode = con.getResponseCode();
			System.out.println("GET Response Code :: " + responseCode);
			if (responseCode == HttpURLConnection.HTTP_OK) {
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
	
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
	
				// print result
				return response.toString();
			} else {
				System.out.println("GET request not worked");
				return "error: get response code error";
			}
		}catch(IOException e){
			e.printStackTrace();
			return "error: catch error";
		}
	}
	
	
	public Object[][] getCheckList() {
		return checkList;
	}
	
	public boolean ifRunning() {
		return running;
	}
	
	public int stop() {
		running = false;
		return 1;
	}
}
