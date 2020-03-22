package net.speedstor.main;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import net.speedstor.outdated.LoopedCheck;

public class Main {
	static Main main;
	LoopedCheck loopCheck;
	
	//status
	boolean threadStatus_1 = false;
	
	
	public static void main(String[] args){
		System.out.println("------------------------------------------");
		System.out.println(" Canvas Bot 0.5                            ");
		System.out.println("------------------------------------------");
		main = new Main();
		main.start();
	}
	
	//threads
	//Thread loopCheckThread;
	//Object[][] tokenList;
	

	Instant serverStartTime;

	Cmd cmd;
	Log log;
	Clock clock;
	Server server;
	WebSocketHandler websocketHandler;

	Thread serverThread;
	Thread cmdThread;
	
	public void start() {
		clock = Clock.systemDefaultZone();
		double timeStart = clock.millis();
		serverStartTime = clock.instant();
		
		
		//log
		log = new Log(clock);	
		
		//websockethandler
		websocketHandler = new WebSocketHandler(log);

		//server
		int port = 40;
		server = new Server(log, clock, port, timeStart, websocketHandler);
		serverThread = new Thread(server);
		serverThread.start();

		cmd = new Cmd(main, log, server, websocketHandler);
		cmdThread = new Thread(cmd);
		cmdThread.start();

		log.log("Started @" + (clock.millis() - timeStart) + "ms");
		
		//canvas email bot
		/*
		//get list
		tokenList = new Object[1][6];
		
		//tokenList[0] = new Object[6];
		tokenList[0][0] = new String("8918~fRvcPzh1Z3hXqq5Z61DaPSmxS1Hrw2mJEPJmw5Q0ts0Nvcd9mEGo5E1H5XVEvRfJ");
		
		//start loop
		loopCheck = new LoopedCheck(tokenList);
		if(loopCheck.getCheckList() == tokenList) {
			loopCheckThread = new Thread(loopCheck);
			loopCheckThread.start();
			threadStatus_1 = true;
		}	
		*/
	}
	
	
	public void stop() {
		((Server) server).setRunning(false);
		log.logStop("Stopping Server...");
		while(((Server) server).ifRunning()) {
			
		}
		long runTimeSecond = Duration.between(serverStartTime, clock.instant()).getSeconds();
		int runTimeMinute = (int) runTimeSecond / 60;
		int runTimeMinSec = (int) runTimeSecond % 60;
		log.logStop("Server stopped; run time: @"+runTimeMinute+"m"+runTimeMinSec+"s");
		
		log.programStop();
		System.exit(0);
	}
}
