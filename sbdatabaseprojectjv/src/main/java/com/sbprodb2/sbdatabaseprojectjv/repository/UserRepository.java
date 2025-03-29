/*package com.sbprodb2.sbdatabaseprojectjv.repository;

import com.sbprodb2.sbdatabaseprojectjv.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends 
    JpaRepository<UserEntity, Long>,    // Standard CRUD operations
    CustomUserRepository {              // Your custom operations
    
    // Derived query method
    Optional<UserEntity> findByUsername(String username);
    
    // Additional standard methods can be added here
    //boolean existsByEmail(String email); //needs to be removed
}*/
