package com.vladko.autoshopcore.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladko.autoshopcore.security.BearerTokenAuthenticationFilter;
import com.vladko.autoshopcore.security.AuthServiceClient;
import com.vladko.autoshopcore.security.CoreAccessDeniedHandler;
import com.vladko.autoshopcore.security.CoreAuthenticationEntryPoint;
import com.vladko.autoshopcore.security.SecurityErrorResponseWriter;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityErrorResponseWriter securityErrorResponseWriter(ObjectMapper objectMapper) {
        return new SecurityErrorResponseWriter(objectMapper);
    }

    @Bean
    public CoreAuthenticationEntryPoint coreAuthenticationEntryPoint(SecurityErrorResponseWriter responseWriter) {
        return new CoreAuthenticationEntryPoint(responseWriter);
    }

    @Bean
    public CoreAccessDeniedHandler coreAccessDeniedHandler(SecurityErrorResponseWriter responseWriter) {
        return new CoreAccessDeniedHandler(responseWriter);
    }

    @Bean
    public BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter(
            ObjectProvider<AuthServiceClient> authServiceClientProvider,
            CoreAuthenticationEntryPoint authenticationEntryPoint,
            SecurityErrorResponseWriter responseWriter
    ) {
        return new BearerTokenAuthenticationFilter(authServiceClientProvider, authenticationEntryPoint, responseWriter);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter,
                                                   CoreAuthenticationEntryPoint authenticationEntryPoint,
                                                   CoreAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/procurement/purchase-orders").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/procurement/stock-receipts").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/procurement/supplier-quotes/search").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")

                        .requestMatchers(HttpMethod.GET, "/api/orders/*/parts", "/api/orders/*/parts/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST", "MECHANIC")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/parts", "/api/orders/*/parts/**").hasAnyRole("ADMIN", "MANAGER", "MECHANIC")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/*/parts/**").hasAnyRole("ADMIN", "MANAGER", "MECHANIC")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/*/parts/**").hasAnyRole("ADMIN", "MANAGER", "MECHANIC")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/*/loyalty/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/*/loyalty/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")

                        .requestMatchers(HttpMethod.PUT, "/api/orders/*/assign").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/*/estimate").hasAnyRole("ADMIN", "MANAGER", "MECHANIC")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/*/status").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST", "MECHANIC")
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST", "MECHANIC")
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/*").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")

                        .requestMatchers(HttpMethod.GET, "/api/customers", "/api/customers/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.POST, "/api/customers", "/api/customers/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.PUT, "/api/customers/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/vehicles", "/api/vehicles/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST", "MECHANIC")
                        .requestMatchers(HttpMethod.POST, "/api/vehicles", "/api/vehicles/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.PUT, "/api/vehicles/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.DELETE, "/api/vehicles/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")

                        .requestMatchers(HttpMethod.GET, "/api/parts", "/api/parts/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST", "MECHANIC")
                        .requestMatchers(HttpMethod.POST, "/api/parts", "/api/parts/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/parts", "/api/parts/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/parts/**").hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/loyalty/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")
                        .requestMatchers("/api/**").denyAll()
                        .anyRequest().denyAll()
                );

        http.addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
