package com.kittyp.common.health;

import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
public class HealthDashboardManagementConfig {

	@Bean
	FilterRegistrationBean<HealthHtmlRedirectFilter> healthHtmlRedirectFilter() {
		FilterRegistrationBean<HealthHtmlRedirectFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new HealthHtmlRedirectFilter());
		registration.addUrlPatterns("/actuator/health");
		registration.setOrder(0);
		return registration;
	}
}
