package org.isima.ejb.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.XMLFormatter;

public class XMLLoggingConfig implements LoggingConfigurator{
	private static final String LOG_FILE_PATH = "log"+File.separator+"xml"+File.separator;
	
	public XMLLoggingConfig(){
		File level0 = new File("/","log");
		if(!level0.exists()){level0.mkdir();}
		File level1 = new File("log","xml");
		if(!level1.exists()){level0.mkdir();}
	}

	@Override
	public void setup(Logger logger,Level lvl) {
		try {				
			//Getting the file handler
			File f = new File(LOG_FILE_PATH+logger.getName().replace(".", "_")+"logs.xml");
			f.createNewFile();
			FileHandler outputFileHandler = new FileHandler(LOG_FILE_PATH+logger.getName().replace(".", "_")+"logs.xml");
			
			//Setting the formater to XMLFormatter
			outputFileHandler.setFormatter(new XMLFormatter());
			
			//Adding handler to logs
			logger.addHandler(outputFileHandler);
			
		} catch (SecurityException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
