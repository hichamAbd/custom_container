package ejbs;


import org.isima.ejb.annotations.PersistenceContext;
import org.isima.ejb.annotations.PostConstruct;
import org.isima.ejb.annotations.PreDestroy;
import org.isima.ejb.annotations.Stateless;
import org.isima.ejb.entityManagement.EntityManager;

import ejbs.Local.LocalEJBInterfaceStateless;
import entites.BEntity;
import entites.CEntity;

@Stateless
public class EJBClassStateless implements LocalEJBInterfaceStateless {
	
	@PersistenceContext
	private EntityManager em;
	
	private BEntity b = new BEntity(1, 2d, "BEntity in stateless");
	private CEntity c = new CEntity(1, 3d, "CEntity in stateless");

	@Override
	public void aBusinessMethod() {
		System.out.println("[Stateless] Business Method from Stateless ejb : "+this.toString());
		em.remove(b);
		em.merge(c);
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
