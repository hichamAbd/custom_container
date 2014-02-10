package org.isima.ejb.logging;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingConfiguratorFactory {
	
	private Level level;
	private Logger logger;
	
	public LoggingConfiguratorFactory(Logger log,Level lvl){			
	    this.logger = log;
	    log.setLevel(lvl);
	    this.logger.setLevel(lvl);
		this.level = lvl;
		

		File level0 = new File("log");
		if(!level0.exists()){
			level0.mkdir();
		}else{
			if(level0.isDirectory()){
				deleteRecusiveDirectory(level0);
				level0.mkdir();
			}else{
				//log is an exisiting file .... what to do ?
				//FIXME do some logique and don't let logs carsh after this
			}
		}
		File level11 = new File("log"+File.separator+"xml");
		if(!level11.exists()){
			level11.mkdir();
		}
		
		File level10 = new File("log"+File.separator+"txt");
		if(!level10.exists()){
			level10.mkdir();
		}
		
	}
	
	public XMLLoggingConfig applyXMLLoggingConfig(){
		XMLLoggingConfig xmlLoggingConfig = new XMLLoggingConfig();
		xmlLoggingConfig.setup(logger,level);
		return xmlLoggingConfig;
	}
	
	public SimpleLoggingConfig applySimpleLoggingConfig(){
		SimpleLoggingConfig simpleLoggingConfig = new SimpleLoggingConfig();
		simpleLoggingConfig.setup(logger,level);
		return simpleLoggingConfig;
	}
	
	private void deleteRecusiveDirectory(File directory){
		if(directory.isDirectory()){
			if(directory.listFiles().length != 0){
				for(File file : directory.listFiles()){
					if(file.isDirectory()){
						deleteRecusiveDirectory(file);
					}else{
						file.delete();
					}
				}
			}else{
				directory.delete();
			}			
		}
	}
}
