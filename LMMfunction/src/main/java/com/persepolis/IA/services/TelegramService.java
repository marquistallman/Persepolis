package com.persepolis.IA.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendNotification(String message) {
        if (botToken == null || botToken.isEmpty() || chatId == null || chatId.isEmpty()) {
            System.out.println("--- TELEGRAM: Credenciales no configuradas, saltando notificación.");
            return;
        }
        
        // Usamos placeholders {token} etc para que RestTemplate codifique la URL correctamente
        String url = "https://api.telegram.org/bot{token}/sendMessage?chat_id={chatId}&text={text}";
        
        try {
            restTemplate.getForObject(url, String.class, botToken, chatId, message);
        } catch (Exception e) {
            System.err.println("--- TELEGRAM ERROR: " + e.getMessage());
        }
    }
}