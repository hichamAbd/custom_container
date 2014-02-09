package org.isima.ejb.test;

import org.isima.ejb.annotations.Local;

@Local
public interface LocalEJBInterfaceStateless {

	public void aBusinessMethod();
}
