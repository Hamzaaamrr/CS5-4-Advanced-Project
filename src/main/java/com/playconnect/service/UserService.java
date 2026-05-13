package com.playconnect.service;

import com.playconnect.entity.User;
import com.playconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    public User authenticate(String emailOrUsername, String password) {
        Optional<User> user = userRepository.findByEmail(emailOrUsername);
        if (user.isEmpty()) {
            user = userRepository.findByUsername(emailOrUsername);
        }
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user.get();
        }
        
        return null;
    }
    public User getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return user.get();
        }
        return null;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean hardDeleteUser(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            userRepository.delete(user.get());
            return true;
        }
        return false;
    }
    public boolean registerPlayer(String firstName, String lastName, String username, String email, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            return false;
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return false;
        }
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole("PLAYER");
        user.setActive(true);
        userRepository.save(user);
        return true;
    }
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    public User findByUsername(String name) {
        throw new UnsupportedOperationException("Unimplemented method 'findByUsername'");
    }
}
