/*
   Copyright 2012 Matt Weagle (mweagle@gmail.com)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.logpig.mweagle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class Main {
	
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	private static final int		LOG_MESSAGE_COUNT = 1000;
	
	public static void main(String [] args)
	{
		System.out.println("Logpig testing");
		
		final Random magicMaker = new Random(System.nanoTime());
		for (int i=0; i != com.logpig.mweagle.Main.LOG_MESSAGE_COUNT; ++i)
		{
			final String message = String.format("Guess %d is that the magic number is: %d", i, magicMaker.nextInt(10));
			logger.info(message);
		}
		// Wait for them to finish...
	     try {
	    	 	System.out.println("Sleeping...");
	    	 	Thread.sleep(5000);
		} catch (InterruptedException e) {
			// NOP
		}
		System.out.println("Shutting down");
	}
}
