package org.isima.ejb.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.isima.ejb.annotations.EJB;
import org.isima.ejb.annotations.Singleton;
import org.isima.ejb.annotations.Startup;
import org.isima.ejb.annotations.Stateful;
import org.isima.ejb.annotations.Stateless;
import org.isima.ejb.exception.AnnotationException;
import org.isima.ejb.exception.PersistanceException;
import org.isima.ejb.persistenceUnitSchema.ObjectFactory;
import org.isima.ejb.persistenceUnitSchema.Persistence;
import org.reflections.Reflections;

public class EJBContainer {

	private static EJBContainer containerInstance = new EJBContainer();
	private Map<Class<?>,Object> ejbSingletons;
	private Persistence persistanceUnit;
	
	private static final String EJB_TYPE_SINGLETON = "singleton";
	private static final String EJB_TYPE_SINGLETON_STARTUP = "singletonstartup";
	private static final String EJB_TYPE_STATEFUL = "stateful";
	private static final String EJB_TYPE_STATELESS = "stateless";
	private static final String EJB_TYPE_UNKNOWN= "unknown";
	

	public static EJBContainer getInstance() {
		return containerInstance;
	}

	private EJBContainer() {
		try {
			bootstrapInit();
		} catch (PersistanceException e) {
			e.printStackTrace();
		}
	}

	private void bootstrapInit() throws PersistanceException {
		ejbSingletons = new HashMap<>();
		// Scan all classes of the classloader
		
		//Getting persistence.xml file
		URL url = getClass().getClassLoader().getResource("META-INF/persistence.xml");
		if(url != null){
			JAXBContext jaxbContext;
			try {
				jaxbContext = JAXBContext.newInstance(ObjectFactory.class);

				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				persistanceUnit = (Persistence) unmarshaller.unmarshal(url);
			} catch (JAXBException e) {
				throw new PersistanceException("PersistenceUnit Exception: "+"Reading persistence file error: "+e.getMessage());
			}
		}
		// find EJB interfaces map EJB to implementation		
		// manage errors

	}

	private String getEJBFieldType(Field field){
		int singletonAnnotation = 0;
		int startupAnnotation = 0;
		int statefulAnnotation = 0;
		int statelessAnnotation = 0;
		
		List<Annotation> fieldAnnotationList;
		//Getting the EJB interface
		Class<?> fieldType = field.getType();
		//Getting the interface implementation
		Reflections reflections = new Reflections(field.getClass().getPackage());
		Set<?> subTypes = reflections.getSubTypesOf(fieldType);
		Class<?> result = (Class<?>) subTypes.iterator().next();
		
		//Getting annotation of the EJB's implementation class
		Annotation[] annotations = result.getDeclaredAnnotations();
		fieldAnnotationList = (List<Annotation>) Arrays.asList(annotations);;
		for(Annotation annotation : fieldAnnotationList){
			if(annotation instanceof Stateless){
				statelessAnnotation++;
			}else if(annotation instanceof  Stateful){
				statefulAnnotation++;
			}else if(annotation instanceof Singleton){
				singletonAnnotation++;
			}else if(annotation instanceof Startup){
				startupAnnotation++;
			}
			
		}
		
		if( (statelessAnnotation + statefulAnnotation + singletonAnnotation + startupAnnotation >2) ){
			return EJB_TYPE_UNKNOWN;
		}else if( (statelessAnnotation + statefulAnnotation + singletonAnnotation + startupAnnotation ==2) && (singletonAnnotation == 1 && startupAnnotation == 1) ){
			return EJB_TYPE_SINGLETON_STARTUP;
		}else if( (statelessAnnotation + statefulAnnotation + singletonAnnotation + startupAnnotation ==1) ){
			if(statelessAnnotation==1){ 
				return EJB_TYPE_STATELESS;				
			}
			else if(statefulAnnotation == 1){ 
				return EJB_TYPE_STATEFUL;
			}
			else if(singletonAnnotation == 1){ 
				return EJB_TYPE_SINGLETON;
			}else if(startupAnnotation == 1){ //Error 
				return EJB_TYPE_UNKNOWN;
			}else{
				return EJB_TYPE_UNKNOWN;
			}
			
		}else{
			//Error
			return EJB_TYPE_UNKNOWN;
		}
		
	}

