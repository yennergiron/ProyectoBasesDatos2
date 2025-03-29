package com.sbprodb2.sbdatabaseprojectjv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sbprodb2.sbdatabaseprojectjv.model.UserEntity;
import com.sbprodb2.sbdatabaseprojectjv.repository.UserRepository;

@Controller
public class ControllerDBP {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

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
    }
}