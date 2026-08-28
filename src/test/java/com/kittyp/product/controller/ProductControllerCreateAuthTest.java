package com.kittyp.product.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.GlobalExceptionHandler;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.product.dto.ProductFilterDto;
import com.kittyp.product.dto.ProductSaveDto;
import com.kittyp.product.model.ProductModel;
import com.kittyp.product.service.ProductService;

import java.util.List;

@SpringJUnitWebConfig(ProductControllerCreateAuthTest.TestConfig.class)
class ProductControllerCreateAuthTest {

	private static final String PRODUCT_BODY = """
			{
			  "name": "Test litter",
			  "description": "Clumping litter",
			  "price": 199.00,
			  "currency": "INR",
			  "status": "ACTIVE",
			  "productImageUrls": ["https://example.com/p.png"],
			  "stockQuantity": 10,
			  "sku": "SKU-1",
			  "category": "litter"
			}
			""";

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private RecordingProductService productService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		productService.reset();
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
	}

	@ParameterizedTest
	@ValueSource(strings = { "ROLE_DOCTOR", "ROLE_CLINIC_ADMIN", "ROLE_CLINIC_STAFF" })
	void addProduct_nonAdmin_forbidden(String authority) throws Exception {
		mockMvc.perform(post("/api/v1/product")
				.with(user("tester").authorities(new SimpleGrantedAuthority(authority)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(PRODUCT_BODY))
				.andExpect(status().isForbidden());

		assertEquals(0, productService.saveCalls);
	}

	@Test
	void addProduct_admin_ok() throws Exception {
		mockMvc.perform(post("/api/v1/product")
				.with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(PRODUCT_BODY))
				.andExpect(status().isOk());

		assertEquals(1, productService.saveCalls);
	}

	@Configuration
	@EnableWebMvc
	@EnableWebSecurity
	@EnableMethodSecurity(prePostEnabled = true)
	static class TestConfig implements WebMvcConfigurer {

		@Bean
		RecordingProductService productService() {
			return new RecordingProductService();
		}

		@Bean
		ApiResponse<?> apiResponse() {
			return new ApiResponse<>();
		}

		@Bean
		ProductController productController(ApiResponse<?> apiResponse, RecordingProductService productService) {
			return new ProductController(apiResponse, productService);
		}

		@Bean
		GlobalExceptionHandler globalExceptionHandler(ApiResponse<?> apiResponse) {
			return new GlobalExceptionHandler(apiResponse);
		}

		@Bean
		SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
			return http.csrf(AbstractHttpConfigurer::disable)
					.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
					.build();
		}

		@Bean
		LocalValidatorFactoryBean validator() {
			return new LocalValidatorFactoryBean();
		}

		@Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
			converters.add(new MappingJackson2HttpMessageConverter(mapper));
		}
	}

	static final class RecordingProductService implements ProductService {
		int saveCalls;

		void reset() {
			saveCalls = 0;
		}

		@Override
		public ProductModel saveProduct(ProductSaveDto productSaveDto) {
			saveCalls++;
			ProductModel saved = new ProductModel();
			saved.setUuid("product-uuid");
			saved.setName(productSaveDto.getName());
			return saved;
		}

		@Override
		public ProductModel productByUuid(String uuid) {
			throw new UnsupportedOperationException();
		}

		@Override
		public PaginationModel<ProductModel> productsByFilter(ProductFilterDto productFilterDto, Integer pageNumber,
				Integer pageSize) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Integer productCount(Boolean isActive) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void updateProductStock(String productUuid, int quantityToReduce) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void validateProductStock(String productUuid, int requestedQuantity) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void reserveStock(String productUuid, int quantity, String orderNumber) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void confirmStockReservation(String orderNumber) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void cancelStockReservation(String orderNumber) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ProductModel updateProduct(String productUuid, ProductSaveDto productSaveDto) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void deleteProduct(String productUuid) {
			throw new UnsupportedOperationException();
		}
	}
}
