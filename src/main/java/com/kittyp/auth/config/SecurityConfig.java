package com.kittyp.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import com.kittyp.auth.util.AuthEntryPointJwt;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

	private final AuthEntryPointJwt authEntryPointJwt;
	private final JwtAccessDeniedHandler accessDeniedHandler;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
		.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception.authenticationEntryPoint(authEntryPointJwt)
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
//						.requestMatchers("/api/v1/article/**","/api/v1/auth/**","/api/v1/webhook/**", "/api/v1/product/**", "/api/v1/public/**", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**")
						.requestMatchers("/api/v1/**")
						.permitAll().anyRequest().authenticated());

		return http.build();
	}

}
