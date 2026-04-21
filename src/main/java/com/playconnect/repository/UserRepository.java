package com.playconnect.repository;  
import com.playconnect.entity.User;  // Import User entity
import org.springframework.data.jpa.repository.JpaRepository;  // Gives CRUD methods
import org.springframework.stereotype.Repository;  // Marks as Spring Bean
import java.util.Optional;  // For handling null safely

@Repository  // Marks this as a Repository component
public interface UserRepository extends JpaRepository<User, Long> {  // Extends JPA repo (User is entity, Long is ID type)
    
    Optional<User> findByUsername(String username);  // Finds user by username, returns Optional (may be empty)
    
    Optional<User> findByEmail(String email);  // Finds user by email, returns Optional (may be empty)
    }
