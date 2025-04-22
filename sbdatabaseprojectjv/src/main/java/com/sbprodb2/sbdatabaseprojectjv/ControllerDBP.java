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
import javax.swing.table.TableColumn;

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
public Object runQuery(@RequestBody QueryRequest queryRequest, Principal principal) {
    String username = principal.getName();
    String query = queryRequest.getQuery();
    String selectedSchema = queryRequest.getSelectedSchema();
    
    logger.info("User {} is executing query on schema {}: {}", username, selectedSchema, query);
    
    if (query == null || query.trim().isEmpty()) {
        return new ErrorResponse("Query cannot be null or empty");
    }
    
    try (Connection conn = dataSource.getConnection()) {
        // Use the selected schema if provided
        if (selectedSchema != null && !selectedSchema.isEmpty()) {
            try (Statement useStmt = conn.createStatement()) {
                useStmt.execute("USE " + selectedSchema);
            }
        }
        
        if (isSelectQuery(query)) {
            // For SELECT queries, return table data
            return executeQuery(conn, query);
        } else {
            // For non-SELECT queries, execute and return affected rows
            return executeUpdate(conn, query);
        }
    } catch (SQLException e) {
        logger.error("Error executing query for user {}: {}", username, e.getMessage());
        return new ErrorResponse("SQL Error: " + e.getMessage());
    }
}

private boolean isSelectQuery(String query) {
    String trimmedQuery = query.trim().toLowerCase();
    return trimmedQuery.startsWith("select") || trimmedQuery.startsWith("show") || 
           trimmedQuery.startsWith("describe") || trimmedQuery.startsWith("explain");
}

private UpdateResponse executeUpdate(Connection conn, String query) throws SQLException {
    int rowsAffected = 0;
    boolean hasResults = false;
    
    try (Statement stmt = conn.createStatement()) {
        String[] queries = query.split(";");
        
        for (String singleQuery : queries) {
            if (singleQuery.trim().isEmpty()) continue;
            
            boolean isResult = stmt.execute(singleQuery.trim());
            if (isResult) {
                hasResults = true;
            } else {
                rowsAffected += stmt.getUpdateCount();
            }
        }
    }
    
    return new UpdateResponse("Query executed successfully", rowsAffected, hasResults);
}

    @PostMapping("/createUser")
@ResponseBody
public ResponseMessage createUser(@RequestBody UserRequest userRequest, Principal principal) throws SQLException {
    String adminUsername = principal.getName();
    
    try (Connection conn = dataSource.getConnection()) {
        // Verify the current user has admin privileges
        if (!checkSuperPrivilege(conn, adminUsername)) {
            return new ResponseMessage("Error: Insufficient privileges");
        }
        
        // Create the user
        String username = userRequest.getUsername();
        String password = userRequest.getPassword();
        String host = userRequest.getHost() != null ? userRequest.getHost() : "%";
        
        String sql = "CREATE USER '" + username + "'@'" + host + "' IDENTIFIED BY '" + password + "'";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            return new ResponseMessage("User created successfully");
        }
    }
}

@PostMapping("/alterUser")
@ResponseBody
public ResponseMessage alterUser(@RequestBody UserRequest userRequest, Principal principal) throws SQLException {
    String adminUsername = principal.getName();
    
    try (Connection conn = dataSource.getConnection()) {
        // Verify the current user has admin privileges
        if (!checkSuperPrivilege(conn, adminUsername)) {
            return new ResponseMessage("Error: Insufficient privileges");
        }
        
        // Alter the user
        String username = userRequest.getUsername();
        String password = userRequest.getPassword();
        String host = userRequest.getHost() != null ? userRequest.getHost() : "%";
        
        String sql = "ALTER USER '" + username + "'@'" + host + "' IDENTIFIED BY '" + password + "'";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            return new ResponseMessage("User updated successfully");
        }
    }
}

@PostMapping("/dropUser")
@ResponseBody
public ResponseMessage dropUser(@RequestBody UserRequest userRequest, Principal principal) throws SQLException {
    String adminUsername = principal.getName();
    
    try (Connection conn = dataSource.getConnection()) {
        // Verify the current user has admin privileges
        if (!checkSuperPrivilege(conn, adminUsername)) {
            return new ResponseMessage("Error: Insufficient privileges");
        }
        
        // Drop the user
        String username = userRequest.getUsername();
        String host = userRequest.getHost() != null ? userRequest.getHost() : "%";
        
        String sql = "DROP USER '" + username + "'@'" + host + "'";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            return new ResponseMessage("User dropped successfully");
        }
    }
}

