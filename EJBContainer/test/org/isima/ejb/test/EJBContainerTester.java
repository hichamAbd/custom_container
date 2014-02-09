package org.isima.ejb.test;

import static org.junit.Assert.*;

import org.isima.ejb.annotations.EJB;
import org.isima.ejb.container.EJBContainer;
import org.junit.Test;

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
		assertEquals(ejbSingleton, cws.getSingleton());

		//Testing lifecycle
		
		//Testing method access
		ejbStateless.aBusinessMethod();
		ejbStateful.aBusinessMethod();
		ejbSingleton.aBusinessMethod();

		Thread.sleep(6000); //To see the passivating and activating on stateful, and killing on stateless
		ejbStateful.aBusinessMethod();
		Thread.sleep(5000);
		//Testing stateful remove
		ejbStateful.removeSatefull();
		ejbSingleton.aBusinessMethod();
		ejbSingleton.prepareDestroy();
		System.out.println("End;");
	}

}
