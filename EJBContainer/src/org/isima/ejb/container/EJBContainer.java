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
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.isima.ejb.logging.LoggingConfiguratorFactory;
import org.isima.ejb.persistenceUnitSchema.PersistenceObjectFactory;
import org.isima.ejb.persistenceUnitSchema.Persistence;
import org.reflections.Reflections;

public class EJBContainer {
	private final Logger logger;
	private static final Level level = Level.ALL;
	
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
		//Setting logs
		logger = Logger.getLogger(EJBContainer.class.getName());
		LoggingConfiguratorFactory lcf = new LoggingConfiguratorFactory(logger,level);
		lcf.applySimpleLoggingConfig();
		lcf.applyXMLLoggingConfig();
		
		try {
			logger.info("BootStrap init launch start on container ("+this.toString()+")");
			bootstrapInit();
			logger.info("BootStrap init end ("+this.toString()+")");
		} catch (PersistanceException e) {
			e.printStackTrace();
		}
	}

	private void bootstrapInit() throws PersistanceException {
		ejbSingletons = new HashMap<>();
		// Scan all classes of the classloader
		
		//Getting persistence.xml file
		logger.info("Loading persistence.xml file from class loader ...");
		URL url = getClass().getClassLoader().getResource("META-INF/persistence.xml");
		if(url != null){
			JAXBContext jaxbContext;
			try {
				jaxbContext = JAXBContext.newInstance(PersistenceObjectFactory.class);

				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				persistanceUnit = (Persistence) unmarshaller.unmarshal(url);
			} catch (JAXBException e) {
				PersistanceException exception = new PersistanceException("PersistenceUnit Exception: "+"Reading persistence file error: "+e.getMessage());
				logger.severe("Severe Error ("+exception.getMessage()+")");
				throw exception;
			}
		}else{
			logger.warning("Loading persistence.xml file failed : File not found");
		}
		// find EJB interfaces map EJB to implementation		
		// manage errors

	}

	private String getEJBFieldType(Field field,Class<?> implementation){
		int singletonAnnotation = 0;
		int startupAnnotation = 0;
		int statefulAnnotation = 0;
		int statelessAnnotation = 0;
		
		logger.fine("Getting EJB type from field: "+field.getName());
		List<Annotation> fieldAnnotationList;
		
		//Getting annotation of the EJB's implementation class
		Annotation[] annotations = implementation.getDeclaredAnnotations();
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
			logger.warning("Error : Unknown EJB type from field: "+field.getName()+"[Type: "+field.getType()+"]");
			return EJB_TYPE_UNKNOWN;
		}else if( (statelessAnnotation + statefulAnnotation + singletonAnnotation + startupAnnotation ==2) && (singletonAnnotation == 1 && startupAnnotation == 1) ){
			logger.info("Found Startup Singleton EJB in field: "+field.getName());
			return EJB_TYPE_SINGLETON_STARTUP;
		}else if( (statelessAnnotation + statefulAnnotation + singletonAnnotation + startupAnnotation ==1) ){
			if(statelessAnnotation==1){ 
				logger.info("Found Stateless EJB in field: "+field.getName());
				return EJB_TYPE_STATELESS;				
			}
			else if(statefulAnnotation == 1){ 
				logger.info("Found Stateful EJB in field: "+field.getName());
				return EJB_TYPE_STATEFUL;
			}
			else if(singletonAnnotation == 1){ 
				logger.info("Found Singleton EJB in field: "+field.getName());
				return EJB_TYPE_SINGLETON;
			}else if(startupAnnotation == 1){
				logger.warning("Error : Unknown EJB type from field: "+field.getName()+"[Type: "+field.getType()+"]");
				return EJB_TYPE_UNKNOWN;
			}else{
				logger.warning("Error : Unknown EJB type from field: "+field.getName()+"[Type: "+field.getType()+"]");
				return EJB_TYPE_UNKNOWN;
			}
			
		}else{
			logger.warning("Error : Unknown EJB type from field: "+field.getName()+"[Type: "+field.getType()+"]");
			return EJB_TYPE_UNKNOWN;
		}
		
	}

	private Object getEJBProxy(Field ejbField,Class<?> concreteEjbClass,String type) throws IllegalArgumentException, InstantiationException, IllegalAccessException, ClassNotFoundException, AnnotationException{
		if(type.equals(EJB_TYPE_SINGLETON)){
			logger.info("Getting Singleton proxy instance of "+ejbField.getType()+" for class: "+concreteEjbClass.getName()+" in field: "+ejbField.getName());
			return SingletonProxyFactory.proxyInstance(ejbField, concreteEjbClass, persistanceUnit, false);
		}else if(type.equals(EJB_TYPE_SINGLETON_STARTUP)){
			logger.info("Getting Startup Singleton proxy instance of "+ejbField.getType()+" for class: "+concreteEjbClass.getName()+" in field: "+ejbField.getName());
			return SingletonProxyFactory.proxyInstance(ejbField, concreteEjbClass, persistanceUnit,true);
		}else if(type.equals(EJB_TYPE_STATEFUL)){
			logger.info("Getting Stateful proxy instance of "+ejbField.getType()+" for class: "+concreteEjbClass.getName()+" in field: "+ejbField.getName());
			return StatefulProxyFactory.proxyInstance(ejbField, concreteEjbClass, persistanceUnit);			
		}else if(type.equals(EJB_TYPE_STATELESS)){
			logger.info("Getting Stateless proxy instance of "+ejbField.getType()+" for class: "+concreteEjbClass.getName()+" in field: "+ejbField.getName());
			return StatelessProxyFactory.proxyInstance(ejbField, concreteEjbClass, persistanceUnit);
		}else{
			throw new AnnotationException("UnexpectedAnnotationException: Unexpected EJB Type");
		}
	}
	
	public void inject(Object o) throws Exception {
		logger.info("Starting injection for object: "+o.toString());
		// Fields collection
		List<Field> fields = new LinkedList<>();

		// Scan all @EJB and inject EJB implementations
		logger.fine("Parsing fields of object: "+o.toString());
		for (Field field : o.getClass().getDeclaredFields()) {
			if (field.getAnnotation(EJB.class) != null) {
				logger.finer("Found EJB annotated field in object: "+o.toString()+" field name: "+field.getName());
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
		if(fields.size() > 0){logger.info("Found "+fields.size()+" EJB annotated fields in object: "+o.toString());}
		for (Field field : fields) {
			// Getting the EJB interface
			Class<?> fieldType = field.getType();
			
			// Getting the interface implementation
			logger.fine("Getting Local interface ("+field.getType()+") implementation for field: "+field.getName());
			Reflections reflections = new Reflections(o.getClass().getPackage());
			Set<?> subTypes = reflections.getSubTypesOf(fieldType);
			Class<?> result;
			if(subTypes.iterator().hasNext()){
				result = (Class<?>) subTypes.iterator().next();
				logger.info("Found implementation of "+field.getType()+" in type:"+result.getName());
			}else{
				AnnotationException exception = new AnnotationException("UnexpectedAnnotationException: Annotated EJB type isn't a local interface or has no implementation");
				logger.severe("Sever Error("+exception.getMessage()+")");
				throw exception;
			}
			
			// Getting annotation of the EJB's implementation class
			String EJBtype = getEJBFieldType(field,result);
			if(EJBtype.equals(EJB_TYPE_SINGLETON) || EJBtype.equals(EJB_TYPE_SINGLETON_STARTUP)){
				if(ejbSingletons.get(fieldType) == null){
					logger.info("First instanciation of Singleton type : "+field.getType());
					Object FieldValue = getEJBProxy(field,result,EJBtype);
					field.set(o, FieldValue);
					ejbSingletons.put(field.getType(), FieldValue);
				}else{
					logger.info("Reaching already existing Singleton EJB. Retrieving Singleton : "+ejbSingletons.get(fieldType));
					field.set(o,ejbSingletons.get(fieldType));
				}
			}else if(EJBtype.equals(EJB_TYPE_STATEFUL)){
				Object FieldValue = getEJBProxy(field,result,EJB_TYPE_STATEFUL);
				field.set(o, FieldValue);
			}else if(EJBtype.equals(EJB_TYPE_STATELESS)){
				Object FieldValue = getEJBProxy(field,result,EJB_TYPE_STATELESS);
				field.set(o, FieldValue);				
			}else{
				AnnotationException exception = new AnnotationException("UnexpectedAnnotationException: At the annotated EJB implementation of "+field.getName());
				logger.severe("Sever Error("+exception.getMessage()+")");
				throw exception;
			}
		}
		
		// TODO: manage @TransactionAttribute and strategies => Proxy ? (required , requiredAttribute, un autre)		

	}
}
