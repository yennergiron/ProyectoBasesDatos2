package com.sbprodb2.sbdatabaseprojectjv;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.ui.Model;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class ControllerDBP {
    /* 
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/signup")
    public String showSignupForm() {
        return "signup"; // Create this template
    }
    
    @PostMapping("/signup")
    public String processSignup(@RequestParam String username, 
                              @RequestParam String password) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        
        return "redirect:/login";
    }

    @GetMapping("/Login")
    public String showLoginPage() {
        return "Login"; // This should match your template name (without .html)
    }*/

    
    @Autowired
    private DataSource dataSource;

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                              Model model) {
        model.addAttribute("error", error);
        return "Login";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Principal principal, Model model) {
        model.addAttribute("username", principal.getName());
        return "dashboard";
    }
    /* 
    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                           @RequestParam String password,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {

    try (Connection conn = dataSource.getConnection()) {
        // Use a prepared statement to prevent SQL injection
        String query = "SELECT password FROM mysql.user WHERE user = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, username);

        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            // Retrieve the hashed password from the database
            String hashedPassword = rs.getString("password");

            // Use PasswordEncoder to verify the provided password
            if (passwordEncoder.matches(password, hashedPassword)) {
                // Store user in session
                request.getSession().setAttribute("dbUser", username);
                return "redirect:/dashboard?username=" + username;
            } else {
                redirectAttributes.addAttribute("error", "Invalid credentials");
                return "redirect:/login";
            }
        } else {
            redirectAttributes.addAttribute("error", "User not found");
            return "redirect:/login";
        }

    } catch (SQLException e) {
        redirectAttributes.addAttribute("error", "Database error: " + e.getMessage());
        return "redirect:/login";
    }
}

    @GetMapping("/dashboard")
    public String showDashboard(@RequestParam String username, Model model) {
        model.addAttribute("username", username);
        return "dashboard"; // You can create a separate dashboard page if preferred
    }*/

    @PostMapping("/execute-query")
    @ResponseBody
    public Map<String, Object> executeQuery(@RequestBody Map<String, String> body,
                                          HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        String username = (String) session.getAttribute("dbUser");
        
        if (username == null) {
            response.put("error", "Not authenticated");
            return response;
        }

        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            boolean isResultSet = stmt.execute(body.get("query"));
            
            if (isResultSet) {
                ResultSet rs = stmt.getResultSet();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                List<Map<String, Object>> results = new ArrayList<>();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    results.add(row);
                }
                
                response.put("results", results);
            } else {
                response.put("affectedRows", stmt.getUpdateCount());
            }
            
        } catch (SQLException e) {
            response.put("error", e.getMessage());
        }
        
        return response;
    }
}