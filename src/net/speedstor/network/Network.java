package net.speedstor.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Network {

	static public String sendGet(String url) {
		return sendRequest(url, "GET", null);
	}
	
	static public String sendPost(String url) {
		return sendRequest(url, "POST", null);
	}
	
	static public String sendPostWithBody(String url, String jsonString) {
		return sendRequest(url, "POST", jsonString);
	}
	
	private static String sendRequest(String url, String requestMethod, String bodyJsonString) {
		try {
			URL urlObj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
			con.setRequestMethod(requestMethod);
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			
			//send body request in json string
			if(bodyJsonString != null) {
				con.setRequestProperty("Content-Type", "application/json; utf-8");
				con.setRequestProperty("Accept", "application/json");
				con.setDoOutput(true);
				
				try(OutputStream os = con.getOutputStream()) {
				    byte[] input = bodyJsonString.getBytes("utf-8");
				    os.write(input, 0, input.length);           
				}
			}
			
			int responseCode = con.getResponseCode();
			//if (responseCode == HttpURLConnection.HTTP_OK) {
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			return response.toString();
		}catch(IOException e){
			//e.printStackTrace();
			return "error: catch error";
		}
	}

	public static String sendGetWithAuth(String url, String token) {
		return sendRequestWithAuth(url, "GET", token);
	}
	
	public static String sendPostWithAuth(String url, String token) {
		return sendRequestWithAuth(url, "POST", token);
	}

	private static String sendRequestWithAuth(String url, String requestMethod, String token) {
		try {
			URL urlObj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
			con.setRequestMethod(requestMethod);
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setRequestProperty("Authorization", "Bearer "+token);
			int responseCode = con.getResponseCode();
			//if (responseCode == HttpURLConnection.HTTP_OK) {
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
	
			return response.toString();
		}catch(IOException e){
			//e.printStackTrace();
			return "error: catch error";
		}		
	}
}
