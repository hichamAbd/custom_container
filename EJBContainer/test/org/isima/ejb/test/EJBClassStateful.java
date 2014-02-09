package org.isima.ejb.test;

import org.isima.ejb.annotations.PersistenceContext;
import org.isima.ejb.annotations.PostActivate;
import org.isima.ejb.annotations.PostConstruct;
import org.isima.ejb.annotations.PreDestroy;
import org.isima.ejb.annotations.PrePassivate;
import org.isima.ejb.annotations.Remove;
import org.isima.ejb.annotations.Stateful;
import org.isima.ejb.entityManagement.EntityManager;

@Stateful
public class EJBClassStateful implements LocalEJBInterfaceStateful{
	@PersistenceContext
	public EntityManager em;

	@Override
	public void aBusinessMethod() {
		System.out.println("[Stateful] Business Method from stateful ejb : "+this.toString());
	}
	
	@PostConstruct
	private void init(){
		System.out.println("[Stateful] PostConstruct on Stateful : "+this.toString());
	}
	
	@PostActivate
	private void refresh(){
		System.out.println("[Stateful] PostActivate on Stateful : "+this.toString());
	}
	
	@PrePassivate
	private void preparePassivate(){
		System.out.println("[Stateful] PrePassivate on Stateful : "+this.toString());
	}
	
	@PreDestroy
	private void prepareDestroy(){
		System.out.println("[Stateful] PreDestroy on Stateful : "+this.toString());
	}
	
	@Remove
	public void removeSatefull(){
		System.out.println("[Stateful] Remove on Stateful : "+this.toString());
	}
	
}
