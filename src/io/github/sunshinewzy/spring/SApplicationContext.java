package io.github.sunshinewzy.spring;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SApplicationContext {
	
	private Class<?> configClass;
	private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>();
	private ArrayList<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();
	
	
	public SApplicationContext(Class<?> configClass) {
		this.configClass = configClass;
		
		// 解析配置类
		// ComponentScan注解 -> 扫描路径 -> 扫描
		ComponentScan componentScanAnnotation = configClass.getDeclaredAnnotation(ComponentScan.class);
		String path = componentScanAnnotation.value();
		
		// ClassLoader
		// Bootstrap ----> jre/lib
		// Ext ----------> jre/ext/lib
		// App ----------> classpath
		ClassLoader classLoader = SApplicationContext.class.getClassLoader();
		URL resource = classLoader.getResource(path.replace('.', '/'));
		
		if(resource != null) {
			File file = new File(resource.getFile());
			URL classPathResource = classLoader.getResource("");
			if(classPathResource != null) {
				String classPath = classPathResource.getPath().substring(1).replace('/', '\\');
				scanFiles(file, classLoader, classPath);
			}
		}

		for(Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
			String beanName = entry.getKey();
			BeanDefinition beanDefinition = entry.getValue();
			
			if(beanDefinition.getScope().equals("singleton")) {
				Object bean = createBean(beanName, beanDefinition);
				singletonObjects.put(beanName, bean);
			}
		}
	}

	private void scanFiles(File file, ClassLoader classLoader, String classPath) {
		if(file.isDirectory()) {
			File[] files = file.listFiles();
			if(files != null) {
				for(File f : files) {
					try {
						String fileName = f.getCanonicalPath();
	
						if(fileName.endsWith(".class")) {
							fileName = fileName.substring(fileName.indexOf(classPath) + classPath.length(), fileName.indexOf(".class")).replace('\\', '.');
							Class<?> clazz = classLoader.loadClass(fileName);

							// 表示这个类是一个Bean
							if(clazz.isAnnotationPresent(Component.class)){
								if(BeanPostProcessor.class.isAssignableFrom(clazz)) {
									BeanPostProcessor instance = (BeanPostProcessor) clazz.newInstance();
									beanPostProcessorList.add(instance);
								}
								
								Component component = clazz.getAnnotation(Component.class);
								String beanName = component.value();
								
								if(beanName.equals("")) {
									beanName = Introspector.decapitalize(clazz.getSimpleName());
								}
								
								BeanDefinition beanDefinition = new BeanDefinition();
								beanDefinition.setType(clazz);
								
								if(clazz.isAnnotationPresent(Scope.class)) {
									Scope scopeAnnotation = clazz.getAnnotation(Scope.class);
									beanDefinition.setScope(scopeAnnotation.value());
								} else {
									beanDefinition.setScope("singleton");
								}
								
								beanDefinitionMap.put(beanName, beanDefinition);
							}
						} else scanFiles(f, classLoader, classPath);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public Object getBean(String beanName) {
		BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
		
		if(beanDefinition == null) {
			throw new RuntimeException("Bean '" + beanName + "' did not be found.");
		} else {
			String scope = beanDefinition.getScope();
			
			if(scope.equals("singleton")) {
				// 单例
				Object bean = singletonObjects.get(beanName);
				if(bean == null) {
					bean = createBean(beanName, beanDefinition);
					if(bean != null) singletonObjects.put(beanName, bean);
				}
				return bean;
			} else {
				// 多例
				return createBean(beanName, beanDefinition);
			}
		}
	}
	
	private Object createObject(Constructor<?> constructor) throws InvocationTargetException, InstantiationException, IllegalAccessException {
		if(constructor.getParameterCount() == 0)
			return constructor.newInstance();
		
		ArrayList<Object> parameterObjects = new ArrayList<>();
		
		for(Parameter parameter : constructor.getParameters()){
			Object bean = getBeanByTypeAndName(parameter.getType(), parameter.getName());
			if(bean != null) parameterObjects.add(bean);
			else throw new RuntimeException("Cannot find bean by type '" + parameter.getType() + "'.");
		}

		return constructor.newInstance(parameterObjects.toArray());
	}

	private Object getBeanByTypeAndName(Class<?> type, String name) {
		ArrayList<String> beanNames = new ArrayList<>();

		beanDefinitionMap.forEach((key, value) -> {
			if(type.equals(value.getType())){
				beanNames.add(key);
			}
		});

		int size = beanNames.size();
		if(size == 0) {
			throw new RuntimeException("Cannot find bean by type '" + type + "'.");
		} else if(size == 1) {
			String beanName = beanNames.get(0);
			return getBean(beanName);
		} else {
			for(String beanName : beanNames){
				if(name.equals(beanName)) {
					return getBean(name);
				}
			}

			throw new RuntimeException("Ambiguous bean names of type '" + type + "'.");
		}
	}

	private Object createBean(String beanName, BeanDefinition beanDefinition) {
		Class<?> clazz = beanDefinition.getType();
		try {
			Object instance;
			
			// 推断构造方法
			Constructor<?>[] constructors = clazz.getConstructors();
			if(constructors.length == 1) {
				instance = createObject(constructors[0]);
			} else {
				int cnt = 0;
				Constructor<?> constructor = null;
				Constructor<?> emptyConstructor = null;
 				for(Constructor<?> con : constructors) {
				    if(con.getParameterCount() == 0)
					    emptyConstructor = con;
 					
					if(con.isAnnotationPresent(Autowired.class)) {
						cnt++;
						
						if(cnt >= 2) break;
						constructor = con;
					}
				}
				
				if(cnt == 1) {
					instance = createObject(constructor);
				} else if(emptyConstructor != null) {
					instance = emptyConstructor.newInstance();
				} else if(cnt == 0) {
					throw new RuntimeException("Ambiguous constructors.");
				} else throw new RuntimeException("Too many constructors with @Autowired.");
			}

			// 依赖注入
			for(Field f : clazz.getDeclaredFields()) {
				if(f.isAnnotationPresent(Autowired.class)) {
					f.setAccessible(true);
					f.set(instance, getBeanByTypeAndName(f.getType(), f.getName()));
				}
			}
			
			// Aware回调
			if(instance instanceof BeanNameAware) {
				((BeanNameAware) instance).setBeanName(beanName);
			}

			for(Method method : clazz.getDeclaredMethods()) {
				if(method.isAnnotationPresent(PostConstruct.class)) {
					method.invoke(instance);
				}
			}

			// BeanPostProcessor
			for(BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
				instance = beanPostProcessor.postProcessBeforeInitialization(beanName, instance);
			}
			
			// 初始化
			if(instance instanceof InitializingBean) {
				((InitializingBean) instance).afterPropertiesSet();
			}

			// BeanPostProcessor
			for(BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
				instance = beanPostProcessor.postProcessAfterInitialization(beanName, instance);
			}
			
			
			
			return instance;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
}