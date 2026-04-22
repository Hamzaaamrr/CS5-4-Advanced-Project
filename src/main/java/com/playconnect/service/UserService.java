package Service;

import Entity.User;
import repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service // This class handles services logic (login, register)
public class UserService {
    @Autowired // Spring automatically creates UserRepository here (no need for *new*)
    private UserRepository userRepository;
    // Login with email OR username
    public User authenticate(String emailOrUsername, String password) {
        // Try to find by email first
        Optional<User> user = userRepository.findByEmail(emailOrUsername);
        // If not found by email, try by username
        if (user.isEmpty()) {
            user = userRepository.findByUsername(emailOrUsername);
        }
        // Check password if it is == to the password of the user
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user.get();// Return the object (User)
        }
        
        return null; // else(Login failed)
    }
    // Get user by ID
    public User getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);  // Find user in DB
        if (user.isPresent()) // Check if user exists
        	{
            return user.get(); // Return the User object
        }
        return null; // User not found
    }
    // Register new user
    public boolean registerPlayer(String firstName, String lastName, String username, String email, String password) {
        // Check if username exists
        if (userRepository.findByUsername(username).isPresent()) {
            return false; // Username taken
        }
        // Check if email exists
        if (userRepository.findByEmail(email).isPresent()) {
            return false; // Email already registered
        }
        // Create new user
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole("PLAYER");
        user.setActive(true);
        userRepository.save(user); // Save to DB
        return true;  // Registration successful
    }
}
