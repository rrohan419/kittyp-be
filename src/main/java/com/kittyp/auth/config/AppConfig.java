/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.auth.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * @author rrohan419@gmail.com 
 */
@Configuration
@EnableJpaAuditing
@EnableAsync
public class AppConfig {
	
	@Bean
	ModelMapper modelMapper() {
		ModelMapper modelMapper = new ModelMapper();
		modelMapper.getConfiguration().setAmbiguityIgnored(true);
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

		return modelMapper;
	}
	
	@Bean(name = "taskExecutor")
	public TaskExecutor workExecutor() {
		ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setThreadNamePrefix("Async-Thread");

		threadPoolTaskExecutor.setCorePoolSize(5);
		threadPoolTaskExecutor.setMaxPoolSize(10);
		threadPoolTaskExecutor.setQueueCapacity(600);
		threadPoolTaskExecutor.afterPropertiesSet();

		return threadPoolTaskExecutor;
	}
	
//	@Bean
//	public ObjectMapper objectMapper() {
//	    ObjectMapper mapper = new ObjectMapper();
//	    mapper.registerModule(new JavaTimeModule()); // Handles LocalDateTime
//	    mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Optional: to format LocalDateTime as ISO string
//        return mapper;
//	}
	
	 @Bean
	    public ObjectMapper objectMapper() {
	        ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder().build();
	        objectMapper.registerModule(new JavaTimeModule()); // Register the JavaTimeModule
	        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);  // Optional: to format LocalDateTime as ISO string
	        return objectMapper;
	    }
}
