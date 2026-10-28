package com.warehouse.inventory.service;

import com.warehouse.inventory.repository.UserRepository;
import com.warehouse.inventory.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.argThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultUsersInitializerTest {
    @Test
    void requiresAdminPassword() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                new DefaultUsersInitializer(
                        mock(UserRepository.class),
                        mock(PasswordEncoder.class),
                        "admin",
                        " "));

        assertEquals("ADMIN_PASSWORD must be configured before the application starts", exception.getMessage());
    }

    @Test
    void createsOnlyTheConfiguredAdminAccount() {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repository.existsByUsername("admin")).thenReturn(false);
        when(encoder.encode("configured-admin-password")).thenReturn("encoded-admin-password");
        DefaultUsersInitializer initializer = new DefaultUsersInitializer(
                repository, encoder, "ADMIN", "configured-admin-password");

        initializer.run();

        verify(repository).save(argThat(user -> user.getUsername().equals("admin")
                && user.getRole() == User.Role.ADMIN
                && user.getPassword().equals("encoded-admin-password")));
        verify(repository, never()).existsByUsername("user");
    }
}
