package main;


import org.isima.ejb.annotations.EJB;
import org.isima.ejb.container.EJBContainer;
import static org.junit.Assert.*;
import org.junit.Test;

import ejbs.Local.LocalEJBInterfaceSingleton;
import ejbs.Local.LocalEJBInterfaceStateful;
import ejbs.Local.LocalEJBInterfaceStateless;
public class EJBContainerTester {
	@EJB
	private LocalEJBInterfaceStateful ejbStateful;
	@EJB
	private LocalEJBInterfaceStateless ejbStateless;
	@EJB
	private LocalEJBInterfaceSingleton ejbSingleton;
	

	@Test
	public void testContainer() throws Exception {

		ClassWithSingleton cws = new ClassWithSingleton();
		//Testing injection
		EJBContainer.getInstance().inject(this);
		EJBContainer.getInstance().inject(cws);
		
		assertTrue(ejbStateful instanceof LocalEJBInterfaceStateful);
		assertTrue(ejbStateless instanceof LocalEJBInterfaceStateless);
		assertTrue(ejbSingleton instanceof LocalEJBInterfaceSingleton);
		assertEquals(ejbSingleton,cws.getSingleton());
		
		//Testing lifecycle
		
		//Testing method access
		ejbStateful.aBusinessMethod();
		ejbStateless.aBusinessMethod();
		ejbSingleton.aBusinessMethod();

		Thread.sleep(5000); //To see the passivating and activating on stateful, and killing on stateless
		ejbStateful.aBusinessMethod();
		Thread.sleep(4000);
		
		//Test on call of dead stateless (re-injection)
		ejbStateless.aBusinessMethod();

		//Testing stateful remove
		ejbStateful.removeSatefull();
		ejbStateless.aBusinessMethod();

		//Testing singleton destroy
		ejbSingleton.prepareDestroy();
	}

}
