package com.sharecycle.application;

import com.sharecycle.service.SessionStore;
import com.sharecycle.ui.LoginController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LoginUseCase loginUseCase;

    @Mock
    private SessionStore sessionStore;

    @InjectMocks
    private LoginController loginController;

    private String username;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(loginController).build();
        username = "loginuser";
    }

    @Test
    void loginReturns200() throws Exception {
        // Correct constructor with UUID, username, email, token
        LoginUseCase.LoginResponse mockResponse =
                new LoginUseCase.LoginResponse(UUID.randomUUID(), username, "user@example.com", "dummyToken");
        when(loginUseCase.execute(username, "password123")).thenReturn(mockResponse);

        String body = "{\n" +
                "  \"username\": \"" + username + "\",\n" +
                "  \"password\": \"password123\"\n" +
                "}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void logoutReturns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer dummyToken"))
                .andExpect(status().isNoContent());
    }
}
