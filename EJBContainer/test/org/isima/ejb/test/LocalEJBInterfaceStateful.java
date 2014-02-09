package org.isima.ejb.test;

import org.isima.ejb.annotations.Local;

@Local
public interface LocalEJBInterfaceStateful {

	public void aBusinessMethod();
	public void removeSatefull();
}
