package com.milaavo.config;

import com.milaavo.common.security.AuthUserJwtConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

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
                                "/api/auth/verify/resend-otp")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(apiSecurityErrorHandler)
                        .accessDeniedHandler(apiSecurityErrorHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authUserJwtConverter)));

        return http.build();
    }
}