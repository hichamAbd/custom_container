package org.isima.ejb.entityManagement;

import org.isima.ejb.persistenceUnitSchema.Persistence;

public abstract class EntityManager{
	private Persistence persistence;
	
	public EntityManager(Persistence persistenceUnit){
		this.persistence = persistenceUnit;
	}
	public abstract void persist(Object entity);
	public abstract void remove(Object entity);
	public abstract <T> T find(Class<T> entityClass, Object primaryKey);
	public abstract <T> T merge(T entity);
	
	protected Persistence getPersistence() {
		return persistence;
	}
	
	protected void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}
	
	
}
