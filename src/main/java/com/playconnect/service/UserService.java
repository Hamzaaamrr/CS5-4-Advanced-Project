package com.playconnect.service;

import com.playconnect.entity.User;
import com.playconnect.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> resolveUser(User user) {
        if (user == null) return Optional.empty();
        if (user.getId() != null) {
            return userRepository.findById(user.getId());
        }
        if (user.getEmail() != null) {
            return userRepository.findByEmail(user.getEmail());
        }
        return Optional.empty();
    }
}
