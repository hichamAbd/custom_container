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

public class StatelessProxyFactory implements InvocationHandler{
	private final Logger logger;
	private static final Level level = Level.ALL;
	
	private static final long KILLING_DELAY_SECONDS = 50l;
	private static final String STATELESS_READY_STATE = "ready";
	private static final String STATELESS_KILLED_STATE = "killed";
	private static final String POST_CONSTRUCT_METHOD = "postconstruct";
	private static final String PRE_DESTROY_METHOD = "predestroy";
	
	private static class Killer implements Runnable{
		StatelessProxyFactory proxy;
		Logger log;
		
		public Killer(StatelessProxyFactory proxy,Logger logger){
			this.proxy = proxy;
			this.log = logger;
		}

		@Override
		public void run() {
			try{
				if( (this.proxy.getLastTimeCalled() < (System.currentTimeMillis() - (KILLING_DELAY_SECONDS * 1000l)) ) ){
						if(this.proxy.getState().equals(STATELESS_READY_STATE)){
								this.log.info("Stateless getting killed, because unaccessed since "+ ((System.currentTimeMillis() - this.proxy.getLastTimeCalled())/1000l)+" seconds" );
								this.proxy.setState(STATELESS_KILLED_STATE);
						}
				}

			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	
	private final ScheduledExecutorService killScheduler = 	Executors.newScheduledThreadPool(1);
	private Map<String, Method> methods;
	private Set<String> methodsNames;
	private static long lastTimeCalled;
	private Killer killer;
	Object ejbImplementation;
	Persistence persistence;
	private String state;

	private StatelessProxyFactory(Object obj,Persistence persistenceUnit){
		this.ejbImplementation = obj;
		
		//Setting logs
		logger = Logger.getLogger(this.ejbImplementation.getClass().getName());
		LoggingConfiguratorFactory lcf = new LoggingConfiguratorFactory(logger,level);
		lcf.applySimpleLoggingConfig();
		lcf.applyXMLLoggingConfig();
		
		//Stateless object ready at creation
		state = STATELESS_READY_STATE;
		
		//Setting lastUsedTime
		lastTimeCalled = System.currentTimeMillis();
		
		killer = new Killer(this,this.logger);
		
		methods = new HashMap<>();
		methodsNames = new HashSet<>();
		
		//Gettin PostContruct and PreDestroy Methods
		try{
				logger.info("Stateless getting PostContruct and PreDestroy Methods from class: "+ejbImplementation.getClass().getName());
				for(Method method :obj.getClass().getDeclaredMethods()){
					method.setAccessible(true);
					for(Annotation annotation : method.getAnnotations() ){
							if(annotation instanceof PrePassivate){
								AnnotationException exception = new AnnotationException("Unexpected annotation : PrePassivate on stateless EJB method");
								logger.severe("Severe Error("+exception.getMessage()+")");
								throw exception;
							}else if(annotation instanceof PostActivate){
								AnnotationException exception = new AnnotationException("Unexpected annotation : PostActivate on stateless EJB method");
								logger.severe("Severe Error("+exception.getMessage()+")");
								throw exception;	
							}else if(annotation instanceof PostConstruct){
								logger.fine("Found PostConstruct method in "+ejbImplementation.getClass().getName()+" method name: "+method.getName());
								methods.put(POST_CONSTRUCT_METHOD, method);		
								methodsNames.add(method.getName());			
							}else if(annotation instanceof PreDestroy){
								logger.fine("Found PreDestroy method in "+ejbImplementation.getClass().getName()+" method name: "+method.getName());
								methods.put(PRE_DESTROY_METHOD, method);	
								methodsNames.add(method.getName());				
							}else if(annotation instanceof Remove){
								AnnotationException exception = new AnnotationException("Unexpected annotation : Remove on stateless EJB method");
								logger.severe("Severe Error("+exception.getMessage()+")");
								throw exception;			
							}
						}
				}
				logger.info("Stateless getting PostContruct and PreDestroy Methods from class: found"+ methodsNames.size());
				
				//Getting the EntityManagerField
				logger.info("Injection of EntityManager instance in Stateless("+ejbImplementation.toString()+")");
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
					logger.info("Invoking Stateless PostConstruct method on EJB("+ejbImplementation.toString()+")");
					this.invoke(this.getEjbImplementation(), methods.get(POST_CONSTRUCT_METHOD), null);
				}
			}catch(AnnotationException exception){
				exception.printStackTrace();
			}catch (Throwable e) {
				e.printStackTrace();
			}
		
		
		
		//Clocking state changes
		logger.warning("Scheduling Stateless kill on  EJB("+ejbImplementation.toString()+") @Rate: every "+KILLING_DELAY_SECONDS+" seconds");
		killScheduler.scheduleAtFixedRate(this.killer,KILLING_DELAY_SECONDS,KILLING_DELAY_SECONDS,TimeUnit.SECONDS);
	}
	
	public static Object proxyInstance(Field ejbField,Class<?> implementingClass,Persistence persistenceUnit) throws IllegalArgumentException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		return Proxy.newProxyInstance(ejbField.getType().getClassLoader(), new Class[]{ejbField.getType()}, new StatelessProxyFactory(Class.forName(implementingClass.getName()).newInstance(),persistenceUnit));
	}

	private String getState() {
		return state;
	}

	private long getLastTimeCalled() {
		return lastTimeCalled;
	}

	private void setState(String stateToSet) throws Throwable {
		if(stateToSet.equals(STATELESS_KILLED_STATE)){
		    logger.info("Changing Stateless state to Killed on EJB("+ejbImplementation.toString()+")");	
			this.state = STATELESS_KILLED_STATE;
			if(methods.get(PRE_DESTROY_METHOD)!=null){
				logger.info("Invoking Stateless PreDestroy method on EJB("+ejbImplementation.toString()+")");
				methods.get(PRE_DESTROY_METHOD).invoke(this.ejbImplementation, (Object[])null);
			}
			this.ejbImplementation = null;
			this.killScheduler.shutdown();
		}
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		boolean isEJBAnnotated = testAnnotatedMethod(method.getName());
		if(!isEJBAnnotated){
			//Update lastCalled time
			logger.finer("Singleton on Called EJB("+ejbImplementation.toString()+")");	
			lastTimeCalled = System.currentTimeMillis();			
		}
		
		//Change state to active if has been passivated
		if(this.getState().equals(STATELESS_KILLED_STATE) ){
			IllegalStateException ise =  new IllegalStateException("LifeCycle Exception: Stateless removed");
			logger.severe("Severe Error("+ise.getMessage()+")");
			throw ise;
		}
		logger.finer("Invoking method: "+method.getName()+" on Stateless EJB("+ejbImplementation.toString()+") using args: "+Arrays.toString(args));
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
