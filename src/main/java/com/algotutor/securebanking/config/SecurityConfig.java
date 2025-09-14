package com.algotutor.securebanking.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.algotutor.securebanking.security.JwtAuthenticationEntryPoint;
import com.algotutor.securebanking.security.JwtAuthenticationFilter;
import com.algotutor.securebanking.service.impl.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	@Autowired
	private UserDetailsServiceImpl userDetailsService;

	@Autowired
	private JwtAuthenticationEntryPoint unauthorizedHandler;

	@Bean
	public JwtAuthenticationFilter authenticationJwtTokenFilter() {
		return new JwtAuthenticationFilter();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// =======================
	// API Security (JWT)
	// =======================
	@Bean
	public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/api/**") // <--- important: restrict this chain only to /api/**
				.cors(AbstractHttpConfigurer::disable).csrf(AbstractHttpConfigurer::disable)
				.exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/**", "/api/public/**").permitAll()
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
								"/swagger-resources/**", "/webjars/**")
						.permitAll().requestMatchers("/h2-console/**").permitAll().requestMatchers("/api/admin/**")
						.hasRole("ADMIN").requestMatchers("/api/customer/**").hasRole("CUSTOMER").anyRequest()
						.authenticated());

		http.authenticationProvider(authenticationProvider());
		http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

		// for H2 console in dev
		http.headers(headers -> headers.frameOptions().disable());

		return http.build();
	}

	// =======================
	// Actuator Security (Basic Auth)
	// =======================
	@Bean
	public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/actuator/**") // <--- isolate actuator paths
				.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**")
						.permitAll().requestMatchers("/actuator/info").permitAll()
						.requestMatchers("/actuator/prometheus").permitAll()
						.requestMatchers("/actuator/env", "/actuator/configprops", "/actuator/beans",
								"/actuator/mappings", "/actuator/threaddump", "/actuator/heapdump")
						.hasRole("ADMIN").requestMatchers("/actuator/metrics", "/actuator/metrics/**").authenticated()
						.requestMatchers("/actuator/loggers", "/actuator/loggers/**").hasRole("ADMIN").anyRequest()
						.hasRole("ADMIN"))
				.httpBasic(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.csrf(AbstractHttpConfigurer::disable);

		return http.build();
	}

	@Bean
	public UserDetailsService actuatorUserDetailsService() {
		UserDetails admin = User.builder().username("actuator-admin")
				.password(passwordEncoder().encode("actuator-secret-2024")).roles("ADMIN").build();

		return new InMemoryUserDetailsManager(admin);
	}
}
