package com.persepolis.IA;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.persepolis.IA.Scraper.CScrap;
import com.persepolis.IA.Scraper.model.WallpaperDTO;
import com.persepolis.IA.services.AiClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final AiClient aiClient;
    private ChatData chatData;

    public ChatService(AiClient aiClient) {
        this.aiClient = aiClient;
        loadChatData();
    }

    private void loadChatData() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.chatData = mapper.readValue(new ClassPathResource("chat_data.json").getInputStream(), ChatData.class);
        } catch (IOException e) {
            throw new RuntimeException("Error cargando chat_data.json", e);
        }
    }

    // Almacenamiento temporal de sesiones (en memoria)
    // Usamos ConcurrentHashMap para seguridad en hilos con múltiples usuarios
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    public Mono<Map<String, Object>> processMessage(String message, String userId) {
        if (message == null) return Mono.just(simpleResponse(""));
        String lower = message.trim().toLowerCase(Locale.ROOT);

        // 1. Verificar si el usuario está en medio de un cuestionario activo
        // ESTE ES EL BUCLE DE INTERACCIÓN: Mientras exista la sesión, el usuario "se queda" aquí.
        if (sessions.containsKey(userId)) {
            return processActiveSession(userId, message);
        }

        if (isGreeting(lower)) {
            return Mono.just(simpleResponse(chatData.getResponse("greeting")));
        }

        if (isStandardRequest(lower)) {
            UserSession session = new UserSession();
            session.currentNodeId = "root";
            session.isStandard = true;
            sessions.put(userId, session);
            return Mono.just(simpleResponse(chatData.getResponse("standardStart", chatData.getDecisionTree().getRoot().getText())));
        }

        if (isUnsure(lower)) {
            return Mono.just(simpleResponse(chatData.getResponse("unsure")));
        }

        // 2. Caso: Usuario seguro con petición específica -> Iniciar flujo de 5 preguntas IA
        return startSpecificFlow(userId, message);
    }

    private boolean isGreeting(String message) {
        String pattern = "^(" + String.join("|", chatData.getGreetingPatterns()) + ").*";
        return message.matches(pattern);
    }

    private boolean isStandardRequest(String message) {
        return chatData.getStandardRequestKeywords().stream().anyMatch(message::contains);
    }

    private boolean isUnsure(String message) {
        if (message.length() < 4) return true;
        return chatData.getUnsureKeywords().stream().anyMatch(message::contains);
    }

    private Mono<Map<String, Object>> startSpecificFlow(String userId, String request) {
        // Enfoque determinista: Limpiamos la petición y usamos las preguntas estándar
        String keywords = extractKeywords(request);
        
        UserSession session = new UserSession();
        session.originalRequest = keywords.isEmpty() ? request : keywords;
        session.currentNodeId = "root";
        session.isStandard = true;
        sessions.put(userId, session);

        return Mono.just(simpleResponse(chatData.getResponse("specificStart", session.originalRequest, chatData.getDecisionTree().getRoot().getText())));
    }

    private Mono<Map<String, Object>> processActiveSession(String userId, String message) {
        UserSession session = sessions.get(userId);

        // 1. Verificar si estamos esperando confirmación de satisfacción
        if (session.waitingForSatisfaction) {
            String lower = message.trim().toLowerCase();
            if (lower.startsWith("si") || lower.startsWith("yes") || lower.startsWith("s") || lower.contains("ok")) {
                sessions.remove(userId);
                return Mono.just(simpleResponse(chatData.getResponse("satisfactionYes")));
            } else {
                // Usuario NO satisfecho -> Usar LLM para generar nuevas keywords
                return generateAlternativeKeywordsWithLLM(session).map(newQuery -> {
                    sessions.remove(userId);
                    Map<String, Object> searchResult = performSearch(newQuery, session.originalRequest);
                    String resultsText = (String) searchResult.get("message");
                    Map<String, Object> response = new HashMap<>(searchResult);
                    response.put("message", chatData.getResponse("satisfactionRetry", resultsText));
                    return response;
                });
            }
        }

        // Lógica del Árbol de Decisión
        ChatData.Node currentNode;
        if ("root".equals(session.currentNodeId)) {
            currentNode = chatData.getDecisionTree().getRoot();
        } else {
            currentNode = chatData.getDecisionTree().getNodes().get(session.currentNodeId);
        }

        String nextNodeId = null;

        if ("open".equals(currentNode.getType())) {
            // Pregunta abierta
            String input = message.trim();
            if (!input.equalsIgnoreCase("no") && !input.equalsIgnoreCase("ninguno") && !input.equalsIgnoreCase("skip")) {
                session.collectedKeywords.add(input);
            }
            nextNodeId = currentNode.getNext();
        } else {
            // Pregunta de opción múltiple (default)
            try {
                String inputKey = message.trim();
                ChatData.Option selectedOption = currentNode.getOptions().get(inputKey);
                
                if (selectedOption == null) {
                    throw new IllegalArgumentException("Opción no válida");
                }
                
                if (selectedOption.getKeyword() != null && !selectedOption.getKeyword().isEmpty()) {
                    session.collectedKeywords.add(selectedOption.getKeyword());
                }
                nextNodeId = selectedOption.getNext();
                
            } catch (Exception e) {
                return Mono.just(simpleResponse(chatData.getResponse("numberFormatError", currentNode.getText())));
            }
        }

        // Avanzar al siguiente nodo
        if (nextNodeId != null && !"END".equals(nextNodeId)) {
            session.currentNodeId = nextNodeId;
            ChatData.Node nextNode = chatData.getDecisionTree().getNodes().get(nextNodeId);
            return Mono.just(simpleResponse(chatData.getResponse("nextQuestion", nextNode.getText())));
        } else {
            // Flujo terminado
            String base = (session.originalRequest != null) ? session.originalRequest + " " : "";
            String finalResult = base + String.join(" ", session.collectedKeywords);
            
            session.lastGeneratedQuery = finalResult;
            session.waitingForSatisfaction = true;
            
            Map<String, Object> searchResult = performSearch(finalResult, session.originalRequest);
            String resultsText = (String) searchResult.get("message");
            searchResult.put("message", chatData.getResponse("satisfactionAsk", resultsText));
            return Mono.just(searchResult);
        }
    }

    private Mono<String> generateAlternativeKeywordsWithLLM(UserSession session) {
        String prompt = chatData.getResponse("llmPrompt", session.originalRequest, session.lastGeneratedQuery);
        return aiClient.generate(prompt, false);
    }

    private String extractKeywords(String text) {
        String processed = text.toLowerCase(Locale.ROOT);

        // 1. Normalización de Slang
        for (Map.Entry<String, String> entry : chatData.getSlangMapping().entrySet()) {
            processed = processed.replace(entry.getKey(), entry.getValue());
        }

        // 2. Limpieza y Tokenización
        String cleaned = processed.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s]", " ");
        List<String> tokens = Arrays.asList(cleaned.split("\\s+"));

        // 3. Filtrado: ¿Coincide con keywords que reconocen los sitios?
        List<String> validKeywords = tokens.stream()
                .filter(chatData.getSiteKeywords()::contains)
                .distinct()
                .collect(Collectors.toList());

        if (!validKeywords.isEmpty()) {
            return String.join(" ", validKeywords);
        }

        // Fallback: Si no hay coincidencias de sitio, usamos limpieza de stopwords estándar
        List<String> stops = chatData.getStopWords();
        return tokens.stream()
                .filter(w -> !w.isEmpty() && !stops.contains(w))
                .collect(Collectors.joining(" "));
    }

    private Map<String, Object> performSearch(String query, String originalRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            CScrap scraper = new CScrap();
            String[] keywords = query.split("\\|\\|\\|");
            Map<String, List<WallpaperDTO>> searchCache = new HashMap<>();
            
            // Usamos Maps auxiliares para contar duplicados basándonos en la URL
            Map<String, WallpaperDTO> uniqueResults = new HashMap<>();
            Map<String, Integer> urlCounts = new HashMap<>();

            for (String keyword : keywords) {
                String k = keyword.trim();
                if (k.isEmpty()) continue;
                
                // Usamos caché para no llamar al scraper múltiples veces por la misma palabra (debido al peso)
                List<WallpaperDTO> results = searchCache.computeIfAbsent(k, key -> scraper.buscarWeb(key));
                
                if (results != null) {
                    for (WallpaperDTO result : results) {
                        if (isSupportContent(result)) continue;
                        String url = result.getEnlace();
                        if (url == null) continue;
                        
                        uniqueResults.putIfAbsent(url, result);
                        urlCounts.put(url, urlCounts.getOrDefault(url, 0) + 1);
                    }
                }
            }
            
            if (urlCounts.isEmpty()) {
                return simpleResponse(chatData.getResponse("noResults", query));
            }

            List<WallpaperDTO> finalResultsList = urlCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Ordenar por popularidad (conteo)
                .map(e -> uniqueResults.get(e.getKey()))
                .collect(Collectors.toList());

            StringBuilder sb = new StringBuilder(chatData.getResponse("resultsHeader", originalRequest));
            
            response.put("message", sb.toString());
            response.put("results", finalResultsList);
            return response;

        } catch (Exception e) {
            return simpleResponse(chatData.getResponse("error", e.getMessage()));
        }
    }

    private Map<String, Object> simpleResponse(String text) {
        Map<String, Object> map = new HashMap<>();
        map.put("message", text);
        return map;
    }

    private boolean isSupportContent(WallpaperDTO result) {
        // Verificamos campos específicos del DTO
        String[] fieldsToCheck = {result.getTitulo(), result.getEnlace(), result.getTipo()};
        
        for (String val : fieldsToCheck) {
            if (val == null) continue;
            String v = val.toLowerCase();
            if (v.contains("support") || v.contains("help") || v.contains("contact") || 
                v.contains("faq") || v.contains("policy") || v.contains("terms")) return true;
            
            if (v.contains("/category/") || v.contains("/tag/") || v.contains("how to set") || 
                v.contains("login") || v.contains("signup") || v.contains("register")) return true;
            
            if (v.startsWith("http")) {
                String url = v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
                if (url.split("/").length <= 3) return true;
            }
        }
        return false;
    }

    private static class UserSession {
        String originalRequest;
        String currentNodeId;
        List<String> collectedKeywords = new ArrayList<>();
        boolean isStandard = false;
        boolean waitingForSatisfaction = false;
        String lastGeneratedQuery;
    }
}
