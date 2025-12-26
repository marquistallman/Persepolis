package com.persepolis.IA.services;
import com.persepolis.IA.dto.*;
import java.util.List;
import java.util.Map;
import com.persepolis.IA.dto.OllamaChatResponse;
import com.persepolis.IA.dto.OpenRouterRequest;
import com.persepolis.IA.dto.OllamaResponse;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AiClient {

    private final WebClient webClient;
    private final String openRouterKey;

    public AiClient(
            WebClient.Builder builder,
            @Value("${openrouter.api-key:}") String openRouterKey
    ) {
        this.webClient = builder.build();
        this.openRouterKey = openRouterKey;
    }

    // 🔥 Método unificado (REACTIVO)
    public Mono<String> generate(String prompt, boolean useLocal) {
        return useLocal
                ? queryOllama(prompt)
                : queryOpenRouter(prompt);
    }

    // 🔥 Método de Chat unificado (REACTIVO)
    public Mono<String> chat(List<Message> messages, boolean useLocal) {
        return useLocal
                ? chatWithOllama(messages)
                : chatWithOpenRouter(messages);
    }

    // ----------------------------
    // LOCAL: Ollama
    // ----------------------------
        private Mono<String> queryOllama(String prompt) {

        System.out.println("🔥 Enviando prompt al modelo local...");
        System.out.println(prompt);

        var body = Map.of(
                "model", "llama3.2",
                "prompt", prompt,
                "stream", false
        );

        return webClient.post()
            .uri("http://localhost:11434/api/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(OllamaResponse.class)
            .map(OllamaResponse::response)
            .doOnNext(text -> {
                System.out.println("✅ Modelo respondió:");
                System.out.println(text);
            });
        }

    private Mono<String> chatWithOllama(List<Message> messages) {
        System.out.println("🔥 Enviando chat al modelo local...");

        var body = Map.of(
                "model", "llama3.2",
                "messages", messages,
                "stream", false
        );

        return webClient.post()
                .uri("http://localhost:11434/api/chat") // <-- Endpoint de CHAT
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(OllamaChatResponse.class)
                .map(res -> res.message().getContent()); // <-- Extraemos el contenido del mensaje
    }


    // ----------------------------
    // ONLINE: OpenRouter
    // ----------------------------
    private Mono<String> queryOpenRouter(String prompt) {

        if (openRouterKey == null || openRouterKey.isBlank()) {
            return Mono.just("Falta configurar OPENROUTER_API_KEY");
        }

        // Para mantener la compatibilidad, convertimos el prompt simple a una lista de mensajes
        var messages = List.of(new Message("user", prompt));
        return chatWithOpenRouter(messages);
    }

    private Mono<String> chatWithOpenRouter(List<Message> messages) {
        var body = new OpenRouterRequest(
                "google/gemini-2.0-flash-lite-preview-02-05:free",
                messages
        );

        return webClient.post()
                .uri("https://openrouter.ai/api/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openRouterKey)
                .header("HTTP-Referer", "http://localhost")
                .header("X-Title", "ia-book")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(OpenRouterResponse.class)
                .map(res -> res.choices().get(0).message().getContent());
    }
}
