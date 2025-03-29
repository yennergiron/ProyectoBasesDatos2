package com.sbprodb2.sbdatabaseprojectjv.repository;

import com.sbprodb2.sbdatabaseprojectjv.model.UserEntity;;

public interface CustomUserRepository {
    
    // Custom save with additional validation
    UserEntity saveWithValidation(UserEntity user);
    
    // Bulk password update
    int updatePasswordForUsers(String oldPassword, String newPassword);
    
    // Custom find with eager loading
    UserEntity findUserWithDetails(Long userId);
}
