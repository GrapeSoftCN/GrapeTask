package test;

import esayhelper.TimeHelper;

public class TestTask {
	public static void main(String[] args) {
		long start = TimeHelper.nowMillis();
		System.out.println(start);
		long end = Long.parseLong("1493177707000");
		System.out.println((int)Math.ceil((double)(end-start)/(1000*60*60*24)));
	}
}
