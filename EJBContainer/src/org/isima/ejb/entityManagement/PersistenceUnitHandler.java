package org.isima.ejb.entityManagement;

import org.isima.ejb.persistenceUnitSchema.Persistence;
import org.isima.ejb.persistenceUnitSchema.Persistence.PersistenceUnit;
import org.isima.ejb.persistenceUnitSchema.Persistence.PersistenceUnit.Properties;
import org.isima.ejb.persistenceUnitSchema.Persistence.PersistenceUnit.Properties.Property;
import org.isima.ejb.persistenceUnitSchema.PersistenceUnitCachingType;
import org.isima.ejb.persistenceUnitSchema.PersistenceUnitTransactionType;
import org.isima.ejb.persistenceUnitSchema.PersistenceUnitValidationModeType;

public class PersistenceUnitHandler {
	private Persistence persistence;

	public PersistenceUnitHandler(Persistence persistence) {
		this.persistence = persistence;
	}
	
	public void parse(){
		System.out.println("!!! NULL POINTERS ALERT !!!");
		String version = this.persistence.getVersion();
		System.out.println("version "+version);
		System.out.println("!!! NULL POINTERS ALERT !!!");
		for(PersistenceUnit persistenceUnit : this.persistence.getPersistenceUnit()){
			boolean excludeUnlistedClasses = persistenceUnit.isExcludeUnlistedClasses();
				System.out.println("excludeUnlistedClasses "+excludeUnlistedClasses);
				System.out.println("!!! NULL POINTERS ALERT !!!");
			String description = persistenceUnit.getDescription();
				System.out.println("description "+description);
				System.out.println("!!! NULL POINTERS ALERT !!!");
			String dataSource = persistenceUnit.getJtaDataSource();
			System.out.println("!!! NULL POINTERS ALERT !!!");
				System.out.println("dataSource "+dataSource);
				System.out.println("!!! NULL POINTERS ALERT !!!");
			String Name = persistenceUnit.getName();
				System.out.println("Name "+Name);
				System.out.println("!!! NULL POINTERS ALERT !!!");
			String nonJtaDataSource = persistenceUnit.getNonJtaDataSource();
				System.out.println("nonJtaDataSource "+nonJtaDataSource);
				System.out.println("!!! NULL POINTERS ALERT !!!");
			String provider = persistenceUnit.getProvider();
				System.out.println("provider "+provider);
				System.out.println("!!! NULL POINTERS ALERT !!!");
			
			for(String jarFile : persistenceUnit.getJarFile()){
				System.out.println("jarFile "+jarFile);
			}

			System.out.println("!!! NULL POINTERS ALERT !!!");
			for(String mappingFile : persistenceUnit.getMappingFile()){
				System.out.println("mappingFile "+mappingFile);
			}
			
			Properties properties = persistenceUnit.getProperties();
			System.out.println("!!! NULL POINTERS ALERT !!!");
			for(Property property : properties.getProperty()){
				String propertyName = property.getName();
				System.out.println("propertyName "+propertyName);
				String propertyValue = property.getValue();
				System.out.println("propertyValue "+propertyValue);
			}

			System.out.println("!!! NULL POINTERS ALERT !!!");
			PersistenceUnitCachingType persistenceUnitCachingType = persistenceUnit.getSharedCacheMode();
			String persistenceUnitCachingTypeValue =  persistenceUnitCachingType.value();
			System.out.println("persistenceUnitCachingTypeValue "+persistenceUnitCachingTypeValue);

			System.out.println("!!! NULL POINTERS ALERT !!!");
			PersistenceUnitTransactionType persistenceUnitTransactionType = persistenceUnit.getTransactionType();
			String persistenceUnitTransactionTypeValue =  persistenceUnitTransactionType.value();
			System.out.println("persistenceUnitTransactionTypeValue "+persistenceUnitTransactionTypeValue);

			System.out.println("!!! NULL POINTERS ALERT !!!");
			PersistenceUnitValidationModeType persistenceUnitValidationModeType = persistenceUnit.getValidationMode();
			String persistenceUnitValidationModeTypeValue = persistenceUnitValidationModeType.value();
			System.out.println("persistenceUnitValidationModeTypeValue "+persistenceUnitValidationModeTypeValue);
		}
	}

}
