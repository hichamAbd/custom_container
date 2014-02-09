package org.isima.ejb.entityManagement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.isima.ejb.annotations.Entity;
import org.isima.ejb.annotations.Id;
import org.isima.ejb.exception.AnnotationException;

public class EntityParser {
	private Map<String, Map<String,Object>> attributs;

	public Map<String, Map<String,Object>> parseEntity(Object entity) {
		boolean isEntity = verifyEntityType(entity);
		try{
			attributs = new HashMap<String,Map<String,Object>>();
			if(isEntity){
				getFieldsAndValues(entity);
				return attributs;
			}else{
				throw new AnnotationException("Expecting @Entity pojo but given object is not an Entity");
			}
		}catch(AnnotationException e){
			e.printStackTrace();
		}
		return null;
	}

	private boolean verifyEntityType(Object entity){
		for(Annotation annotation : entity.getClass().getAnnotations()){
			if(annotation instanceof Entity){
				return true;
			}
		}
		return false;
	}
	
	private void getFieldsAndValues(Object entity){
		Map<String,Object> entry;
		boolean done;
		try{
			for(Field field : entity.getClass().getDeclaredFields()){
				field.setAccessible(true);
				done = false;
				for(Annotation annotation : field.getAnnotations()){
					if(annotation instanceof Id){
						entry = new HashMap<>();
						entry.put(field.getName(), field.get(entity));
						attributs.put("Id", entry);
						done = true;
					}
				}
				if(done == false){
					entry = new HashMap<>();
					entry.put(field.getName(), field.get(entity));
					attributs.put("Id", entry);
					done = true;
				}
			}
		}catch(IllegalAccessException e){
			e.printStackTrace();
		}
	}
}
