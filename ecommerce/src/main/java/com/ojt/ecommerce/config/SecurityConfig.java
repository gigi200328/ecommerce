package com.ojt.ecommerce.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
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
	@Order(0)
	public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**",
				"/webjars/**", "/uploads/**").cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

		return http.build();
	}

	// Main Branch မှ အသစ်ပါလာသော Public Locations API
	@Bean
	@Order(1)
	public SecurityFilterChain publicLocationsSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/api/locations/**").cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		return http.build();
	}

	// 1. Backoffice APIs Chain (Role-Based လုံခြုံရေးများ ပေါင်းထည့်ထားပါသည်)
	@Bean
	@Order(2)
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUtil jwtUtil,
			CustomUserDetailsService userDetailsService) throws Exception {
		JwtAuthenticationFilter backofficeFilter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
		
        http.securityMatcher("/api/backoffice/**", "/api/v1/reports/**", "/api/v1/admin/**", "/api/admin/**").cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/backoffice/auth/**").permitAll()
						
                        // Staff များကို ADMIN သာ စီမံနိုင်သည်
                        .requestMatchers("/api/backoffice/staff", "/api/backoffice/staff/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                        
                        // Inventory ကြည့်ရှုခြင်း (GET) ကို SALES_STAFF ပါ စစ်ဆေးခွင့်ပြုသည်
                        .requestMatchers(
                            HttpMethod.GET,
                            "/api/backoffice/inventory", "/api/backoffice/inventory/**"
                        ).hasAnyAuthority("ADMIN", "ROLE_ADMIN", "INVENTORY_STAFF", "ROLE_INVENTORY_STAFF", "SALES_STAFF", "ROLE_SALES_STAFF")

                        // Products များကို ADMIN နှင့် INVENTORY_STAFF ကိုင်တွယ်နိုင်သည်
                        .requestMatchers(
                            "/api/backoffice/categories", "/api/backoffice/categories/**",
                            "/api/backoffice/brands", "/api/backoffice/brands/**",
                            "/api/backoffice/products", "/api/backoffice/products/**",
                            "/api/backoffice/product-images/**",
                            "/api/backoffice/variations", "/api/backoffice/variations/**",
                            "/api/backoffice/variation-options/**",
                            "/api/backoffice/product-variants/**",
                            "/api/backoffice/variant-option-values/**",
                            "/api/backoffice/tags", "/api/backoffice/tags/**",
                            "/api/backoffice/product-tags/**",
                            "/api/backoffice/product-variant-images/**",
                            "/api/backoffice/inventory", "/api/backoffice/inventory/**"
                        ).hasAnyAuthority("ADMIN", "ROLE_ADMIN", "INVENTORY_STAFF", "ROLE_INVENTORY_STAFF")
                        
                        // Orders များကို ADMIN နှင့် SALES_STAFF ကိုင်တွယ်နိုင်သည်
                        .requestMatchers("/api/backoffice/orders", "/api/backoffice/orders/**", "/api/backoffice/shipping", "/api/backoffice/shipping/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "SALES_STAFF", "ROLE_SALES_STAFF", "ORDER_STAFF", "ROLE_ORDER_STAFF")

                        // Refunds များကို ADMIN နှင့် SALES_STAFF ကိုင်တွယ်နိုင်သည်
                        .requestMatchers(
                            "/api/v1/admin/notifications", "/api/v1/admin/notifications/**",
                            "/api/v1/admin/refunds", "/api/v1/admin/refunds/**",
                            "/api/backoffice/refunds", "/api/backoffice/refunds/**"
                        ).hasAnyAuthority("ADMIN", "ROLE_ADMIN", "SALES_STAFF", "ROLE_SALES_STAFF", "ORDER_STAFF", "ROLE_ORDER_STAFF")
                        
                        // Reports များကို ADMIN နှင့် INVENTORY_STAFF (စတော့အစီရင်ခံစာများအတွက်) ကြည့်ခွင့်ပေးမည်
                        .requestMatchers("/api/backoffice/reports", "/api/backoffice/reports/**", "/api/v1/reports/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "INVENTORY_STAFF", "ROLE_INVENTORY_STAFF", "SALES_STAFF", "ROLE_SALES_STAFF")

						// Products များကို ADMIN နှင့် INVENTORY_STAFF ကိုင်တွယ်နိုင်သည်
						.requestMatchers("/api/backoffice/categories", "/api/backoffice/categories/**",
								"/api/backoffice/brands", "/api/backoffice/brands/**", "/api/backoffice/products",
								"/api/backoffice/products/**", "/api/backoffice/product-images/**",
								"/api/backoffice/variations", "/api/backoffice/variations/**",
								"/api/backoffice/variation-options/**", "/api/backoffice/product-variants/**",
								"/api/backoffice/variant-option-values/**", "/api/backoffice/tags",
								"/api/backoffice/tags/**", "/api/backoffice/product-tags/**",
								"/api/backoffice/product-variant-images/**", "/api/backoffice/inventory",
								"/api/backoffice/inventory/**")
						.hasAnyAuthority("ADMIN", "ROLE_ADMIN", "INVENTORY_STAFF", "ROLE_INVENTORY_STAFF")

						// Orders များကို ADMIN နှင့် SALES_STAFF ကိုင်တွယ်နိုင်သည်
						.requestMatchers("/api/backoffice/orders", "/api/backoffice/orders/**")
						.hasAnyAuthority("ADMIN", "ROLE_ADMIN", "SALES_STAFF", "ROLE_SALES_STAFF")

						// Main မှ အသစ်ပါလာသော Reports များကို ADMIN သာ ကြည့်ခွင့်ပေးမည်
						.requestMatchers("/api/backoffice/reports", "/api/backoffice/reports/**")
						.hasAnyAuthority("ADMIN", "ROLE_ADMIN")

						.anyRequest().authenticated())

				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(backofficeFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	@Order(3)
	public SecurityFilterChain g5SecurityFilterChain(HttpSecurity http, G5JwtUtil g5JwtUtil) throws Exception {
		G5JwtAuthenticationFilter storefrontFilter = new G5JwtAuthenticationFilter(g5JwtUtil);
		http.securityMatcher("/api/storefront/v1/**").cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/storefront/v1/auth/customer/login",
								"/api/storefront/v1/auth/customer/register",
								"/api/storefront/v1/auth/customer/forgot-password",
								"/api/storefront/v1/auth/customer/reset-password",
								"/api/storefront/v1/auth/customer/verify-code")
						.permitAll()
						.requestMatchers("/api/storefront/v1/products/**", "/api/storefront/v1/categories/**",
								"/api/storefront/v1/cart/guest/**")
						.permitAll().requestMatchers("/api/storefront/v1/tags/**", "/api/storefront/v1/brands/**")
						.permitAll().requestMatchers(HttpMethod.GET, "/api/storefront/v1/search-history/popular")
						.permitAll()
//                .requestMatchers("/api/storefront/v1/delivery-zones/**", "/api/storefront/v1/shipping/quote/**", "/api/storefront/v1/payment-callbacks/g3").permitAll()
						.requestMatchers("/api/storefront/v1/payment-callbacks/g3").permitAll()

						.anyRequest().authenticated())
				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(storefrontFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
		configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:3000"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		configuration.setAllowedHeaders(
				List.of("Authorization", "Content-Type", "Cache-Control", "Accept", "X-Requested-With"));
		configuration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}