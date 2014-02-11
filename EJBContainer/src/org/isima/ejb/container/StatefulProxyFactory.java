package org.isima.ejb.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.isima.ejb.annotations.PersistenceContext;
import org.isima.ejb.annotations.PostActivate;
import org.isima.ejb.annotations.PostConstruct;
import org.isima.ejb.annotations.PreDestroy;
import org.isima.ejb.annotations.PrePassivate;
import org.isima.ejb.annotations.Remove;
import org.isima.ejb.entityManagement.ConcreteEntityManager;
import org.isima.ejb.entityManagement.EntityManager;
import org.isima.ejb.exception.AnnotationException;
import org.isima.ejb.exception.PersistanceException;
import org.isima.ejb.logging.LoggingConfiguratorFactory;
import org.isima.ejb.persistenceUnitSchema.Persistence;

public class StatefulProxyFactory implements InvocationHandler {
	private final Logger logger;
	private static final Level level = Level.ALL;
	
	private static final long PASSIVATING_DELAY_SECONDS = 4l;
	private static final String STATEFUL_READY_STATE = "ready";
	private static final String STATEFUL_PASSIVE_STATE = "passive";
	private static final String STATEFUL_KILLED_STATE = "killed";
	private static final String PRE_PASSIVATE_METHOD = "prepassivate";
	private static final String POST_ACTIVATE_METHOD = "postactivate";
	private static final String POST_CONSTRUCT_METHOD = "postconstruct";
	private static final String PRE_DESTROY_METHOD = "predestroy";
	private static final String REMOVE_METHOD = "remove";
	
	private static class Passivator implements Runnable{
		StatefulProxyFactory proxy;
		Logger log;
		
		public Passivator(StatefulProxyFactory proxy,Logger logger){
			this.proxy = proxy;
			this.log = logger;
		}

