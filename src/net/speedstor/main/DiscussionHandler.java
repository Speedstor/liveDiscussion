package net.speedstor.main;

import java.util.ArrayList;

public class DiscussionHandler implements Runnable{
	Log log;
	
	ArrayList<String> participantList = new ArrayList<String>();
	
	public DiscussionHandler(Log log, String url) {
		this.log = log;
	}
	
	public int addParticipant(String token) {
		participantList.add(token);
		log.log(""+participantList.size());
		return 1;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
