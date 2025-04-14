package com.sbprodb2.sbdatabaseprojectjv;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.sql.DataSource;

@Controller
public class ControllerDBP {

    private static final Logger logger = LoggerFactory.getLogger(ControllerDBP.class);
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

    @GetMapping("/showTableData")
    @ResponseBody
    public TableData showTableData(@RequestParam("database") String database, @RequestParam("table") String table, Principal principal) throws SQLException {
        String username = principal.getName();

        try (Connection conn = dataSource.getConnection()) {
            // Check if user has SUPER privilege
            boolean isAdmin = checkSuperPrivilege(conn, username);

            // Get top 200 records from the selected table
            return getTableData(conn, database, table, username, isAdmin);
        }
    }

    
    @PostMapping("/runQuery")
    @ResponseBody
    public TableData runQuery(@RequestBody QueryRequest queryRequest, Principal principal) {
        String username = principal.getName();
        String query = queryRequest.getQuery();
        logger.info("User {} is executing query: {}", username, query);
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        
        // Check if the query is safe to execute
        if (query.toLowerCase().contains("drop") || query.toLowerCase().contains("delete") || query.toLowerCase().contains("update")) {
            throw new IllegalArgumentException("Unsafe query detected");
        }

        // Validate the query to prevent SQL injection
        if (!query.toLowerCase().startsWith("select")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
        
        try (Connection conn = dataSource.getConnection()) {
            // Execute the custom query
            return executeQuery(conn, query);
        } catch (SQLException e) {
            logger.error("Error executing query for user {}: {}", username, query, e.getMessage());
            throw new RuntimeException("Error executing query", e);
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

    private TableData getTableData(Connection conn, String database, String table, String username, boolean isAdmin) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        String sql = "SELECT * FROM " + database + "." + table + " LIMIT 200";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(rs.getMetaData().getColumnName(i));
            }
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }
        }
        return new TableData(columns, rows);
    }

    
    private TableData executeQuery(Connection conn, String query) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(rs.getMetaData().getColumnName(i));
            }
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }
        }
        return new TableData(columns, rows);
        }
    
    

    public static class TableData {
        private List<String> columns;
        private List<List<Object>> rows;

        public TableData(List<String> columns, List<List<Object>> rows) {
            this.columns = columns;
            this.rows = rows;
        }

        public List<String> getColumns() {
            return columns;
        }

        public List<List<Object>> getRows() {
            return rows;
        }
    }


    public static class QueryRequest {
        private String query;
        
        public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
        }
    }
    

}