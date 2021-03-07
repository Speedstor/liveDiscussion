package net.speedstor.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import net.speedstor.control.Log;
import net.speedstor.control.Settings;

public class Cache {
	HashMap<String, HashMap<String, String>> cached;
	Log log;
	
	public Cache(Log log) {
		cached = new HashMap<>();
		this.log = log;
		retrieveCache();
	}
	
	//#region manipulation categories
	public boolean defineCategory(String name) {
		if(cached.containsKey(name)) {
			//log.warn("creating category that is already defined");
			return false;
		}else {			
			cached.put(name, new HashMap<>());
			return true;
		}
	}
	
	public boolean ifCategoryExist(String name) {
		return cached.containsKey(name);
	}
	//#endregion
	
	//#region put data
	public boolean putData(String category, String key, String value) {
		if(!cached.containsKey(category)) return false;
		cached.get(category).put(key, value);
		return true;
	}
	
	public boolean categoryContainKey(String category, String key) {
		if(ifCategoryExist(category)) {			
			return cached.get(category).containsKey(key);
		}else {
			return false;
		}
	}
	
	public boolean categoryContainValue(String category, String value) {
		if(ifCategoryExist(category)) {			
			return cached.get(category).containsValue(value);
		}else {
			return false;
		}
	}
	//#endregion
	
	//#region getting data
	public HashMap<String, String> getCategoryWhole(String category){
		return cached.get(category);
	}
	
	public String getCachedData(String category, String key) {
		return cached.get(category).get(key);
	}
	//#endregion
	
	//#region save cache
	public void retrieveCache(){
		Scanner sc;
		try {
			sc = new Scanner(new File(Settings.DOCS_LOC, Settings.CACHE_FILE_NAME));
			String line;
			while (sc.hasNextLine()) {
				 line = sc.nextLine();
				 if(line.length() > 2) {
					 line = line.substring(0, line.length()-1); //remove \n
					 int firstSeperatorIndex = line.indexOf(":");
					 int secondSeperatorIndex = line.indexOf(":", firstSeperatorIndex+1);
					 if(firstSeperatorIndex > -1 && secondSeperatorIndex > -1) {
						 String categoryName = line.substring(0, firstSeperatorIndex);
						 String itemKey = line.substring(firstSeperatorIndex + 1, secondSeperatorIndex);
						 String itemValue = line.substring(secondSeperatorIndex+1);
						 defineCategory(categoryName);
						 putData(categoryName, itemKey, itemValue);
					 }else {
						 log.warn("cache store corrupted");
					 }
				 }
			}
			log.log("Cache store retrieved from file");
		} catch (FileNotFoundException e) {
			log.error("retrieve cache from file error");
		}
	}
	
	public boolean save() {
		FileWriter fr;
		try {
			fr = new FileWriter(new File(Settings.DOCS_LOC, Settings.CACHE_FILE_NAME), false);
			for(String categoryName : cached.keySet()) {
				if(!categoryName.equals("discussionLists")) {					
					for(Map.Entry<String, String> category : cached.get(categoryName).entrySet()) {
						fr.write("\n"+categoryName+":"+category.getKey()+":"+category.getValue());
					}
				}
			}
			fr.close();
			log.log("Saved Cache in file");
		} catch (IOException e) {
			log.error("write cache file error");
		}
		return true;
	}
	//#endregion
}
