package io.github.sunshinewzy.spring.service;

import io.github.sunshinewzy.spring.BeanNameAware;
import io.github.sunshinewzy.spring.InitializingBean;

public interface IUserService extends BeanNameAware, InitializingBean {
	void test();
}
