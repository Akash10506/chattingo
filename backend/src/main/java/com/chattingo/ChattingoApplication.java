package com.chattingo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Add these new imports for security
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication
public class ChattingoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChattingoApplication.class, args);
    }

    // Add this entire method to the class
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF as it's not needed for stateless APIs
            .csrf(csrf -> csrf.disable())

            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow unauthenticated access to actuator, auth APIs, and WebSockets
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                // Require authentication for all other requests
                .anyRequest().authenticated()
            )

            // Configure session management to be stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }
}
