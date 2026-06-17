package com.securityincidentmanager.controller;

import com.securityincidentmanager.dto.response.UserResponse;
import com.securityincidentmanager.auth.CustomUserDetailsService;
import com.securityincidentmanager.auth.JwtTokenService;
import com.securityincidentmanager.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_shouldReturn200_withResponse() throws Exception {
        UUID id = UUID.randomUUID();

        UserResponse response = new UserResponse();
        response.setId(id);
        response.setUsername("jdoe");

        when(userService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.username").value("jdoe"));
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_shouldReturn200_withList() throws Exception {
        UserResponse response = new UserResponse();
        response.setUsername("jdoe");

        when(userService.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("jdoe"));
    }

    // ── getByEmail ────────────────────────────────────────────────────────────

    @Test
    void getByEmail_shouldReturn200_withResponse() throws Exception {
        String email = "jdoe@example.com";

        UserResponse response = new UserResponse();
        response.setEmail(email);
        response.setUsername("jdoe");

        when(userService.getByEmail(email)).thenReturn(response);

        mockMvc.perform(get("/api/users/email/{email}", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.username").value("jdoe"));
    }
}