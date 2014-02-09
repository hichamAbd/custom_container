package org.isima.ejb.test;

import org.isima.ejb.annotations.EJB;

public class ClassWithSingleton {

	@EJB
	private LocalEJBInterfaceSingleton singleton;

	public LocalEJBInterfaceSingleton getSingleton() {
		return singleton;
	}

	public ClassWithSingleton() {
		super();
	}
	
	
}
