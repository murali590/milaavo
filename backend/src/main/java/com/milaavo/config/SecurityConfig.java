package com.milaavo.config;

import com.milaavo.common.security.AuthUserJwtConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import java.time.Duration;
import java.util.List;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

        private final ApiSecurityErrorHandler apiSecurityErrorHandler;
        private final AuthUserJwtConverter authUserJwtConverter;

        public SecurityConfig(
                        ApiSecurityErrorHandler apiSecurityErrorHandler,
                        AuthUserJwtConverter authUserJwtConverter) {
                this.apiSecurityErrorHandler = apiSecurityErrorHandler;
                this.authUserJwtConverter = authUserJwtConverter;
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http)
                        throws Exception {

                http
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(apiSecurityErrorHandler)
                                                .accessDeniedHandler(apiSecurityErrorHandler))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/api/health",
                                                                "/api/auth/register",
                                                                "/api/auth/login",
                                                                "/api/auth/verify-mobile",
                                                                "/api/auth/verify/resend-otp",
                                                                "/api/auth/verify/start",
                                                                "/api/auth/refresh",
                                                                "/api/auth/logout")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .authenticationEntryPoint(apiSecurityErrorHandler)
                                                .accessDeniedHandler(apiSecurityErrorHandler)
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(authUserJwtConverter)));

                return http.build();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(corsProperties.allowedOrigins());
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
                config.setMaxAge(Duration.ofHours(1));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", config);
                return source;
        }
}