package com.ss.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RelogLog {
	private static Logger logger = LoggerFactory.getLogger(RelogLog.class.getName() + ".record");

	public static void record(String msg) {

		logger.info(msg);
	}

	public static void main(String[] args) {
		RelogLog.record("cs=============================");
	}

}
