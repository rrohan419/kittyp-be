/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
                        .allowedOrigins("http://localhost:8080", "https://kittyp.netlify.app", "https://kittyp-fe-production.up.railway.app") // Frontend URL
                        .allowedMethods("*") // Allow all HTTP methods
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

	
}
