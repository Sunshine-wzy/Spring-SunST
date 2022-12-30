package io.github.sunshinewzy.spring.service;

import io.github.sunshinewzy.spring.Autowired;
import io.github.sunshinewzy.spring.Component;
import io.github.sunshinewzy.spring.Scope;

@Component
@Scope("prototype")
public class OrderService {
	
	@Autowired
	private DeliverService deliverService;
	
	
	public OrderService() {
		
	}
	
	public OrderService(DeliverService deliverService) {
		this.deliverService = deliverService;
	}
	
	public OrderService(DeliverService deliverService1, DeliverService deliverService2) {
		this.deliverService = deliverService1;
	}


	public DeliverService getDeliverService() {
		return deliverService;
	}
}