@GetMapping("/listUsers")
@ResponseBody
public List<UserInfo> listUsers(Principal principal) throws SQLException {
    String adminUsername = principal.getName();
    List<UserInfo> users = new ArrayList<>();
    
    try (Connection conn = dataSource.getConnection()) {
        // Verify the current user has admin privileges
        if (!checkSuperPrivilege(conn, adminUsername)) {
            return users; // Return empty list if not admin
        }
        
        // Get user list
        String sql = "SELECT User, Host FROM mysql.user ORDER BY User";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new UserInfo(rs.getString("User"), rs.getString("Host")));
            }
        }
    }
    return users;
}

@PostMapping("/grantRoles")
@ResponseBody
public ResponseMessage grantRoles(@RequestBody GrantRequest grantRequest, Principal principal) throws SQLException {
    String adminUsername = principal.getName();
    
    try (Connection conn = dataSource.getConnection()) {
        // Verify the current user has admin privileges
        if (!checkSuperPrivilege(conn, adminUsername)) {
            return new ResponseMessage("Error: Insufficient privileges");
        }
        
        // Grant the privileges
        String username = grantRequest.getUsername();
        String host = grantRequest.getHost();
        String database = grantRequest.getDatabase();
        List<String> privileges = grantRequest.getPrivileges();
        Boolean isSuper = grantRequest.getIsSuper();
        Boolean revoke = grantRequest.getRevoke();
        
        String sql;
        if (Boolean.TRUE.equals(revoke)) {
            // Handle revocation
            if ("*".equals(database)) {
                sql = "REVOKE ALL PRIVILEGES ON *.* FROM '" + username + "'@'" + host + "'";
            } else {
                sql = "REVOKE ALL PRIVILEGES ON " + database + ".* FROM '" + username + "'@'" + host + "'";
            }
        } else if (Boolean.TRUE.equals(isSuper)) {
            // Handle super privileges
            sql = "GRANT ALL PRIVILEGES ON *.* TO '" + username + "'@'" + host + "' WITH GRANT OPTION";
        } else {
            // Handle normal privileges
            String privilegesStr = privileges.isEmpty() ? "ALL PRIVILEGES" : String.join(", ", privileges);
            sql = "GRANT " + privilegesStr + " ON " + database + ".* TO '" + username + "'@'" + host + "'";
        }
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            
            // Execute FLUSH PRIVILEGES to apply changes
            stmt.execute("FLUSH PRIVILEGES");
            
            if (Boolean.TRUE.equals(revoke)) {
                return new ResponseMessage("Privileges revoked successfully");
            } else if (Boolean.TRUE.equals(isSuper)) {
                return new ResponseMessage("Super privileges granted successfully");
            } else {
                return new ResponseMessage("Privileges granted successfully");
            }
        }
    }
}

@GetMapping("/database-management")
public String showDatabaseManagement(Principal principal, Model model) throws SQLException {
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

    return "database-management";
}

// New methods to handle database operations
@PostMapping("/createSchema")
@ResponseBody
public ResponseMessage createSchema(@RequestBody SchemaRequest schemaRequest, Principal principal) throws SQLException {
    String username = principal.getName();
    
    try (Connection conn = dataSource.getConnection()) {
        // Verify the current user has admin privileges
        if (!checkSuperPrivilege(conn, username)) {
            return new ResponseMessage("Error: Insufficient privileges");
        }
        
        // Create the schema
        String schemaName = schemaRequest.getSchemaName();
        String sql = "CREATE DATABASE " + schemaName;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            return new ResponseMessage("Schema created successfully");
        }
    }
}

@PostMapping("/createTable")
@ResponseBody
public ResponseMessage createTable(@RequestBody TableRequest tableRequest, Principal principal) throws SQLException {
    String username = principal.getName();
    
    try (Connection conn = dataSource.getConnection()) {
        // Check privileges
        boolean isAdmin = checkSuperPrivilege(conn, username);
        if (!isAdmin) {
            // For non-admins, check if they have CREATE privilege on the database
            // Logic to check specific privileges could be added here
        }
        
        // Create the table
        String database = tableRequest.getDatabase();
        String tableName = tableRequest.getTableName();
        List<TableColumn> columns = tableRequest.getColumns();
        
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("CREATE TABLE ").append(database).append(".").append(tableName).append(" (");
        
        for (int i = 0; i < columns.size(); i++) {
            TableColumn column = columns.get(i);
            sqlBuilder.append(column.getName()).append(" ").append(column.getType());
            
            if (column.getLength() != null && !column.getLength().isEmpty()) {
                sqlBuilder.append("(").append(column.getLength()).append(")");
            }
            
            if (column.isPrimaryKey()) {
                sqlBuilder.append(" PRIMARY KEY");
            }
            
            if (column.isNotNull()) {
                sqlBuilder.append(" NOT NULL");
            }
            
            if (column.isAutoIncrement()) {
                sqlBuilder.append(" AUTO_INCREMENT");
            }
            
            if (i < columns.size() - 1) {
                sqlBuilder.append(", ");
            }
        }
        
        sqlBuilder.append(")");
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sqlBuilder.toString());
            return new ResponseMessage("Table created successfully");
        }
    }
}

