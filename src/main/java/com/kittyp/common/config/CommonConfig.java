/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @author rrohan419@gmail.com
 */
@Configuration
public class CommonConfig {

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**") // Allow all endpoints
						.allowedOrigins("http://localhost:8080", "https://kittyp.netlify.app", "https://kittyp.in", "https://www.kittyp.in") // Frontend URL
						.allowedMethods("*") // Allow all HTTP methods
						.allowedHeaders("*").allowCredentials(true);
			}
		};
	}
}
