package com.persepolis.IA.controller;

import com.persepolis.IA.ChatService;
import com.persepolis.IA.dto.Message;
import com.persepolis.IA.services.AiClient;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin(origins = "*")
public class TestController {

    private final AiClient ai;
    private final ChatService chatService;

    // Mapa para guardar el historial separado por ID de sesión del usuario
    private final Map<String, List<Message>> userHistories = new ConcurrentHashMap<>();

    public TestController(AiClient ai, ChatService chatService) {
        this.ai = ai;
        this.chatService = chatService;
    }

    @GetMapping("/test/ollama")
    public Mono<String> testOllama() {
        return ai.generate("Dime si estás vivo", true);
    }

    // Helper para obtener o crear el historial de un usuario específico
    private List<Message> getHistoryForUser(String sessionId) {
        return userHistories.computeIfAbsent(sessionId, k -> {
            List<Message> history = new ArrayList<>();
            history.add(new Message("system", "Eres un asistente experto en wallpapers. Tus respuestas son amigables y concisas."));
            return history;
        });
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
