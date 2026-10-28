package com.warehouse.inventory.controller;

import com.warehouse.inventory.config.SecurityConfig;
import com.warehouse.inventory.config.WebConfig;
import com.warehouse.inventory.model.ContactMessage;
import com.warehouse.inventory.model.User;
import com.warehouse.inventory.repository.ContactMessageRepository;
import com.warehouse.inventory.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, ContactController.class})
@Import({SecurityConfig.class, WebConfig.class})
class AuthenticationAndContactControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ContactMessageRepository contactMessageRepository;

    @BeforeEach
    void configureCsrfSession() throws Exception {
        mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk());
    }

    @Test
    void loginCreatesSessionAndMeReturnsUsernameAndRole() throws Exception {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(new User("admin", passwordEncoder.encode("Admin@123"), User.Role.ADMIN)));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidLoginReturnsUnauthorized() throws Exception {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(new User("admin", passwordEncoder.encode("correct"), User.Role.ADMIN)));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"username":"admin","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registrationCreatesStandardUserWithEncodedPasswordAndSignsThemIn() throws Exception {
        String rawPassword = "a-strong-password";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User registeredUser = new User("new.operator@example.com", encodedPassword, User.Role.USER);
        when(userRepository.existsByUsername("new.operator@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUsername("new.operator@example.com")).thenReturn(Optional.of(registeredUser));

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"username":"New.Operator@Example.com","password":"a-strong-password","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("new.operator@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        verify(userRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(user ->
                user.getUsername().equals("new.operator@example.com")
                        && user.getRole() == User.Role.USER
                        && passwordEncoder.matches(rawPassword, user.getPassword())));
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new.operator@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void registrationRejectsDuplicateUsername() throws Exception {
        when(userRepository.existsByUsername("existing.user")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"username":"existing.user","password":"a-strong-password"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("That username is already taken"));

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void registrationRequiresStrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"username":"valid.user","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationRejectsPasswordsLongerThanBcryptByteLimit() throws Exception {
        String password = "é".repeat(37);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"username":"valid.user","password":"%s"}
                                """.formatted(password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must not exceed 72 UTF-8 bytes"));
    }

    @Test
    void contactFormIsPublicAndPersistsMessage() throws Exception {
        when(contactMessageRepository.save(org.mockito.ArgumentMatchers.any(ContactMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/contact")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"name":"A User","email":"a@example.com","message":"Please contact me"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotViewContactMessages() throws Exception {
        mockMvc.perform(get("/api/contact/messages"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanViewContactMessages() throws Exception {
        when(contactMessageRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(new ContactMessage("A User", "a@example.com", "Please contact me")));

        mockMvc.perform(get("/api/contact/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.messages[0].email").value("a@example.com"));
    }

    @Test
    void anonymousMeReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}