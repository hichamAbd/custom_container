package org.isima.ejb.logging;

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
}
