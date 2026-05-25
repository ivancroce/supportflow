package com.supportflow.user;

import com.supportflow.exception.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Return a lazy reference (Hibernate proxy) without hitting the DB. Use when you only need
     * the FK value — e.g., setting an owner on a new entity — not the user's other fields.
     */
    public User getReference(UUID id) {
        return userRepository.getReferenceById(id);
    }

    @Transactional
    public User findOrCreateDevUser(String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new User(email, null, null, AuthProvider.DEV)));
    }

    @Transactional
    public User findOrCreateOAuthUser(String email, String name, String avatarUrl, AuthProvider provider) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new User(email, name, avatarUrl, provider)));
    }
}
