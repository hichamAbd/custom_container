package org.isima.ejb.test;

import org.isima.ejb.annotations.PostConstruct;
import org.isima.ejb.annotations.PreDestroy;
import org.isima.ejb.annotations.Singleton;
import org.isima.ejb.annotations.Startup;

@Singleton
@Startup
public class EJBClassSingleton implements LocalEJBInterfaceSingleton{
	@Override
	public void aBusinessMethod() {
		System.out.println("[Singleton] Business Method from Singleton ejb : "+this.toString());
	}
	
	@PostConstruct
	private void init(){
		System.out.println("[Singleton] PostConstruct on Singleton : "+this.toString());
	}
	
	@PreDestroy
	public void prepareDestroy(){
		System.out.println("[Singleton] PreDestroy on Singleton : "+this.toString());
	}
}
