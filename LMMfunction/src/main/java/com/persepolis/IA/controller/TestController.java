package com.persepolis.IA.controller;

import com.persepolis.IA.ChatService;
import com.persepolis.IA.dto.Message;
import com.persepolis.IA.services.OpenRouterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin(origins = "*")
public class TestController {

    private final OpenRouterService openRouterService;
    private final ChatService chatService;

    // Reemplazo de Caffeine: Mapa nativo + Wrapper para controlar el tiempo
    private final Map<String, SessionData> userHistories = new ConcurrentHashMap<>();

    // Clase auxiliar para guardar el historial y cuándo fue la última vez que se usó
    private static class SessionData {
        final List<Message> history;
        long lastAccessTime;

        SessionData(List<Message> history) {
            this.history = history;
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    public TestController(OpenRouterService openRouterService, ChatService chatService) {
        this.openRouterService = openRouterService;
        this.chatService = chatService;
    }

    @GetMapping("/test/openrouter")
    public String testOpenRouter() {
        return openRouterService.generateResponse("Dime si estás vivo. Soy OpenRouter.");
    }

    // Helper para obtener o crear el historial de un usuario específico
    private List<Message> getHistoryForUser(String sessionId) {
        SessionData session = userHistories.computeIfAbsent(sessionId, k -> {
            List<Message> history = new CopyOnWriteArrayList<>();
            history.add(new Message("system", "Eres un asistente experto en wallpapers. Tus respuestas son amigables y concisas."));
            return new SessionData(history);
        });
        
        // Actualizamos el tiempo de acceso para que no se borre
        session.lastAccessTime = System.currentTimeMillis();
        return session.history;
    }

    // Tarea programada: Limpieza manual de RAM (Reemplaza la expiración automática de Caffeine)
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void cleanupRam() {
        long now = System.currentTimeMillis();
        long oneHour = TimeUnit.HOURS.toMillis(1);

        // Borrar sesiones que no se han usado en 1 hora
        userHistories.entrySet().removeIf(entry -> (now - entry.getValue().lastAccessTime) > oneHour);

        // Protección extra: Si hay demasiadas sesiones, limpiar todo para evitar crash
        if (userHistories.size() > 1000) {
            userHistories.clear();
        }
    }

    @GetMapping("/test/chat")
    public Mono<Map<String, Object>> chat(
            @RequestParam String message, 
            @RequestHeader(value = "X-Session-ID", defaultValue = "guest") String sessionId) {
        
        List<Message> history = getHistoryForUser(sessionId);
        
        // 1. Añadimos el mensaje del usuario al historial
        history.add(new Message("user", message));

        // 2. Delegamos la lógica al ChatService (que maneja el flujo de preguntas)
        return chatService.processMessage(message, sessionId)
                .flatMap(responseMap -> {
                    // 3. Añadimos la respuesta de la IA al historial
                    String textResponse = (String) responseMap.get("message");
                    history.add(new Message("assistant", textResponse));
                    // 4. Devolvemos la respuesta de la IA al cliente
                    return Mono.just(responseMap);
                });
    }

    // Endpoint para ver el historial actual
    @GetMapping("/test/chat/history")
    public List<Message> getHistory(@RequestHeader(value = "X-Session-ID", defaultValue = "guest") String sessionId) {
        return getHistoryForUser(sessionId);
    }
}
