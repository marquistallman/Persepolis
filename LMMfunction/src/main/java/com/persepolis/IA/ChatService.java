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
        String prompt = "Genera 5 preguntas numeradas (1., 2., etc) para afinar la búsqueda de un fondo de pantalla basado en: '" + request + "'. Las preguntas deben ser para responder con una escala del 1 al 10.";
        
        return aiClient.generate(prompt, true).map(response -> {
            List<String> generatedQuestions = Arrays.stream(response.split("\\n"))
                    .filter(line -> line.matches("^\\d+\\..*"))
                    .limit(5)
                    .collect(Collectors.toList());
            
            if (generatedQuestions.isEmpty()) {
                generatedQuestions = List.of("1. ¿Luminosidad? (1-10)", "2. ¿Color? (1-10)", "3. ¿Estilo? (1-10)", "4. ¿Complejidad? (1-10)", "5. ¿Vibe? (1-10)");
            }

            UserSession session = new UserSession();
            session.originalRequest = request;
            session.questions = generatedQuestions;
            sessions.put(userId, session);

            return "Entendido. Para afinar tu búsqueda, responde del 1 al 10:\n" + generatedQuestions.get(0);
        });
    }

    private Mono<String> processActiveSession(String userId, String message) {
        UserSession session = sessions.get(userId);
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
            Mono<String> resultMono;
            if (session.isStandard) {
                resultMono = Mono.just(generateStandardKeywords(session));
            } else {
                resultMono = generateKeywordsFromAI(session);
            }
            return resultMono.map(finalResult -> {
                sessions.remove(userId);
                return searchAndFormat(finalResult);
            });
        }
    }

    private String generateStandardKeywords(UserSession session) {
        StringBuilder query = new StringBuilder();
        List<Integer> answers = session.answers;
        
        // Bucle for que itera de 0 a 9 procesando las respuestas estándar
        for (int i = 0; i < answers.size(); i++) {
            if (i >= KEYWORD_MAPPING.size()) break;
            String[] options = KEYWORD_MAPPING.get(i);
            // Si la respuesta es > 5 usa la segunda opción (índice 1), si no la primera (índice 0)
            query.append(answers.get(i) > 5 ? options[1] : options[0]).append(" ");
        }
        return query.toString().trim();
    }

    private Mono<String> generateKeywordsFromAI(UserSession session) {
        String prompt = "Genera palabras clave de búsqueda para: '" + session.originalRequest + "' considerando estos valores (1-10) para las preguntas anteriores: " + session.answers;
        return aiClient.generate(prompt, false);
    }

    private String searchAndFormat(String query) {
        try {
            CScrap scraper = new CScrap();
            List<Map<String, String>> results = scraper.buscarWeb(query);
            
            if (results == null || results.isEmpty()) {
                return "No se encontraron fondos para: " + query;
            }
            StringBuilder sb = new StringBuilder("¡Aquí tienes tus fondos para '" + query + "'!:\n");
            for (Map<String, String> result : results) {
                sb.append(result.values()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Hubo un error buscando los fondos: " + e.getMessage();
        }
    }

    private static class UserSession {
        String originalRequest;
        List<String> questions;
        List<Integer> answers = new ArrayList<>();
        int currentQuestionIndex = 0;
        boolean isStandard = false;
    }
}
