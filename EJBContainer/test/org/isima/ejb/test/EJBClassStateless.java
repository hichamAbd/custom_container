package org.isima.ejb.test;

import org.isima.ejb.annotations.PostConstruct;
import org.isima.ejb.annotations.PreDestroy;
import org.isima.ejb.annotations.Stateless;

@Stateless
public class EJBClassStateless implements LocalEJBInterfaceStateless {

	@Override
	public void aBusinessMethod() {
		System.out.println("[Stateless] Business Method from Stateless ejb : "+this.toString());
	}
	
	@PostConstruct
	private void init(){
		System.out.println("[Stateless] PostConstruct on Stateless : "+this.toString());
	}
	
	@PreDestroy
	private void prepareDestroy(){
		System.out.println("[Stateless] PreDestroy on Stateless : "+this.toString());
	}
}
