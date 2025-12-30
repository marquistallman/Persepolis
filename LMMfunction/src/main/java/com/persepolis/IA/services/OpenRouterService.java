package com.persepolis.IA.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenRouterService {

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String model;

    @Value("${app.domain}")
    private String siteUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenRouterService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String generateResponse(String prompt) {
        String url = "https://openrouter.ai/api/v1/chat/completions";

        // Headers requeridos por OpenRouter
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("HTTP-Referer", siteUrl); // Para rankings de OpenRouter
        headers.set("X-Title", "WallPaperSystem");
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Cuerpo de la petición (JSON)
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        
        body.put("messages", List.of(message));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return extractContentFromResponse(response.getBody());
            } else {
                return "Error en OpenRouter: " + response.getStatusCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error conectando con IA: " + e.getMessage();
        }
    }

    private String extractContentFromResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            // OpenRouter devuelve estructura estándar de OpenAI: choices[0].message.content
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            return "Error procesando respuesta JSON";
        }
    }
}