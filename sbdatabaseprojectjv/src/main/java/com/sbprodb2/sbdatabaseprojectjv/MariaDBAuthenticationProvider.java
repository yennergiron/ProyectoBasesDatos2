package com.sbprodb2.sbdatabaseprojectjv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class MariaDBAuthenticationProvider implements AuthenticationProvider{
    private final DataSource dataSource;

    public MariaDBAuthenticationProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        String username = auth.getName();
        String password = auth.getCredentials().toString();

        try (Connection conn = DriverManager.getConnection(
                dataSource.getConnection().getMetaData().getURL(),
                username,
                password)) {
            
            // Test connection with a simple query
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");
            if (rs.next()) {
                return new UsernamePasswordAuthenticationToken(
                    username, 
                    password,
                    AuthorityUtils.createAuthorityList("ROLE_USER"));
            }
        } catch (SQLException e) {
            throw new BadCredentialsException("Invalid credentials");
        }
        throw new BadCredentialsException("Authentication failed");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
