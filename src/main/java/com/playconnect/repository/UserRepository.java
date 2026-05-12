package repository;  
// This file is in the repository (database operations)

import Entity.User;  

import org.springframework.data.jpa.repository.JpaRepository;  // Gives built-in database methods (no SQL needed)
import org.springframework.stereotype.Repository;  // Marks this as a Repository component

import java.util.Optional;  // For handling null safely (avoid errors)

@Repository 
public interface UserRepository extends JpaRepository<User, Long> {  // <Entity type, ID type>
    
    // Find user by their username (username is unique)
    Optional<User> findByUsername(String username);  
    
    // Find user by their email (email is unique)
    Optional<User> findByEmail(String email);  
    }
