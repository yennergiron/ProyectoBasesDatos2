package com.sbprodb2.sbdatabaseprojectjv;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sbprodb2.sbdatabaseprojectjv.model.UserEntity;
import com.sbprodb2.sbdatabaseprojectjv.repository.UserRepository;

@SpringBootApplication
public class SbdatabaseprojectjvApplication {

	public static void main(String[] args) {
		SpringApplication.run(SbdatabaseprojectjvApplication.class, args);
	}

	@Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                UserEntity admin = new UserEntity();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("admin123"));
                userRepository.save(admin);
            }
        };
	}
}
