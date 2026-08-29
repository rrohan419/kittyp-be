package com.kittyp.auth.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.kittyp.auth.util.AuthEntryPointJwt;
import com.kittyp.auth.util.AuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

	private final AuthEntryPointJwt authEntryPointJwt;
	private final JwtAccessDeniedHandler accessDeniedHandler;
	private final AuthenticationFilter authenticationFilter;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistration(
			AuthenticationFilter filter) {
		FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception.authenticationEntryPoint(authEntryPointJwt)
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/**").permitAll()
						.requestMatchers("/api/v1/public/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/product/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/product/all").permitAll()
						// Authenticated doctor/admin author profile — must run before public article GETs
						.requestMatchers(HttpMethod.GET, "/api/v1/article/author/me").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/article/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/article/all").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/blogs/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/blogs/all").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/clinic/invites/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/clinic/staff-invite/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/clinic/staff-invite/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/upload/signup-documents").permitAll()
						.requestMatchers("/api/v1/webhook/**").permitAll()
						.requestMatchers("/health", "/actuator/health", "/actuator/dashboard").permitAll()
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
