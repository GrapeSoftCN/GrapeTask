package test;

import httpServer.booter;

public class TestTask {
	public static void main(String[] args) {
		 booter booter = new booter();
		 try {
		 System.out.println("GrapeTask!");
		 System.setProperty("AppName", "GrapeTask");
		 booter.start(1003);
		} catch (Exception e) {
		}
	}
}
