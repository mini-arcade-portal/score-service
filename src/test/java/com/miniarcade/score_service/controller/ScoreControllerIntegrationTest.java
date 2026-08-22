package com.miniarcade.score_service.controller;

import com.miniarcade.score_service.entity.Difficulty;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ScoreControllerIntegrationTest {

    private static final String TEST_SECRET =
            "integration-test-secret-key-at-least-64-bytes-long-for-hs512-xxxxxxxxxxxxxxxxxx";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void overrideJwtSecret(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> TEST_SECRET);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenFor(Long userId, String username) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .claim("role", "USER")
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    private String startSessionAndGetId(String token, String gameType, Difficulty difficulty) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("gameType", gameType, "difficulty", difficulty));
        String response = mockMvc.perform(post("/api/scores/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("sessionId").asText();
    }

    @Test
    void startSession_returnsCreated_withSessionId() throws Exception {
        String token = tokenFor(1L, "alice");
        String body = objectMapper.writeValueAsString(Map.of("gameType", "snake", "difficulty", "HARD"));

        mockMvc.perform(post("/api/scores/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void startSession_unauthenticated_returnsUnauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("gameType", "snake", "difficulty", "HARD"));

        mockMvc.perform(post("/api/scores/sessions")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void submit_returnsCreated_forValidSessionAndPlausibleScore() throws Exception {
        String token = tokenFor(2L, "bob");
        String sessionId = startSessionAndGetId(token, "snake", Difficulty.EASY);
        String body = objectMapper.writeValueAsString(
                Map.of("gameType", "snake", "difficulty", "EASY", "score", 0, "sessionId", sessionId));

        mockMvc.perform(post("/api/scores")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.score").value(0));
    }

    @Test
    void submit_returnsNotFound_whenSessionDoesNotExist() throws Exception {
        String token = tokenFor(3L, "carol");
        String body = objectMapper.writeValueAsString(
                Map.of("gameType", "snake", "difficulty", "EASY", "score", 0, "sessionId", UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/scores")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void submit_returnsNotFound_whenSessionBelongsToAnotherUser() throws Exception {
        String ownerToken = tokenFor(4L, "dave");
        String attackerToken = tokenFor(5L, "eve");
        String sessionId = startSessionAndGetId(ownerToken, "snake", Difficulty.EASY);
        String body = objectMapper.writeValueAsString(
                Map.of("gameType", "snake", "difficulty", "EASY", "score", 0, "sessionId", sessionId));

        mockMvc.perform(post("/api/scores")
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void submit_returnsConflict_onSessionReplay() throws Exception {
        String token = tokenFor(6L, "frank");
        String sessionId = startSessionAndGetId(token, "snake", Difficulty.EASY);
        String body = objectMapper.writeValueAsString(
                Map.of("gameType", "snake", "difficulty", "EASY", "score", 0, "sessionId", sessionId));

        mockMvc.perform(post("/api/scores")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/scores")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void submit_returnsUnprocessableEntity_forImplausibleScore() throws Exception {
        String token = tokenFor(7L, "grace");
        String sessionId = startSessionAndGetId(token, "snake", Difficulty.HARD);
        String body = objectMapper.writeValueAsString(
                Map.of("gameType", "snake", "difficulty", "HARD", "score", 999_999, "sessionId", sessionId));

        mockMvc.perform(post("/api/scores")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void submit_returnsBadRequest_whenGameTypeOrDifficultyMismatchesSession() throws Exception {
        String token = tokenFor(8L, "heidi");
        String sessionId = startSessionAndGetId(token, "snake", Difficulty.EASY);
        String body = objectMapper.writeValueAsString(
                Map.of("gameType", "flappybird", "difficulty", "EASY", "score", 0, "sessionId", sessionId));

        mockMvc.perform(post("/api/scores")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submit_returnsBadRequest_whenSessionIdMissing() throws Exception {
        String token = tokenFor(9L, "ivan");
        String body = objectMapper.writeValueAsString(
                Map.of("gameType", "snake", "difficulty", "EASY", "score", 0));

        mockMvc.perform(post("/api/scores")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
