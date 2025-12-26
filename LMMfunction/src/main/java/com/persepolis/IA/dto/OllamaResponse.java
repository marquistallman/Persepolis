package com.persepolis.IA.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Ignoramos cualquier campo extra que Ollama pueda enviar y que no nos interese
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaResponse(
    String response // El campo que contiene el texto de la respuesta
) {
}