package io.github.sunshinewzy.spring.test;

import io.github.sunshinewzy.spring.SApplicationContext;
import io.github.sunshinewzy.spring.service.IUserService;
import io.github.sunshinewzy.spring.service.OrderService;

public class Test {

	public static void main(String[] args) {
		SApplicationContext sApplicationContext = new SApplicationContext(AppConfig.class);

		OrderService orderService = (OrderService) sApplicationContext.getBean("orderService");
		System.out.println(orderService.getDeliverService());
		
//		IUserService userService = (IUserService) sApplicationContext.getBean("userService");
//		userService.test();
	}
	
}
