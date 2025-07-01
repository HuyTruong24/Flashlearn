package com.api.flashlearn.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.flashlearn.entities.User;

public interface UserRepository extends JpaRepository<User, String> {
    
    // Custom query methods can be defined here if needed
    // For example, to find a user by username:
    // Optional<User> findByUsername(String username);
    
}