@PostMapping("/dropTable")
@ResponseBody
public ResponseMessage dropTable(@RequestBody TableRequest tableRequest, Principal principal) throws SQLException {
    String username = principal.getName();
    
    try (Connection conn = dataSource.getConnection()) {
        // Check privileges
        boolean isAdmin = checkSuperPrivilege(conn, username);
        if (!isAdmin) {
            // For non-admins, check if they have DROP privilege on the database
            // Logic to check specific privileges could be added here
        }
        
        // Drop the table
        String database = tableRequest.getDatabase();
        String tableName = tableRequest.getTableName();
        
        String sql = "DROP TABLE " + database + "." + tableName;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            return new ResponseMessage("Table dropped successfully");
        }
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
    
    // Helper method to extract data from a ResultSet
private TableData extractResultSetData(ResultSet rs) throws SQLException {
    List<String> columns = new ArrayList<>();
    List<List<Object>> rows = new ArrayList<>();
    
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
        private String selectedSchema;
        
        public String getQuery() {
            return query;
        }
        
        public void setQuery(String query) {
            this.query = query;
        }
        
        public String getSelectedSchema() {
            return selectedSchema;
        }
        
        public void setSelectedSchema(String selectedSchema) {
            this.selectedSchema = selectedSchema;
        }
    }
    
    public static class ErrorResponse {
        private String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
        
        public String getError() {
            return error;
        }
    }
    
    public static class UpdateResponse {
        private String message;
        private int rowsAffected;
        private boolean hasResults;
        
        public UpdateResponse(String message, int rowsAffected, boolean hasResults) {
            this.message = message;
            this.rowsAffected = rowsAffected;
            this.hasResults = hasResults;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getRowsAffected() {
            return rowsAffected;
        }
        
        public boolean isHasResults() {
            return hasResults;
        }
    }
    
    // Additional classes for request/response handling
public static class UserRequest {
    private String username;
    private String password;
    private String host;
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
}

public static class UserInfo {
    private String username;
    private String host;
    
    public UserInfo(String username, String host) {
        this.username = username;
        this.host = host;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getHost() {
        return host;
    }
}

public static class ResponseMessage {
    private String message;
    
    public ResponseMessage(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
}


// Add this class for grant request
public static class GrantRequest {
    private String username;
    private String host;
    private String database;
    private List<String> privileges;
    private Boolean isSuper;
    private Boolean revoke;
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database;
    }
    
    public List<String> getPrivileges() {
        return privileges;
    }
    
    public void setPrivileges(List<String> privileges) {
        this.privileges = privileges;
    }
    
    public Boolean getIsSuper() {
        return isSuper;
    }
    
    public void setIsSuper(Boolean isSuper) {
        this.isSuper = isSuper;
    }
    
    public Boolean getRevoke() {
        return revoke;
    }
    
    public void setRevoke(Boolean revoke) {
        this.revoke = revoke;
    }
}

// Additional classes for request/response handling
public static class SchemaRequest {
    private String schemaName;
    
    public String getSchemaName() {
        return schemaName;
    }
    
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
}

public static class TableRequest {
    private String database;
    private String tableName;
    private List<TableColumn> columns;
    
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public List<TableColumn> getColumns() {
        return columns;
    }
    
    public void setColumns(List<TableColumn> columns) {
        this.columns = columns;
    }
}

public static class TableColumn {
    private String name;
    private String type;
    private String length;
    private boolean primaryKey;
    private boolean notNull;
    private boolean autoIncrement;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getLength() {
        return length;
    }
    
    public void setLength(String length) {
        this.length = length;
    }
    
    public boolean isPrimaryKey() {
        return primaryKey;
    }
    
    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }
    
    public boolean isNotNull() {
        return notNull;
    }
    
    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }
    
    public boolean isAutoIncrement() {
        return autoIncrement;
    }
    
    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }
}

}