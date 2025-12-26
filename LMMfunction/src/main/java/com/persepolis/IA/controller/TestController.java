package com.persepolis.IA.controller;

import com.persepolis.IA.dto.Message;
import com.persepolis.IA.services.AiClient;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.ArrayList;
import reactor.core.publisher.Mono;

@RestController
public class TestController {

    private final AiClient ai;

    // Usamos una lista mutable para construir el historial de la conversación
    private final List<Message> conversationHistory = new ArrayList<>();

    public TestController(AiClient ai) {
        this.ai = ai;
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
    public Mono<String> chat(@RequestParam String message) {
        // 1. Añadimos el mensaje del usuario al historial
        conversationHistory.add(new Message("user", message));

        // 2. Hacemos la llamada al servicio de chat con el historial completo
        return ai.chat(conversationHistory, true)
                .flatMap(aiResponse -> {
                    // 3. Añadimos la respuesta de la IA al historial
                    conversationHistory.add(new Message("assistant", aiResponse));
                    // 4. Devolvemos la respuesta de la IA al cliente
                    return Mono.just(aiResponse);
                });
    }

    // Endpoint para ver el historial actual
    @GetMapping("/test/chat/history")
    public List<Message> getHistory() {
        return conversationHistory;
    }
}
