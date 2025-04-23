package com.kittyp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAsync
@PropertySource("classpath:exception.properties")
@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties")
public class KittypApplication {

	public static void main(String[] args) {
		SpringApplication.run(KittypApplication.class, args);
	}

}
