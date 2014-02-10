package org.isima.ejb.entityManagement;

import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.isima.ejb.annotations.Entity;
import org.isima.ejb.exception.PersistanceException;
import org.isima.ejb.logging.LoggingConfiguratorFactory;
import org.isima.ejb.persistenceUnitSchema.Persistence;

public class ConcreteEntityManager extends EntityManager {
	private final Logger logger;
	private static final Level level = Level.ALL;
	
	private EntityParser entityParser;

	public ConcreteEntityManager(Persistence persistenceUnit) {
		super(persistenceUnit);
		
		//Setting logs
		logger = Logger.getLogger(EntityManager.class.getSimpleName()+"@"+System.identityHashCode(this));
		LoggingConfiguratorFactory lcf = new LoggingConfiguratorFactory(logger,level);
		lcf.applySimpleLoggingConfig();
		lcf.applyXMLLoggingConfig();
		this.entityParser = new EntityParser();
	}

	@Override
	public void persist(Object entity) {
		try{
			boolean isEntity;
			if(entity == null){
				PersistanceException persistanceException =  new PersistanceException("Persistence context exception: Trying to persist null object");
				logger.severe("Sever EntityManager Error("+persistanceException.getMessage()+")");
				throw persistanceException;
			}else{
				isEntity = verifyEntityType(entity);
				if(isEntity){
					this.entityParser.parseEntity(entity);
					logger.info("Persiting Entity("+entity.toString()+" in EntityManager : "+this.toString());
					System.out.println("Persiting "+entity.toString()+" using em: "+this.toString());			
				}else{
					PersistanceException persistanceException =  new PersistanceException("Persistence context exception: Trying to persist non Entity object");
					logger.severe("Sever EntityManager Error("+persistanceException.getMessage()+")");
					throw persistanceException;
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
				PersistanceException persistanceException =  new PersistanceException("Persistence context exception: Trying to remove null object");
				logger.severe("Sever EntityManager Error("+persistanceException.getMessage()+")");
				throw persistanceException;
			}else{
				isEntity = verifyEntityType(entity);
				if(isEntity){
					this.entityParser.parseEntity(entity);
					logger.info("Removing Entity("+entity.toString()+" in EntityManager : "+this.toString());
					System.out.println("Removing "+entity.toString()+" using em: "+this.toString());	
				}else{
					PersistanceException persistanceException =  new PersistanceException("Persistence context exception: Trying to remove non Entity object");
					logger.severe("Sever EntityManager Error("+persistanceException.getMessage()+")");
					throw persistanceException;
				}
			}
		}catch(PersistanceException e){
			e.printStackTrace();
		}
	}

	@Override
	@SuppressWarnings({"finally", "unchecked" })
	public <T> T find(Class<T> entityClass, Object primaryKey) {
		boolean isEntity = verifyEntityClassAnnotation(entityClass);
		try{
			if(isEntity){
				logger.info("Try to find an Entity("+entityClass.getSimpleName()+") using PrimaryKey("+primaryKey+") in EntityManager : "+this.toString());
				System.out.println("Try to find a "+entityClass.getSimpleName()+" with primaryKey:"+primaryKey+" using em: "+this.toString());	
				return (T) Class.forName(entityClass.getName()).newInstance();
			}else{
				PersistanceException persistanceException =  new PersistanceException("Persistence context exception: Trying to find an Entity from something which is not an Entity");
				logger.severe("Sever EntityManager Error("+persistanceException.getMessage()+")");
				throw persistanceException;
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
				PersistanceException persistanceException =  new PersistanceException("Persistence context exception: Trying to merge null object");
				logger.severe("Sever EntityManager Error("+persistanceException.getMessage()+")");
				throw persistanceException;
			}else{
				isEntity = verifyEntityType(entity);
				if(isEntity){
					this.entityParser.parseEntity(entity);
					logger.info("Merging Entity("+entity.toString()+" in EntityManager : "+this.toString());
					System.out.println("Merging "+entity.toString()+" using em: "+this.toString());		
				}else{
					PersistanceException persistanceException =  new PersistanceException("Persistence context exception: Trying to merge non Entity object");
					logger.severe("Sever EntityManager Error("+persistanceException.getMessage()+")");
					throw persistanceException;
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
