package com.hourglassapps.util;

import java.io.IOException;

import com.googlecode.jpingy.Ping;
import com.googlecode.jpingy.PingResult;
import com.googlecode.jpingy.Ping.Backend;
import com.googlecode.jpingy.PingArguments;

public class MainHeartBeat {
	private final static String TAG=MainHeartBeat.class.getName();

	//In seconds
	private final static int TIMEOUT=60;
	//In ms
	private final static int PERIOD=60*1000;
	private final static String TARGET="google.com";
	
	private final int mPids[];
	
	private final static PingArguments mPingArgs=new PingArguments.Builder().url(TARGET).
			timeout(TIMEOUT).bytes(4).count(1).ttl(60).interval(60*1000).build();
	
	public MainHeartBeat(int[] pPids) {
		mPids=pPids;
	}

	public void signal() throws IOException {
		String cmdArgs[]=new String[]{"kill", "-HUP",""};
		for(int pid: mPids) {
			cmdArgs[2]=Integer.toString(pid);
			Runtime.getRuntime().exec(cmdArgs);
		}
	}
	
	public void beat() {
		while(true) {
			try {
				Ping.ping(mPingArgs, Backend.UNIX);
				Thread.sleep(PERIOD);
			} catch(Exception e) {
				Log.e(TAG, "problem pinging "+TARGET);
				break;
			}
		}
		try {
			signal();
		} catch(IOException e) {
			Log.e(TAG, e);
		}
	}
	
	public static void main(String[] pArgs) {
		int pids[]=new int[pArgs.length];
		int pidIdx=0;
		for(String arg: pArgs) {
			pids[pidIdx]=Integer.parseInt(arg);
			pidIdx++;
		}
		
		new MainHeartBeat(pids).beat();
	}
}
