package io.github.sunshinewzy.spring.service;

import io.github.sunshinewzy.spring.BeanPostProcessor;
import io.github.sunshinewzy.spring.Component;

import java.lang.reflect.Proxy;

@Component
public class SBeanPostProcessor implements BeanPostProcessor {
	@Override
	public Object postProcessBeforeInitialization(String beanName, Object bean) {
		if(beanName.equals("userService")) {
			UserService userService = (UserService) bean;
			userService.test();
		}
		
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(String beanName, Object bean) {
		if(beanName.equals("userService")) {
			// AOP
			Object proxyInstance = Proxy.newProxyInstance(SBeanPostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), (proxy, method, args) -> {
				System.out.println("切面逻辑");
				return method.invoke(bean, args);
			});
			
			return proxyInstance;
		}
		
		return bean;
	}
}
