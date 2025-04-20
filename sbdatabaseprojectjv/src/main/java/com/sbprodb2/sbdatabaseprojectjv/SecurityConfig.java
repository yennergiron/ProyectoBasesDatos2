package com.sbprodb2.sbdatabaseprojectjv;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DataSource dataSource;

    public SecurityConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/login", "/css/**").permitAll()
            .anyRequest().authenticated()
        )
        .csrf(csrf -> csrf.ignoringRequestMatchers(
            "/runQuery", "/createUser", "/alterUser", "/dropUser","/listUsers")) // Disable CSRF for these endpoints
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/dashboard", true)
            .failureUrl("/login?error=true")
            .permitAll()
        )
        .logout(logout -> logout
            .logoutSuccessUrl("/login?logout")
            .permitAll()
        );
    return http.build();
}

    @Bean
    public AuthenticationProvider authenticationProvider() {
    return new AuthenticationProvider() {
        @Override
        public Authentication authenticate(Authentication auth) throws AuthenticationException {
            String username = auth.getName();
            String password = auth.getCredentials().toString();
            
            try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3307/proyecto_bd",
                username,
                password)) {
                
                return new UsernamePasswordAuthenticationToken(
                    username, password, Collections.emptyList());
                    
            } catch (SQLException e) {
                throw new BadCredentialsException("Invalid credentials");
            }
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return true;
        }
    };
}
}
