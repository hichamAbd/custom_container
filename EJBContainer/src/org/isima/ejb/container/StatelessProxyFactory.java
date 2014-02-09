package org.isima.ejb.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import org.isima.ejb.persistenceUnitSchema.Persistence;

public class StatelessProxyFactory implements InvocationHandler{
	private static final long KILLING_DELAY_SECONDS = 5l;
	private static final String STATELESS_READY_STATE = "ready";
	private static final String STATELESS_KILLED_STATE = "killed";
	private static final String POST_CONSTRUCT_METHOD = "postconstruct";
	private static final String PRE_DESTROY_METHOD = "predestroy";
	
	private static class Killer implements Runnable{
		StatelessProxyFactory proxy;
		
		public Killer(StatelessProxyFactory proxy){
			this.proxy = proxy;
		}

		@Override
		public void run() {
			try{
				if( (this.proxy.getLastTimeCalled() < (System.currentTimeMillis() - (KILLING_DELAY_SECONDS * 1000l)) ) ){
						if(this.proxy.getState().equals(STATELESS_READY_STATE)){
								this.proxy.setState(STATELESS_KILLED_STATE);
						}else{
							System.out.println("Stateless"+proxy.getState());
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
		//Stateless object ready at creation
		state = STATELESS_READY_STATE;
		
		//Setting lastUsedTime
		lastTimeCalled = System.currentTimeMillis();
		
		killer = new Killer(this);
		
		methods = new HashMap<>();
		methodsNames = new HashSet<>();
		
		//Gettin PostContruct PrePassivate PostActivate and PreDestroy Methods
		try{
				for(Method method :obj.getClass().getDeclaredMethods()){
					method.setAccessible(true);
					
						for(Annotation annotation : method.getAnnotations() ){
							if(annotation instanceof PrePassivate){
								throw new AnnotationException("Unexpected annotation : PrePassivate on stateless EJB method");
							}else if(annotation instanceof PostActivate){
								throw new AnnotationException("Unexpected annotation : PostActivate on stateless EJB method");			
							}else if(annotation instanceof PostConstruct){
								methods.put(POST_CONSTRUCT_METHOD, method);		
								methodsNames.add(method.getName());			
							}else if(annotation instanceof PreDestroy){
								methods.put(PRE_DESTROY_METHOD, method);	
								methodsNames.add(method.getName());				
							}else if(annotation instanceof Remove){
								throw new AnnotationException("Unexpected annotation : Remove on stateless EJB method");				
							}
						}
				}
				
				//Getting the EntityManagerField
				this.persistence = persistenceUnit;
				Constructor<?> entityManagerConstructor = Class.forName(ConcreteEntityManager.class.getName()).getConstructor(Persistence.class);
				for(Field ejbField : obj.getClass().getDeclaredFields()){
					ejbField.setAccessible(true);
					for(Annotation annotation : ejbField.getAnnotations()){
						if(annotation instanceof PersistenceContext){
							if( (ejbField.getType().equals(EntityManager.class)) && this.persistence != null){
								ejbField.set(obj, entityManagerConstructor.newInstance(this.persistence));
							}else if(this.persistence == null){
								throw new PersistanceException("Persistence file not found: Couldn't find persistence.exe file in META-INF");
							}else{
								throw new AnnotationException("Unexpected annotation exception: expected "+EntityManager.class.getSimpleName()+" field type but found "+ejbField.getType().getSimpleName());
							}
						}
					}
				}
				
				//Invoking POST_CONSTRUCT_METHOD Method
				if(methods.get(POST_CONSTRUCT_METHOD) != null){
					this.invoke(this.getEjbImplementation(), methods.get(POST_CONSTRUCT_METHOD), null);
				}
			}catch(AnnotationException exception){
				exception.printStackTrace();
			}catch (Throwable e) {
				e.printStackTrace();
			}
		
		
		
		//Clocking state changes
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
				//System.out.println(STATELESS_KILLED_STATE+" instance: "+this.toString());
			this.state = STATELESS_KILLED_STATE;
			if(methods.get(PRE_DESTROY_METHOD)!=null){
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
			lastTimeCalled = System.currentTimeMillis();			
		}
		
		//Change state to active if has been passivated
		if(this.getState().equals(STATELESS_KILLED_STATE) ){
			throw new IllegalStateException("LifeCycle Exception: Stateless removed");
		}
		
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
		           throw new IllegalStateException("Invoking method on EJB error: "+String.valueOf(method));
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
