package com.sbprodb2.sbdatabaseprojectjv;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

@Controller
public class ControllerDBP {
    
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                              Model model) {
        model.addAttribute("error", error != null ? "Invalid username or password" : null);
        return "Login";
    }

    private final DataSource dataSource;
    
    public ControllerDBP(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @GetMapping("/dashboard")
    public String showDashboard(Principal principal, Model model) throws SQLException {
        String username = principal.getName();
        model.addAttribute("username", username);
        
        try (Connection conn = dataSource.getConnection()) {
            // Check if user has SUPER privilege
            boolean isAdmin = checkSuperPrivilege(conn, username);
            model.addAttribute("isAdmin", isAdmin);
            
            // Get list of databases the user can access
            List<String> databases = getAccessibleDatabases(conn, username);
            model.addAttribute("databases", databases);
        }
        
        return "dashboard";
    }
    
    private boolean checkSuperPrivilege(Connection conn, String username) throws SQLException {
        String sql = "SELECT Super_priv FROM mysql.user WHERE User = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String superPriv = rs.getString("Super_priv");
                return "Y".equalsIgnoreCase(superPriv);
            }
        }
        return false;
    }
    
    private List<String> getAccessibleDatabases(Connection conn, String username) throws SQLException {
        List<String> databases = new ArrayList<>();
        String sql = "SHOW DATABASES";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                databases.add(rs.getString(1));
            }
        }
        return databases;
    }
}