package com.persepolis.IA;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.persepolis.IA.Scraper.CScrap;
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
    private final Map<String, UserSession> sessions = new HashMap<>();

    public Mono<String> processMessage(String message) {
        if (message == null) return Mono.just("");
        String userId = "default_user"; // Identificador temporal (en producción usarías un ID de sesión real)
        String lower = message.trim().toLowerCase(Locale.ROOT);

        // 1. Verificar si el usuario está en medio de un cuestionario activo
        // ESTE ES EL BUCLE DE INTERACCIÓN: Mientras exista la sesión, el usuario "se queda" aquí.
        if (sessions.containsKey(userId)) {
            return processActiveSession(userId, message);
        }

        if (isGreeting(lower)) {
            return Mono.just(chatData.getResponse("greeting"));
        }

        if (isStandardRequest(lower)) {
            UserSession session = new UserSession();
            session.currentNodeId = "root";
            session.isStandard = true;
            sessions.put(userId, session);
            return Mono.just(chatData.getResponse("standardStart", chatData.getDecisionTree().getRoot().getText()));
        }

        if (isUnsure(lower)) {
            return Mono.just(chatData.getResponse("unsure"));
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

    private Mono<String> startSpecificFlow(String userId, String request) {
        // Enfoque determinista: Limpiamos la petición y usamos las preguntas estándar
        String keywords = extractKeywords(request);
        
        UserSession session = new UserSession();
        session.originalRequest = keywords.isEmpty() ? request : keywords;
        session.currentNodeId = "root";
        session.isStandard = true;
        sessions.put(userId, session);

        return Mono.just(chatData.getResponse("specificStart", session.originalRequest, chatData.getDecisionTree().getRoot().getText()));
    }

    private Mono<String> processActiveSession(String userId, String message) {
        UserSession session = sessions.get(userId);

        // 1. Verificar si estamos esperando confirmación de satisfacción
        if (session.waitingForSatisfaction) {
            String lower = message.trim().toLowerCase();
            if (lower.startsWith("si") || lower.startsWith("yes") || lower.startsWith("s") || lower.contains("ok")) {
                sessions.remove(userId);
                return Mono.just(chatData.getResponse("satisfactionYes"));
            } else {
                // Usuario NO satisfecho -> Usar LLM para generar nuevas keywords
                return generateAlternativeKeywordsWithLLM(session).map(newQuery -> {
                    sessions.remove(userId);
                    return chatData.getResponse("satisfactionRetry", searchAndFormat(newQuery, session.originalRequest));
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
                return Mono.just(chatData.getResponse("numberFormatError", currentNode.getText()));
            }
        }

        // Avanzar al siguiente nodo
        if (nextNodeId != null && !"END".equals(nextNodeId)) {
            session.currentNodeId = nextNodeId;
            ChatData.Node nextNode = chatData.getDecisionTree().getNodes().get(nextNodeId);
            return Mono.just(chatData.getResponse("nextQuestion", nextNode.getText()));
        } else {
            // Flujo terminado
            String base = (session.originalRequest != null) ? session.originalRequest + " " : "";
            String finalResult = base + String.join(" ", session.collectedKeywords);
            
            session.lastGeneratedQuery = finalResult;
            session.waitingForSatisfaction = true;
            
            String results = searchAndFormat(finalResult, session.originalRequest);
            return Mono.just(chatData.getResponse("satisfactionAsk", results));
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

    private String searchAndFormat(String query, String originalRequest) {
        try {
            CScrap scraper = new CScrap();
            String[] keywords = query.split("\\|\\|\\|");
            Map<String, List<Map<String, String>>> searchCache = new HashMap<>();
            Map<Map<String, String>, Integer> resultCounts = new HashMap<>();

            for (String keyword : keywords) {
                String k = keyword.trim();
                if (k.isEmpty()) continue;
                
                // Usamos caché para no llamar al scraper múltiples veces por la misma palabra (debido al peso)
                List<Map<String, String>> results = searchCache.computeIfAbsent(k, key -> scraper.buscarWeb(key));
                
                if (results != null) {
                    for (Map<String, String> result : results) {
                        if (isSupportContent(result)) continue;
                        resultCounts.put(result, resultCounts.getOrDefault(result, 0) + 1);
                    }
                }
            }
            
            if (resultCounts.isEmpty()) {
                return chatData.getResponse("noResults", query);
            }

            List<Map.Entry<Map<String, String>, Integer>> sortedResults = new ArrayList<>(resultCounts.entrySet());
            sortedResults.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            StringBuilder sb = new StringBuilder(chatData.getResponse("resultsHeader", originalRequest));
            for (Map.Entry<Map<String, String>, Integer> entry : sortedResults) {
                sb.append(entry.getKey().values()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return chatData.getResponse("error", e.getMessage());
        }
    }

    private boolean isSupportContent(Map<String, String> result) {
        for (String val : result.values()) {
            if (val == null) continue;
            String v = val.toLowerCase();
            if (v.contains("support") || v.contains("help") || v.contains("contact") || 
                v.contains("faq") || v.contains("policy") || v.contains("terms")) return true;
            
            // Filtros de limpieza (categorías, tutoriales, login)
            if (v.contains("/category/") || v.contains("/tag/") || v.contains("how to set") || 
                v.contains("login") || v.contains("signup") || v.contains("register")) return true;
            
            // Filtrar homepages (URLs cortas sin path)
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