		@Override
		public void run() {
			try{
				if( (this.proxy.getLastTimeCalled() < (System.currentTimeMillis() - (PASSIVATING_DELAY_SECONDS * 1000l)) ) ){
						if(this.proxy.getState().equals(STATEFUL_READY_STATE)){
								this.log.info("Stateful getting passivated, because unaccessed since "+ ((System.currentTimeMillis() - this.proxy.getLastTimeCalled())/1000l)+" seconds" );
								this.proxy.setState(STATEFUL_PASSIVE_STATE);
						}
				}

			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	
	private final ScheduledExecutorService passivationScheduler = 	Executors.newScheduledThreadPool(1);
	private Map<String, Method> methods;
	private Set<String> methodsNames;
	private static long lastTimeCalled;
	private Passivator passivator;
	Object ejbImplementation;
	Persistence persistence;
	private String state;

	
	private StatefulProxyFactory(Object obj,Persistence persistenceUnit){
		this.ejbImplementation = obj;
		
		//Setting logs
		logger = Logger.getLogger(this.ejbImplementation.getClass().getName());
		LoggingConfiguratorFactory lcf = new LoggingConfiguratorFactory(logger,level);
		lcf.applySimpleLoggingConfig();
		lcf.applyXMLLoggingConfig();
		
		//Stateful object ready at creation
		state = STATEFUL_READY_STATE;
		
		//Setting lastUsedTime
		lastTimeCalled = System.currentTimeMillis();
		
		//Setting the passivator
		passivator = new Passivator(this,this.logger);
		
		//Gettin PostContruct PrePassivate PostActivate PreDestroy and remove Methods
		methods = new HashMap<>();
		methodsNames = new HashSet<>();		
		try {
			logger.info("Stateful getting PostContruct PrePassivate PostActivate PreDestroy and remove Methods from class: "+ejbImplementation.getClass().getName());
			for(Method method : obj.getClass().getDeclaredMethods()){
				method.setAccessible(true);
				for(Annotation annotation : method.getAnnotations() ){
					if(annotation instanceof PrePassivate){
						logger.fine("Found PrePassivate method in "+ejbImplementation.getClass().getName()+" method name: "+method.getName());
						methods.put(PRE_PASSIVATE_METHOD, method);
						methodsNames.add(method.getName());
					}else if(annotation instanceof PostActivate){
						logger.fine("Found PostActivate method in "+ejbImplementation.getClass().getName()+" method name: "+method.getName());
						methods.put(POST_ACTIVATE_METHOD, method);	
						methodsNames.add(method.getName());				
					}else if(annotation instanceof PostConstruct){
						logger.fine("Found PostConstruct method in "+ejbImplementation.getClass().getName()+" method name: "+method.getName());
						methods.put(POST_CONSTRUCT_METHOD, method);	
						methodsNames.add(method.getName());				
					}else if(annotation instanceof PreDestroy){
						logger.fine("Found PreDestroy method in "+ejbImplementation.getClass().getName()+" method name: "+method.getName());
						methods.put(PRE_DESTROY_METHOD, method);	
						methodsNames.add(method.getName());				
					}else if(annotation instanceof Remove){
						logger.fine("Found Remove method in "+ejbImplementation.getClass().getName()+" method name: "+method.getName());
						methods.put(REMOVE_METHOD, method);	
						methodsNames.add(method.getName());				
					}
				}
			}
			logger.info("Stateful getting PostContruct PrePassivate PostActivate PreDestroy and remove Methods: found"+ methodsNames.size());
			
			//Getting the EntityManagerField
			logger.info("Injection of EntityManager instance in Statefull("+ejbImplementation.toString()+")");
			this.persistence = persistenceUnit;
			Constructor<?> entityManagerConstructor = Class.forName(ConcreteEntityManager.class.getName()).getConstructor(Persistence.class);
			for(Field ejbField : obj.getClass().getDeclaredFields()){
				ejbField.setAccessible(true);
				for(Annotation annotation : ejbField.getAnnotations()){
					if(annotation instanceof PersistenceContext){
						if( (ejbField.getType().equals(EntityManager.class)) && this.persistence != null){
							ConcreteEntityManager entityManager = (ConcreteEntityManager) entityManagerConstructor.newInstance(this.persistence);
							logger.info("Injecting instance ("+entityManager.toString()+") of EntityManager in "+ejbImplementation.toString());
							ejbField.set(obj, entityManager);
						}else if(this.persistence == null){
							PersistanceException e = new PersistanceException("Persistence file not found: Couldn't find persistence.xml file in META-INF");
							logger.severe("Sever Error("+e.getMessage()+")");
							throw e;
						}else{
							AnnotationException e = new AnnotationException("Unexpected annotation exception: expected "+EntityManager.class.getSimpleName()+" field type but found "+ejbField.getType().getSimpleName());
							logger.severe("Sever Error("+e.getMessage()+")");
							throw e;
						}
					}
				}
			}
		
			//Invoking POST_CONSTRUCT_METHOD Method
			if(methods.get(POST_CONSTRUCT_METHOD) != null){
				logger.info("Invoking Stateful PostConstruct method on EJB("+ejbImplementation.toString()+")");
				this.invoke(this.getEjbImplementation(), methods.get(POST_CONSTRUCT_METHOD), null);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		//Clocking state changes
		logger.warning("Scheduling Stateful passivation on EJB("+ejbImplementation.toString()+") @Rate: every "+PASSIVATING_DELAY_SECONDS+" seconds");
		passivationScheduler.scheduleAtFixedRate(this.passivator,PASSIVATING_DELAY_SECONDS,PASSIVATING_DELAY_SECONDS,TimeUnit.SECONDS);
	}
	
	public static Object proxyInstance(Field ejbField,Class<?> implementingClass,Persistence persistenceUnit) throws IllegalArgumentException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		return Proxy.newProxyInstance(ejbField.getType().getClassLoader(), new Class[]{ejbField.getType()}, new StatefulProxyFactory(Class.forName(implementingClass.getName()).newInstance(),persistenceUnit));
	}

	private String getState() {
		return state;
	}

	private long getLastTimeCalled() {
		return lastTimeCalled;
	}

	private void setState(String stateToSet) throws Throwable {
		
		if(stateToSet.equals(STATEFUL_READY_STATE)){
			logger.info("Changing Statful state to Ready on EJB("+ejbImplementation.toString()+")");
			this.state = STATEFUL_READY_STATE;
			//Calling POST_ACTIVATE_METHOD
			if(methods.get(POST_ACTIVATE_METHOD)!=null){
				logger.info("Invoking Statful PostActivate method on EJB("+ejbImplementation.toString()+")");
				this.invoke(this.getEjbImplementation(), methods.get(POST_ACTIVATE_METHOD), null);
			}
		}else if(stateToSet.equals(STATEFUL_PASSIVE_STATE)){
			logger.info("Changing Statful state to Passive on EJB("+ejbImplementation.toString()+")");			
			this.state = STATEFUL_PASSIVE_STATE;
			
			if(methods.get(PRE_PASSIVATE_METHOD)!=null){
				logger.info("Invoking Statful PrePassivate method on EJB("+ejbImplementation.toString()+")");
				this.invoke(this.getEjbImplementation(), methods.get(PRE_PASSIVATE_METHOD), null);
			}
		}else if(stateToSet.equals(STATEFUL_KILLED_STATE)){
			this.state = STATEFUL_KILLED_STATE;
			this.passivationScheduler.shutdown();
			this.ejbImplementation = null;
			
		}
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		boolean isEJBAnnotated = testAnnotatedMethod(method.getName());
		if(!isEJBAnnotated){
			//Update lastCalled time
			logger.finer("Statful on Called EJB("+ejbImplementation.toString()+")");	
			lastTimeCalled = System.currentTimeMillis();			
		}
		
		//Change state to active if has been passivated
		if(this.getState().equals(STATEFUL_PASSIVE_STATE)){
			//The PrePassivate doesn't activate the ejb
			if(methods.get(PRE_PASSIVATE_METHOD) != null){
				if(!method.getName().equals(methods.get(PRE_PASSIVATE_METHOD).getName())){
					this.setState(STATEFUL_READY_STATE);
				}
			}else{
				this.setState(STATEFUL_READY_STATE);
			}
		}else if(this.getState().equals(STATEFUL_KILLED_STATE) && isEJBAnnotated==false){
				IllegalStateException ise =  new IllegalStateException("LifeCycle Exception: Statefull instance already removed");
				logger.severe("Severe Error("+ise.getMessage()+")");
				throw ise;
		}
		logger.finer("Invoking method: "+method.getName()+" on Stateful EJB("+ejbImplementation.toString()+") using args: "+Arrays.toString(args));
		if(Object.class  == method.getDeclaringClass()) {
		       String name = method.getName();
		       if("equals".equals(name)) {
		           return proxy == args[0];
		       } else if("hashCode".equals(name)) {
		           return System.identityHashCode(proxy);
		       } else if("toString".equals(name)) {
		           return proxy.getClass().getName() + "@" +
		               Integer.toHexString(System.identityHashCode(proxy)) +
		               ", with InvocationHandler " + this;
		       } else {
		    	    IllegalStateException ise =  new IllegalStateException("Invoking method on EJB error: "+String.valueOf(method));
					logger.severe("Severe Error("+ise.getMessage()+")");
					throw ise;
		       }
		   }
			
		if(methods.get(REMOVE_METHOD) !=null){	
		   if(methods.get(REMOVE_METHOD).getName().equals(method.getName())){
			   logger.info("Changing Statful state to Killed on EJB("+ejbImplementation.toString()+")");	
			   if(methods.get(PRE_DESTROY_METHOD)!=null){
				   logger.info("Invoking Statful PreDestroy method on EJB("+ejbImplementation.toString()+")");
				   methods.get(PRE_DESTROY_METHOD).invoke(ejbImplementation, args);
				}
				logger.info("Invoking Statful Remove method on EJB("+ejbImplementation.toString()+")");
			    method.invoke(ejbImplementation, args);
			    this.setState(STATEFUL_KILLED_STATE);
			    return null;
		   }
		}
		   
		return method.invoke(ejbImplementation, args);
	}

	private Object getEjbImplementation() {
		return ejbImplementation;
	}
	
	private boolean testAnnotatedMethod(String methodName){
		for(String name : methodsNames){
			if(name.equals(methodName)){
				return true;
			}
		}
		return false;
	}
		
}
