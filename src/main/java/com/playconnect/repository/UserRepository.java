package com.playconnect.repository;  
import com.playconnect.entity.User;  
import org.springframework.data.jpa.repository.JpaRepository;  
import org.springframework.stereotype.Repository;  
import java.util.Optional;  

@Repository  
public interface UserRepository extends JpaRepository<User, Long> {  
    Optional<User> findByUsername(String username);  // Finds user by username, returns Optional (may be empty)
    Optional<User> findByEmail(String email);  // Finds user by email, returns Optional (may be empty)

}