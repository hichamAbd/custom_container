package org.isima.ejb.entityManagement;

import java.lang.annotation.Annotation;

import org.isima.ejb.annotations.Entity;
import org.isima.ejb.exception.PersistanceException;
import org.isima.ejb.persistenceUnitSchema.Persistence;

public class ConcreteEntityManager extends EntityManager {
	private EntityParser entityParser;

	public ConcreteEntityManager(Persistence persistenceUnit) {
		super(persistenceUnit);
		this.entityParser = new EntityParser();
	}

	@Override
	public void persist(Object entity) {
		try{
			boolean isEntity;
			if(entity == null){
				throw new PersistanceException("Persistence context exception: Trying to persist null object");
			}else{
				isEntity = verifyEntityType(entity);
				if(isEntity){
					this.entityParser.parseEntity(entity);
					System.out.println("Persiting "+entity.toString()+" using em: "+this.toString());			
				}else{
					throw new PersistanceException("Persistence context exception: Trying to persist non Entity object");
				}
			}
		}catch(PersistanceException e){
			e.printStackTrace();
		}
	}

	@Override
	public void remove(Object entity) {
		try{
			boolean isEntity;
			if(entity == null){
				throw new PersistanceException("Persistence context exception: Trying to remove null object");
			}else{
				isEntity = verifyEntityType(entity);
				if(isEntity){
					this.entityParser.parseEntity(entity);
					System.out.println("Removing "+entity.toString()+" using em: "+this.toString());	
				}else{
					throw new PersistanceException("Persistence context exception: Trying to remove non Entity object");
				}
			}
		}catch(PersistanceException e){
			e.printStackTrace();
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "finally" })
	public <T> T find(Class<T> entityClass, Object primaryKey) {
		boolean isEntity = verifyEntityClassAnnotation(entityClass);
		try{
			if(isEntity){
				System.out.println("Try to find a "+entityClass.getSimpleName()+" with primaryKey:"+primaryKey+" using em: "+this.toString());	
				return (T) Class.forName(entityClass.getName()).newInstance();
			}else{
				throw new PersistanceException("Persistence context exception: Trying to find Entity for not Entity class");
			}
		}catch(PersistanceException e){
			e.printStackTrace();
		}finally{
			return null;
		}
	}

	@Override
	public <T> T merge(T entity) {
		try{
			boolean isEntity;
			if(entity == null){
				throw new PersistanceException("Persistence context exception: Trying to merge null object");
			}else{
				isEntity = verifyEntityType(entity);
				if(isEntity){
					this.entityParser.parseEntity(entity);
					System.out.println("Merging "+entity.toString()+" using em: "+this.toString());		
				}else{
					throw new PersistanceException("Persistence context exception: Trying to merge non Entity object");
				}
			}
		}catch(PersistanceException e){
			e.printStackTrace();
		}
		return entity;
	}
	
	private <T> boolean verifyEntityType(T entity){
		for(Annotation annotation : entity.getClass().getAnnotations()){
			if(annotation instanceof Entity){
				return true;
			}
		}
		return false;
	}
	
	private <T> boolean verifyEntityClassAnnotation(Class<T> entityClass){
		for(Annotation annotation : entityClass.getAnnotations()){
			if(annotation instanceof Entity){
				return true;
			}
		}
		return false;
	}

}
