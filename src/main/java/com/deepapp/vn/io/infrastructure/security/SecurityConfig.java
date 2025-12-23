package com.deepapp.vn.io.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints - no auth required
                .requestMatchers("/", "/login**", "/error**", "/actuator/**").permitAll()
                .requestMatchers("/api/**").permitAll()  // All API endpoints public for testing
                .requestMatchers("/AA/**").permitAll()   // OAuth test endpoints also public
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/AA/A0/AAA0_0100", true)
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable()); // Disable CSRF for demo
        
        return http.build();
    }
}
