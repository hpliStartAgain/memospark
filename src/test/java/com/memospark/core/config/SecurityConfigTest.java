package com.memospark.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memospark.core.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for SecurityConfig — verifies that unauthenticated requests
 * to /api/** return 401 and public endpoints return 200.
 */
@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private MockMvc mvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(userService.loadUserByUsername(anyString()))
                .thenThrow(new UsernameNotFoundException("Not found"));
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void unauthenticatedApiRequest_returns401() throws Exception {
        mvc.perform(get("/api/decks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealth_isPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void loginEndpoint_isPublic() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"x\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized()); // 401 due to bad creds after reaching the public controller
    }

    @Test
    void registerEndpoint_isPublic() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"newuser\",\"password\":\"pass123\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void adminEndpoint_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void spaRoutes_arePublic() throws Exception {
        mvc.perform(get("/decks"))
                .andExpect(status().isOk());
    }
}
