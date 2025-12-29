package com.persepolis.IA.controller;

import com.persepolis.IA.ChatService;
import com.persepolis.IA.dto.Message;
import com.persepolis.IA.services.AiClient;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import reactor.core.publisher.Mono;

@RestController
public class TestController {

    private final AiClient ai;
    private final ChatService chatService;

    // Usamos una lista mutable para construir el historial de la conversación
    private final List<Message> conversationHistory = new ArrayList<>();

    public TestController(AiClient ai, ChatService chatService) {
        this.ai = ai;
        this.chatService = chatService;
    }

    @GetMapping("/test/ollama")
    public Mono<String> testOllama() {
        return ai.generate("Dime si estás vivo", true);
    }

    // Método para inicializar la conversación con un contexto
    @PostConstruct
    public void initializeConversation() {
        conversationHistory.add(new Message("system", "Eres un asistente experto en libros de ciencia ficción. Tus respuestas son amigables y concisas."));
    }

    @GetMapping("/test/chat")
    public Mono<Map<String, Object>> chat(@RequestParam String message) {
        // 1. Añadimos el mensaje del usuario al historial
        conversationHistory.add(new Message("user", message));

        // 2. Delegamos la lógica al ChatService (que maneja el flujo de preguntas)
        return chatService.processMessage(message)
                .flatMap(responseMap -> {
                    // 3. Añadimos la respuesta de la IA al historial
                    String textResponse = (String) responseMap.get("message");
                    conversationHistory.add(new Message("assistant", textResponse));
                    // 4. Devolvemos la respuesta de la IA al cliente
                    return Mono.just(responseMap);
                });
    }

    // Endpoint para ver el historial actual
    @GetMapping("/test/chat/history")
    public List<Message> getHistory() {
        return conversationHistory;
    }
}
