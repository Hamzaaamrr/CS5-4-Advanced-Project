package Service;

import Entity.*;
import repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service  // this class is a Service (business logic)
public class UserService {
    
    @Autowired  // Spring automatically injects UserRepository
    private UserRepository userRepository;
     // Login with email OR username
     // emailOrUsername (Can be either email or username)
     // password (User's password)
     // return User object if login successful. null if failed
    public User authenticate(String emailOrUsername, String password) {
        // Try to find by email first
        Optional<User> user = userRepository.findByEmail(emailOrUsername);
        
        // If not found by email try by username
        if (user.isEmpty()) {
            user = userRepository.findByUsername(emailOrUsername);
        }
        
        // Check if user exists and password matches
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user.get();  // Return the User object
        }
        
        return null;  // Login failed
    }
    
  //getUserById: Get user by ID
     // id: User ID to find
     // return: User object if found. null if not
    public User getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return user.get();
        }
        return null;
    }
    
    //findByEmail: Find user by email
    // email: Email address to search for
    // return: User if found. null if not
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
     
    //registerPlayer: Register a new player account
    // firstName: User's first name
    // lastName: User's last name
    // username: Unique username
    // email: Unique email address
    // password: User's password
   // return true if registration successful, false if username/email already exists
    public boolean registerPlayer(String firstName, String lastName, String username, String email, String password) {
        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            return false;  // Username taken
        }
        
        // Check if email already exists
        if (userRepository.findByEmail(email).isPresent()) {
            return false;  // Email already registered
        }
        
        // Create new user object
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole("PLAYER");   // Default role for new users
        user.setActive(true);      // New users are active by default
        
        userRepository.save(user);  // Save to database
        return true;  // Registration successful
    }
    
  // admin methods (admin only)
   //getAllUsers: return all users from database (including inactive)
    
    public List<User> getAllUsers() {
        return userRepository.findAll();  // Returns every user in the database
    }
    
    //updateUser: Update an existing user in the database
     // user: The user object with updated values
     // return The saved user object
    public User updateUser(User user) {
        return userRepository.save(user);  // Saves changes to database
    }
    
    // hardDeleteUser: delete a user from the database (CANNOT be undone)
    // id: ID of user to delete
    public void hardDeleteUser(Long id) {
        userRepository.deleteById(id);  // Completely removes user from database
    }
}
