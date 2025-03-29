package com.sbprodb2.sbdatabaseprojectjv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;

public class DatabaseUserDetailsService implements UserDetailsService {
    private final DataSource dataSource;

    public DatabaseUserDetailsService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT user, password FROM mysql.user WHERE user = ?");
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return org.springframework.security.core.userdetails.User
                        .withUsername(rs.getString("username"))
                        .password(rs.getString("password"))
                        .roles("USER") // Replace with dynamic roles if needed
                        .build();
            } else {
                throw new UsernameNotFoundException("User not found: " + username);
            }
        } catch (Exception e) {
            throw new UsernameNotFoundException("Database error occurred", e);
        }
    }
        
    /*@Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // We authenticate directly against MariaDB
        return User.withUsername(username)
                  .password("") // Password will be checked by MariaDB
                  //.roles("USER")
                  .build();
    }*/
}
