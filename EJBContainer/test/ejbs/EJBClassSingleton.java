package ejbs;


import org.isima.ejb.annotations.*;
import org.isima.ejb.entityManagement.EntityManager;

import ejbs.Local.LocalEJBInterfaceSingleton;
import entites.AEntity;
import entites.BEntity;

@Singleton
@Startup
public class EJBClassSingleton implements LocalEJBInterfaceSingleton{
	
	@PersistenceContext
	private EntityManager em;
	
	private AEntity a = new AEntity(2, 1d, "AEntity in singleton");
	private BEntity b = new BEntity(2, 2d, "BEntity in singleton");
	
	@Override
	public void aBusinessMethod() {
		System.out.println("[Singleton] Business Method from Singleton ejb : "+this.toString());
		em.persist(a);
		em.merge(b);
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
