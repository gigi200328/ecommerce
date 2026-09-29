package com.ojt.ecommerce.backofficeinventory.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetailsService;
import com.ojt.ecommerce.backofficeinventory.security.JwtAuthenticationFilter;
import com.ojt.ecommerce.backofficeinventory.security.JwtUtil;
import com.ojt.ecommerce.storefront.security.G5JwtAuthenticationFilter;
import com.ojt.ecommerce.storefront.security.G5JwtUtil;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {


	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	@Order(1)
	public SecurityFilterChain securityFilterChain(HttpSecurity http,JwtUtil jwtUtil,
	        CustomUserDetailsService userDetailsService) throws Exception {
		   JwtAuthenticationFilter backofficeFilter =
		            new JwtAuthenticationFilter(
		                    jwtUtil,
		                    userDetailsService
		            );
		http.securityMatcher("/api/v1/**")
		.cors(Customizer.withDefaults()).csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/v1/auth/**", "/error", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
				.permitAll().anyRequest().authenticated())
				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(backofficeFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	} 
	
    @Bean
    @Order(2)
    public SecurityFilterChain g5SecurityFilterChain(HttpSecurity http,G5JwtUtil g5JwtUtil) throws Exception {
    	G5JwtAuthenticationFilter storefrontFilter = new G5JwtAuthenticationFilter(g5JwtUtil);
        http
        .securityMatcher("/api/storefront/v1/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/storefront/v1/auth/customer/login", "/api/storefront/v1/auth/customer/register").permitAll()
                .requestMatchers("/api/storefront/v1/products/**", "/api/storefront/v1/categories/**", "/api/storefront/v1/cart/guest/**").permitAll()
                .requestMatchers("/api/storefront/v1/delivery-zones/**", "/api/storefront/v1/shipping/quote/**", "/api/storefront/v1/payment-callbacks/g3").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(storefrontFilter , UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

	
	@Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}