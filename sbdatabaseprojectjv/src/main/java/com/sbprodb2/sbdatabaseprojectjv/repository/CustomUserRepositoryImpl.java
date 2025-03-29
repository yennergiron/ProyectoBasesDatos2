package com.sbprodb2.sbdatabaseprojectjv.repository;

import com.sbprodb2.sbdatabaseprojectjv.model.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class CustomUserRepositoryImpl implements CustomUserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public UserEntity saveWithValidation(UserEntity user) {
        // Custom validation logic
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        
        if (user.getPassword() == null || user.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        // Persist or merge
        if (user.getId() == null) {
            entityManager.persist(user);
        } else {
            user = entityManager.merge(user);
        }
        
        // Flush to get any database-generated IDs
        entityManager.flush();
        return user;
    }

    @Override
    @Transactional
    public int updatePasswordForUsers(String oldPassword, String newPassword) {
        return entityManager.createQuery(
                "UPDATE UserEntity u SET u.password = :newPassword " +
                "WHERE u.password = :oldPassword")
            .setParameter("newPassword", newPassword)
            .setParameter("oldPassword", oldPassword)
            .executeUpdate();
    }

    @Override
    public UserEntity findUserWithDetails(Long userId) {
        TypedQuery<UserEntity> query = entityManager.createQuery(
            "SELECT u FROM UserEntity u LEFT JOIN FETCH u.roles " +
            "LEFT JOIN FETCH u.permissions WHERE u.id = :userId", UserEntity.class);
        query.setParameter("userId", userId);
        return query.getSingleResult();
    }
}