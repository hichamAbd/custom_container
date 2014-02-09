package org.isima.ejb.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SimpleLoggingConfig implements LoggingConfigurator{
	private static final String LOG_FILE_PATH = "log"+File.separator+"txt"+File.separator;
	
	@Override
	public void setup(Logger logger,Level lvl) {
		try {			
			//Getting the file handler
			File f = new File(LOG_FILE_PATH+logger.getName().replace(".", "_")+".logs.txt");
			f.createNewFile();
			FileHandler outputFileHandler = new FileHandler(LOG_FILE_PATH+logger.getName().replace(".", "_")+".logs.txt");
			
			//Setting the formater to SimpleFormatter
			outputFileHandler.setFormatter(new SimpleFormatter());
			
			//Adding handler to logs
			logger.addHandler(outputFileHandler);
			
		} catch (SecurityException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
