package test;

import esayhelper.TimeHelper;
import httpServer.booter;
import interfaceApplication.task;

public class TestTask {
	public static void main(String[] args) {
//		long start = TimeHelper.nowMillis();
//		System.out.println(start);
//		long end = Long.parseLong("1493177707000");
//		System.out.println((int)Math.ceil((double)(end-start)/(1000*60*60*24)));
		
//		String info = "{\"name\":\"测试任务\",\"timediff\":3,\"type\":1}";
//		System.out.println(new task().TaskAdd(info));
		
//		System.out.println(new task().TaskPage(1, 2));
		 booter booter = new booter();
		 try {
		 System.out.println("GrapeTask!");
		 System.setProperty("AppName", "GrapeTask");
		 booter.start(1003);
		} catch (Exception e) {
		}
	}
}
