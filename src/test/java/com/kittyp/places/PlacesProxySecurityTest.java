package com.kittyp.places;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.GlobalExceptionHandler;
import com.kittyp.places.controller.PlacesController;
import com.kittyp.places.service.PlacesProxyService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class PlacesProxySecurityTest {

	private static final String KEY = "AIza-TEST-KEY-MUST-NOT-LEAK";

	private MockMvc mockMvc;
	private MockMvc unauthMvc;

	@BeforeEach
	void setUp() {
		PlacesProxyService service = new PlacesProxyService(RestClient.builder().build(), new ObjectMapper(), "", "");
		ApiResponse<?> responseBuilder = new ApiResponse<>();
		PlacesController controller = new PlacesController(service, responseBuilder);
		GlobalExceptionHandler advice = new GlobalExceptionHandler(responseBuilder);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(advice)
				.setMessageConverters(new MappingJackson2HttpMessageConverter())
				.build();
		unauthMvc = MockMvcBuilders.standaloneSetup(controller)
				.addFilters((Filter) (ServletRequest request, ServletResponse response, FilterChain chain) -> {
					HttpServletRequest req = (HttpServletRequest) request;
					if (req.getRequestURI() != null && req.getRequestURI().contains("/places/")
							&& !req.getRequestURI().contains("/public/")) {
						((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						return;
					}
					chain.doFilter(request, response);
				})
				.setControllerAdvice(advice)
				.setMessageConverters(new MappingJackson2HttpMessageConverter())
				.build();
	}

	@Test
	void unauthenticatedPlaces_is401() throws Exception {
		unauthMvc.perform(post("/api/v1/places/autocomplete")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"query\":\"pune clinic\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void crlfQuery_is400() throws Exception {
		mockMvc.perform(post("/api/v1/places/autocomplete")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"query\":\"ab\\r\\ninject\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void blankApiKeyAutocomplete_is503AndDoesNotContainApiKey() throws Exception {
		String body = mockMvc.perform(post("/api/v1/places/autocomplete")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"query\":\"pune clinic\"}"))
				.andExpect(status().isServiceUnavailable())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertTrue(!body.contains(KEY), body);
	}

	@Test
	void detailsCrLf_rejectedBeforeGoogle() {
		PlacesProxyService service = new PlacesProxyService(mock(RestClient.class), new ObjectMapper(), KEY, "");
		assertThrows(IllegalArgumentException.class, () -> service.details("place\r\nid", null));
	}
}
