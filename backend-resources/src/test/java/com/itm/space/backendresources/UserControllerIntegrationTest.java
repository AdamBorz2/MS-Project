package com.itm.space.backendresources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class     UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    TestRestTemplate restTemplate = new TestRestTemplate();

    private String token;
    @MockBean
    private UserService userService;

    @BeforeEach
    void getAccessToken() throws Exception {
        String url = "http://backend-gateway-client:8080/auth/realms/ITM/protocol/openid-connect/token";

        // Устанавливаем заголовки
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String requestBody = "client_id=" + URLEncoder.encode("backend-gateway-client", StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode("V36inROZmLSxRBAPWPwfAgMkSDorul0z", StandardCharsets.UTF_8) +
                "&username=" + URLEncoder.encode("user", StandardCharsets.UTF_8) +
                "&password=" + URLEncoder.encode("user", StandardCharsets.UTF_8) +
                "&grant_type=" + URLEncoder.encode("password", StandardCharsets.UTF_8);

        // Создаем HTTP-запрос
        HttpEntity<String> stringHttpEntity = new HttpEntity<>(requestBody, headers);

        // Отправляем запрос
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, stringHttpEntity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            // Парсим тело ответа и получаем access_token
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.getBody());
            token = root.path("access_token").asText();
        } else {
            throw new RuntimeException("Failed to get token: " + response.getStatusCode());
        }
    }

    @Test
    void createUser_ShouldReturnCreatedStatus_WhenUserIsSuccessfullyCreated() throws Exception {
        String userJson = "{ \"username\": \"john_doe\", \"firstName\": \"John\", \"lastName\": \"Doe\", \"email\": \"john.doe@example.com\", \"password\": \"password123\" }";

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    public void testGetUserById() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(
                "John",
                "Doe",
                "john.doe@example.com",
                List.of("ROLE_USER"),
                List.of("GROUP_USER")
        );

        // Мокаем сервис, чтобы он возвращал ожидаемый ответ
        when(userService.getUserById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(userResponse)));
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    public void testHello() throws Exception {
        mockMvc.perform(get("/api/users/hello")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("user"));

        // Здесь можно добавить проверку имени пользователя, если нужно
    }
}
