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
        String prompt = "Analiza la petición: '" + request + "'.\n" +
                        "1. Extrae palabras clave OBLIGATORIAS en INGLÉS (tema central) para buscar en Wallhaven/Google.\n" +
                        "2. Genera 5 preguntas en ESPAÑOL para afinar. Incluye las opciones en el texto (ej: '¿Tono? (1: Claro, 10: Oscuro)').\n" +
                        "3. Define keywords en INGLÉS para cada extremo (1 y 10).\n" +
                        "Formato OBLIGATORIO (sin markdown, usa | como separador):\n" +
                        "MANDATORY: kw1, kw2\n" +
                        "Pregunta | Keyword_Bajo_Ingles | Keyword_Alto_Ingles | Peso_1_3";
        
        return aiClient.generate(prompt, true).map(response -> {
            List<String> generatedQuestions = new ArrayList<>();
            List<DynamicQuestion> dynamicData = new ArrayList<>();
            List<String> mandatory = new ArrayList<>();

            for (String line : response.split("\\n")) {
                String cleanLine = line.replaceAll("[*`]", "").trim();
                if (cleanLine.toUpperCase().startsWith("MANDATORY:")) {
                    String[] parts = cleanLine.substring(10).split("[,\\s]+");
                    for (String p : parts) if (!p.isEmpty()) mandatory.add(p.trim());
                } else if (cleanLine.contains("|")) {
                    String[] parts = cleanLine.split("\\|");
                    if (parts.length >= 4) {
                        String q = parts[0].trim();
                        generatedQuestions.add(q);
                        int w = 1;
                        try { w = Integer.parseInt(parts[3].trim()); } catch (Exception e) {}
                        dynamicData.add(new DynamicQuestion(q, parts[1].trim(), parts[2].trim(), w));
                    }
                }
            }
            
            if (generatedQuestions.isEmpty()) {
                generatedQuestions = List.of("1. ¿Luminosidad? (1-10)", "2. ¿Color? (1-10)", "3. ¿Estilo? (1-10)", "4. ¿Complejidad? (1-10)", "5. ¿Vibe? (1-10)");
                dynamicData.add(new DynamicQuestion("1. ¿Luminosidad?", "bright", "dark", 1));
                dynamicData.add(new DynamicQuestion("2. ¿Color?", "monochrome", "colorful", 1));
                dynamicData.add(new DynamicQuestion("3. ¿Estilo?", "abstract", "realistic", 1));
                dynamicData.add(new DynamicQuestion("4. ¿Complejidad?", "minimalist", "detailed", 1));
                dynamicData.add(new DynamicQuestion("5. ¿Vibe?", "calm", "energetic", 1));
            }

            if (mandatory.isEmpty()) mandatory.add(request);

            UserSession session = new UserSession();
            session.originalRequest = request;
            session.questions = generatedQuestions;
            session.dynamicQuestions = dynamicData;
            session.mandatoryKeywords = mandatory;
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
                resultMono = Mono.just(generateWeightedKeywords(session));
            }
            return resultMono.map(finalResult -> {
                sessions.remove(userId);
                return searchAndFormat(finalResult, session.originalRequest);
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
            query.append(answers.get(i) > 5 ? options[1] : options[0]).append(" ||| ");
        }
        return query.toString();
    }

    private String generateWeightedKeywords(UserSession session) {
        StringBuilder query = new StringBuilder();
        String mandatory = String.join(" ", session.mandatoryKeywords);
        
        // Base: Mandatory. Usamos solo las keywords procesadas (en inglés) para mejor resultado.
        query.append(mandatory).append(" ||| ");

        for (int i = 0; i < session.answers.size(); i++) {
            if (i >= session.dynamicQuestions.size()) break;
            DynamicQuestion dq = session.dynamicQuestions.get(i);
            String keyword = (session.answers.get(i) > 5) ? dq.highKeyword : dq.lowKeyword;
            
            // Combinamos obligatorio con preferencia para asegurar relevancia
            String combined = mandatory + " " + keyword;
            for(int w=0; w < dq.weight; w++) query.append(combined).append(" ||| ");
        }
        return query.toString();
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
        List<DynamicQuestion> dynamicQuestions = new ArrayList<>();
        List<String> mandatoryKeywords = new ArrayList<>();
    }

    private static class DynamicQuestion {
        String text;
        String lowKeyword;
        String highKeyword;
        int weight;

        public DynamicQuestion(String text, String lowKeyword, String highKeyword, int weight) {
            this.text = text;
            this.lowKeyword = lowKeyword;
            this.highKeyword = highKeyword;
            this.weight = weight;
        }
    }
}