	private Object getEJBProxy(Field ejbField,Class<?> concreteEjbClass,String type) throws IllegalArgumentException, InstantiationException, IllegalAccessException, ClassNotFoundException, AnnotationException{
		if(type.equals(EJB_TYPE_SINGLETON)){
			return SingletonProxyFactory.proxyInstance(ejbField, concreteEjbClass, persistanceUnit, false);
		}else if(type.equals(EJB_TYPE_SINGLETON_STARTUP)){
			return SingletonProxyFactory.proxyInstance(ejbField, concreteEjbClass, persistanceUnit,true);
		}else if(type.equals(EJB_TYPE_STATEFUL)){
			return StatefulProxyFactory.proxyInstance(ejbField, concreteEjbClass, persistanceUnit);			
		}else if(type.equals(EJB_TYPE_STATELESS)){
			return StatelessProxyFactory.proxyInstance(ejbField, concreteEjbClass, persistanceUnit);
		}else{
			throw new AnnotationException("UnexpectedAnnotationException: Unexpected EJB Type");
		}
	}
	
	public void inject(Object o) throws Exception {
		// Fields collection
		List<Field> fields = new LinkedList<>();

		// Scan all @EJB and inject EJB implementations
		for (Field field : o.getClass().getDeclaredFields()) {
			if (field.getAnnotation(EJB.class) != null) {
				// The field is annotated @EJB		
				fields.add(field);
				
				// Setting accessible to private ejb fields
				field.setAccessible(true);
			}
		}

		
		// Manage @Stateless & @Stateful & @Singleton strategies => do not manage EJB pool
		// manage @PosteConstruct
		// manage PreDestroy
		// manage @PersistenceContext injection
		for (Field field : fields) {
			//Object FieldValue = field.get(o);
			// Getting the EJB interface
			Class<?> fieldType = field.getType();
			
			// Getting the interface implementation
			Reflections reflections = new Reflections(o.getClass().getPackage());
			Set<?> subTypes = reflections.getSubTypesOf(fieldType);
			Class<?> result;
			if(subTypes.iterator().hasNext()){
				result = (Class<?>) subTypes.iterator().next();
			}else{
				throw new AnnotationException("UnexpectedAnnotationException: Annotated ejb type isn't a local interface");
			}
			
			// Getting annotation of the EJB's implementation class
			String EJBtype = getEJBFieldType(field);
			if(EJBtype.equals(EJB_TYPE_SINGLETON) || EJBtype.equals(EJB_TYPE_SINGLETON_STARTUP)){
				if(ejbSingletons.get(fieldType) == null){
					Object FieldValue = getEJBProxy(field,result,EJBtype);
					field.set(o, FieldValue);
					ejbSingletons.put(field.getType(), FieldValue);
				}else{
					field.set(o,ejbSingletons.get(fieldType));
				}
			}else if(EJBtype.equals(EJB_TYPE_STATEFUL)){
				Object FieldValue = getEJBProxy(field,result,EJB_TYPE_STATEFUL);
				field.set(o, FieldValue);
			}else if(EJBtype.equals(EJB_TYPE_STATELESS)){
				Object FieldValue = getEJBProxy(field,result,EJB_TYPE_STATELESS);
				field.set(o, FieldValue);				
			}else{
				throw new AnnotationException("UnexpectedAnnotationException: At the annotated EJB implementation of "+field.toString());
			}
		}
		
		// manage @TransactionAttribute and strategies => Proxy ? (required , requiredAttribute, un autre)

	}

}
