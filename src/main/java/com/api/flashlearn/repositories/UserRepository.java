package com.api.flashlearn.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.flashlearn.entities.User;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsername(String username);
   
    
}
