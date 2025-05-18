/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.exception;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientException;

/**
 * @author rrohan419@gmail.com 
 */
@FunctionalInterface
public interface ErrorHandler {
	void handle(ClientHttpResponse response) throws RestClientException;
}
