package com.persepolis.IA.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// La respuesta de /api/chat contiene un objeto "message"
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatResponse(Message message) {
}