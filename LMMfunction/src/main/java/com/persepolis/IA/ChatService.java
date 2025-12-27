package com.persepolis.IA;

import com.persepolis.IA.Scraper.CScrap;
import com.persepolis.IA.services.AiClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

    public ChatService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    private static final List<String> STANDARD_QUESTIONS = List.of(
        "1. ¿Nivel de luminosidad? (1: Muy oscuro, 10: Muy brillante)",
        "2. ¿Saturación de color? (1: Blanco y negro, 10: Colores neón)",
        "3. ¿Estilo visual? (1: Abstracto/Surrealista, 10: Fotorealista)",
        "4. ¿Complejidad? (1: Minimalista, 10: Muy detallado)",
        "5. ¿Época? (1: Retro/Vintage, 10: Futurista/Sci-Fi)",
        "6. ¿Temperatura? (1: Frío/Azul, 10: Cálido/Naranja)",
        "7. ¿Energía? (1: Calma/Zen, 10: Acción/Caos)",
        "8. ¿Entorno? (1: Interior/Tecnológico, 10: Naturaleza/Exterior)",
        "9. ¿Temática? (1: Cotidiano, 10: Fantasía)",
        "10. ¿Enfoque? (1: Paisaje amplio, 10: Primer plano/Macro)"
    );

    // Mapeo de palabras clave para las 10 preguntas (Opción A <= 5, Opción B > 5)
    private static final List<String[]> KEYWORD_MAPPING = List.of(
        new String[]{"dark", "bright"},          // 1. Luminosidad
        new String[]{"monochrome", "colorful"},  // 2. Saturación
        new String[]{"abstract", "realistic"},   // 3. Estilo
        new String[]{"minimalist", "detailed"},  // 4. Complejidad
        new String[]{"vintage", "futuristic"},   // 5. Época
        new String[]{"cold", "warm"},            // 6. Temperatura
        new String[]{"calm", "dynamic"},         // 7. Energía
        new String[]{"tech", "nature"},          // 8. Entorno
        new String[]{"daily", "fantasy"},        // 9. Temática
        new String[]{"landscape", "macro"}       // 10. Enfoque
    );

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
            return Mono.just("¡Hola! / Hello! / Bonjour! ¿Tienes una idea específica? Si no, escribe 'estándar' para iniciar nuestra búsqueda guiada.");
        }

        if (isStandardRequest(lower)) {
            UserSession session = new UserSession();
            session.questions = STANDARD_QUESTIONS;
            session.isStandard = true;
            sessions.put(userId, session);
            return Mono.just("¡Perfecto! Iniciemos el proceso estándar (10 preguntas).\n" + STANDARD_QUESTIONS.get(0));
        }

        if (isUnsure(lower)) {
            return Mono.just("No estoy seguro de lo que buscas. Te recomiendo escribir 'estándar' para que te ayudemos con preguntas clave.");
        }

        // 2. Caso: Usuario seguro con petición específica -> Iniciar flujo de 5 preguntas IA
        return startSpecificFlow(userId, message);
    }

    private boolean isGreeting(String message) {
        return message.matches("^(hola|hello|hi|bonjour|salut|ola|hallo|buenos|buenas|saludos|hey|good morning).*");
    }

    private boolean isStandardRequest(String message) {
        return message.contains("estandar") || message.contains("estándar") || 
               message.contains("standard") || message.contains("guia") || message.contains("guide");
    }

    private boolean isUnsure(String message) {
        return message.length() < 4 || message.contains("no se") || message.contains("idk") || message.contains("maybe");
    }

    private Mono<String> startSpecificFlow(String userId, String request) {
        // Enfoque determinista: Limpiamos la petición y usamos las preguntas estándar
        String keywords = extractKeywords(request);
        
        UserSession session = new UserSession();
        session.originalRequest = keywords.isEmpty() ? request : keywords;
        session.questions = STANDARD_QUESTIONS;
        session.isStandard = true;
        sessions.put(userId, session);

        return Mono.just("Entendido. Buscaré fondos sobre: '" + session.originalRequest + "'.\n" +
                         "Para afinar los resultados, responde estas 10 preguntas (1-10):\n" + 
                         STANDARD_QUESTIONS.get(0));
    }

    private Mono<String> processActiveSession(String userId, String message) {
        UserSession session = sessions.get(userId);

        // 1. Verificar si estamos esperando confirmación de satisfacción
        if (session.waitingForSatisfaction) {
            String lower = message.trim().toLowerCase();
            if (lower.startsWith("si") || lower.startsWith("yes") || lower.startsWith("s") || lower.contains("ok")) {
                sessions.remove(userId);
                return Mono.just("¡Genial! Me alegro de haber ayudado.");
            } else {
                // Usuario NO satisfecho -> Usar LLM para generar nuevas keywords
                return generateAlternativeKeywordsWithLLM(session).map(newQuery -> {
                    sessions.remove(userId);
                    return searchAndFormat(newQuery, session.originalRequest) + "\n\nEspero que estos resultados sean mejores.";
                });
            }
        }

        try {
            int rating = Integer.parseInt(message.trim());
            if (rating < 1 || rating > 10) throw new NumberFormatException();
            session.answers.add(rating);
        } catch (NumberFormatException e) {
            return Mono.just("Por favor, responde únicamente con un número del 1 al 10.\n" + session.questions.get(session.currentQuestionIndex));
        }

        session.currentQuestionIndex++;

        if (session.currentQuestionIndex < session.questions.size()) {
            return Mono.just("Siguiente (" + (session.currentQuestionIndex + 1) + "/" + session.questions.size() + "):\n" + session.questions.get(session.currentQuestionIndex));
        } else {
            // Flujo terminado
            String finalResult = generateStandardKeywords(session);
            session.lastGeneratedQuery = finalResult;
            session.waitingForSatisfaction = true;
            
            String results = searchAndFormat(finalResult, session.originalRequest);
            return Mono.just(results + "\n\n¿Estás satisfecho con los resultados? (Sí/No)");
        }
    }

    private Mono<String> generateAlternativeKeywordsWithLLM(UserSession session) {
        String prompt = "El usuario buscó: '" + session.originalRequest + "'.\n" +
                        "Se usaron estas palabras clave: '" + session.lastGeneratedQuery + "' pero el usuario NO está satisfecho.\n" +
                        "Genera una lista de 3 a 5 nuevas combinaciones de palabras clave (queries) para mejorar la búsqueda.\n" +
                        "Deben ser diferentes a las anteriores. Devuelve SOLO las palabras clave separadas por ' ||| '.\n" +
                        "Ejemplo: query one ||| query two ||| query three";
        return aiClient.generate(prompt, false);
    }

    private String generateStandardKeywords(UserSession session) {
        StringBuilder query = new StringBuilder();
        List<Integer> answers = session.answers;
        String base = session.originalRequest;
        // Repetimos las palabras clave base para darles mayor peso en la búsqueda
        String prefix = (base != null && !base.isEmpty()) ? base + " " + base + " " : "";
        
        // Bucle for que itera de 0 a 9 procesando las respuestas estándar
        for (int i = 0; i < answers.size(); i++) {
            if (i >= KEYWORD_MAPPING.size()) break;
            String[] options = KEYWORD_MAPPING.get(i);
            // Si la respuesta es > 5 usa la segunda opción (índice 1), si no la primera (índice 0)
            String trait = answers.get(i) > 5 ? options[1] : options[0];
            query.append(prefix).append(trait).append(" ||| ");
        }
        return query.toString();
    }

    private String extractKeywords(String text) {
        // Reemplazar puntuación con espacios y limpiar
        String cleaned = text.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s]", " ");
        String[] stopWords = {"quiero", "un", "una", "el", "la", "los", "las", "fondo", "fondos", "de", "del", "pantalla", "wallpaper", "wallpapers", "imagen", "imagenes", "foto", "fotos", "sobre", "about", "for", "picture", "pictures", "dame", "ver", "show", "me", "y", "and", "with", "con", "busco", "necesito", "tienes", "hola", "hey", "buenos", "dias"};
        List<String> stops = Arrays.asList(stopWords);
        return Arrays.stream(cleaned.split("\\s+"))
                .filter(w -> !w.isEmpty() && !stops.contains(w.toLowerCase()))
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
                return "No se encontraron fondos para: " + query;
            }

            List<Map.Entry<Map<String, String>, Integer>> sortedResults = new ArrayList<>(resultCounts.entrySet());
            sortedResults.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            StringBuilder sb = new StringBuilder("¡Aquí tienes tus fondos para '" + originalRequest + "'!:\n");
            for (Map.Entry<Map<String, String>, Integer> entry : sortedResults) {
                sb.append(entry.getKey().values()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Hubo un error buscando los fondos: " + e.getMessage();
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
        List<String> questions;
        List<Integer> answers = new ArrayList<>();
        int currentQuestionIndex = 0;
        boolean isStandard = false;
        boolean waitingForSatisfaction = false;
        String lastGeneratedQuery;
    }
}
