package com.sbprodb2.sbdatabaseprojectjv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;

public class MariaDBAuthenticationProvider implements AuthenticationProvider{
    
    private final DataSource dataSource;

    public MariaDBAuthenticationProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        String username = auth.getName();
        String password = auth.getCredentials().toString();

        // Get base URL without credentials
        String jdbcUrl;
        try {
            jdbcUrl = dataSource.getConnection().getMetaData().getURL();
        } catch (SQLException e) {
            throw new BadCredentialsException("Database configuration error");
        }

        // Try to connect with user-supplied credentials
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            // Verify connection with a simple query
            try (var stmt = conn.createStatement()) {
                stmt.executeQuery("SELECT 1");
            }
            
            return new UsernamePasswordAuthenticationToken(
                username,
                password,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                
        } catch (SQLException e) {
            // Provide detailed error message for debugging
            throw new BadCredentialsException(
                "MariaDB authentication failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
