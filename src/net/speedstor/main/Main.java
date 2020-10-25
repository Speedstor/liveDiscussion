package net.speedstor.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import net.speedstor.control.Cmd;
import net.speedstor.control.Log;
import net.speedstor.control.Settings;
import net.speedstor.discussion.DiscussionHandler;
import net.speedstor.server.Server;
import net.speedstor.websocket.WebSocketHandler;

public class Main {
	static Main main;
	
	//status
	boolean threadStatus_1 = false;
	
	
	public static void main(String[] args){
		System.out.println("------------------------------------------");
		System.out.println(" Canvas Bot 0.5                            ");
		System.out.println("------------------------------------------");

		FileWriter fr;
		try {
			fr = new FileWriter(new File(Settings.DOCS_LOC, "log_"+Clock.systemDefaultZone().instant().toString().substring(0, 10))+".txt", true);
			fr.write("\n"+"------------------------------------------\n Canvas Bot 0.5                            \n------------------------------------------");
			fr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		main = new Main();
		main.start();
	}
	
	//threads
	//Thread loopCheckThread;
	//Object[][] tokenList;
	

	Instant serverStartTime;

	Cmd cmd;
	Log log;
	Cache cache;
	Clock clock;
	Server server;
	WebSocketHandler websocketHandler;
	TokenHandler tokenHandler;
	DiscussionHandler discussionHandler;

	Thread serverThread;
	Thread cmdThread;
	
	public void start() {
		clock = Clock.systemDefaultZone();
		double timeStart = clock.millis();
		serverStartTime = clock.instant();
		
		log = new Log(clock);	
		
		cache = new Cache(log);
		
		tokenHandler = new TokenHandler(log, false);
		
		websocketHandler = new WebSocketHandler(log, tokenHandler);
		
		discussionHandler = new DiscussionHandler(log, clock, tokenHandler, websocketHandler);

		int port = 40;
		server = new Server(log, clock, port, timeStart, websocketHandler, tokenHandler, discussionHandler, cache);
		serverThread = new Thread(server);
		serverThread.start();

		cmd = new Cmd(main, log, server, websocketHandler, tokenHandler, discussionHandler);
		cmdThread = new Thread(cmd);
		cmdThread.start();

		log.log("Started @" + (clock.millis() - timeStart) + "ms");
	}
	
	
	public void stop() {
		((Server) server).setRunning(false);
		log.logStop("Stopping Server...");
		while(((Server) server).ifRunning()) {
	        try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				log.error("interrupted server closing");
			}
		}
		cache.save();
		long runTimeSecond = Duration.between(serverStartTime, clock.instant()).getSeconds();
		int runTimeMinute = (int) runTimeSecond / 60;
		int runTimeMinSec = (int) runTimeSecond % 60;
		log.logStop("Server stopped; run time: @"+runTimeMinute+"m"+runTimeMinSec+"s");
		
		log.programStop();
		System.exit(0);
	}
}
