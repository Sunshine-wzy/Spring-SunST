package io.github.sunshinewzy.spring.service;

import io.github.sunshinewzy.spring.*;

@Component
@Scope("prototype")
public class UserService implements IUserService {
	
	@Autowired
	private OrderService orderService;

	private String beanName;
	private String id;
	
	
	@PostConstruct
	public void post() {
		System.out.println("Posted.");
	}
	
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void afterPropertiesSet() {
		id = "Sunshine_wzy";
	}

	
	@Override
	public void test() {
		System.out.println(orderService);
		System.out.println(beanName);
		System.out.println(id);
	}
	
}
