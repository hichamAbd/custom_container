package org.isima.ejb.test;

import org.isima.ejb.annotations.Local;

@Local
public interface LocalEJBInterfaceSingleton {

	public void aBusinessMethod();
	public void prepareDestroy();
}