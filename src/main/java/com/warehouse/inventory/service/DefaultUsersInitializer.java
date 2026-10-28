package com.warehouse.inventory.service;

import com.warehouse.inventory.model.User;
import com.warehouse.inventory.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultUsersInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DefaultUsersInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.admin.username:admin}") String adminUsername,
            @Value("${app.auth.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername.trim().toLowerCase(java.util.Locale.ROOT);
        this.adminPassword = requirePassword("ADMIN_PASSWORD", adminPassword);
    }

    @Override
    public void run(String... args) {
        createIfMissing(adminUsername, adminPassword, User.Role.ADMIN);
    }

    private void createIfMissing(String username, String password, User.Role role) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(new User(username, passwordEncoder.encode(password), role));
        }
    }

    private static String requirePassword(String variable, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(variable + " must be configured before the application starts");
        }
        return password;
    }
}