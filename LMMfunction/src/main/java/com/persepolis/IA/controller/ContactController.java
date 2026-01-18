package com.persepolis.IA.controller;

import com.persepolis.IA.model.ContactMessage;
import com.persepolis.IA.repository.ContactRepository;
import com.persepolis.IA.services.TelegramService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactRepository contactRepository;
    private final TelegramService telegramService;

    public ContactController(ContactRepository contactRepository, TelegramService telegramService) {
        this.contactRepository = contactRepository;
        this.telegramService = telegramService;
    }

    @PostMapping
    public Map<String, String> submitContact(@RequestBody ContactMessage message) {
        message.setTimestamp(LocalDateTime.now());
        contactRepository.save(message);

        String text = "📩 *Nuevo Mensaje de Contacto*\n\n" +
                      "👤 *De:* " + message.getFirstName() + " " + message.getLastName() + "\n" +
                      "📧 *Email:* " + message.getEmail() + "\n" +
                      "📝 *Asunto:* " + message.getSubject() + "\n\n" +
                      "💬 *Mensaje:* \n" + message.getMessage();
        
        // Enviar en segundo plano para no bloquear la respuesta al usuario
        new Thread(() -> telegramService.sendNotification(text)).start();

        return Map.of("status", "success", "message", "Mensaje recibido correctamente");
    }
}