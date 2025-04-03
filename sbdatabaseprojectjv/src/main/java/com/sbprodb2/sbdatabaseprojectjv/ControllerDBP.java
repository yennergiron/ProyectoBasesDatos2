package com.sbprodb2.sbdatabaseprojectjv;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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

    private final DataSource dataSource;
    
    public ControllerDBP(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                              Model model) {
        model.addAttribute("error", error != null ? "Invalid username or password" : null);
        return "Login";
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
            List<String> databases = getAccessibleDatabases(conn, username, isAdmin);
            model.addAttribute("databases", databases);
        }

        return "dashboard";
    }
    
    @GetMapping("/showTables")
    @ResponseBody
    public List<String> showTables(@RequestParam("database") String database, Principal principal) throws SQLException {
        String username = principal.getName();

        try (Connection conn = dataSource.getConnection()) {
            // Check if user has SUPER privilege
            boolean isAdmin = checkSuperPrivilege(conn, username);

            // Get list of tables in the selected database
            return getTables(conn, database, username, isAdmin);
        }
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

    private List<String> getAccessibleDatabases(Connection conn, String username, boolean isAdmin) throws SQLException {
        List<String> databases = new ArrayList<>();
        String sql = isAdmin ? "SHOW DATABASES" : "SHOW GRANTS FOR ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (!isAdmin) {
                stmt.setString(1, username);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (isAdmin) {
                    databases.add(rs.getString(1));
                } else {
                    String grant = rs.getString(1);
                    if (grant.contains("proyecto_bd")) {
                        databases.add("proyecto_bd");
                    }
                }
            }
        }
        return databases;
    }

    private List<String> getTables(Connection conn, String database, String username, boolean isAdmin) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SHOW TABLES FROM " + database;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }

}