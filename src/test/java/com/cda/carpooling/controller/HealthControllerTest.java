package com.cda.carpooling.controller;

import com.cda.carpooling.config.SecurityConfig;
import com.cda.carpooling.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
@DisplayName("HealthController - Tests d'intégration")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("Devrait retourner 200 avec message succès si BDD connectée")
    void shouldReturn200WhenDatabaseConnected() throws Exception {
        // Given
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);

        // When & Then
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("✅")))
                .andExpect(content().string(containsString("PostgreSQL")));
    }

    @Test
    @DisplayName("Devrait retourner 200 avec message erreur si BDD inaccessible")
    void shouldReturn200WithErrorMessageWhenDatabaseFails() throws Exception {
        // Given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        // When & Then
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("❌")))
                .andExpect(content().string(containsString("Connection refused")));
    }
}