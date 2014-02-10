package main;


import org.isima.ejb.annotations.EJB;

import ejbs.Local.LocalEJBInterfaceSingleton;

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
