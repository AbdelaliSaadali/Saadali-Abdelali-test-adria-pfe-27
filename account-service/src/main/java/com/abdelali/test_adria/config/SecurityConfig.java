package com.abdelali.test_adria.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            @Value("${bank.security.enabled:false}") boolean enabled
    ) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);

        if (enabled) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health/**").permitAll()
                    .anyRequest().authenticated()
            );

            http.oauth2ResourceServer(
                    oauth -> oauth.jwt(Customizer.withDefaults())
            );
        } else {
            http.authorizeHttpRequests(
                    auth -> auth.anyRequest().permitAll()
            );
        }

        return http.build();
    }
}