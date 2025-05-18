/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * @author rrohan419@gmail.com 
 */
@Configuration
public class RestClientConfig {

	@Bean
	RestClient restClient() {
		return RestClient
				.builder()
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}
}
