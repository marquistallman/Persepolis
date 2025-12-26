package com.persepolis.IA.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// Ignoramos los campos que no nos interesan de la respuesta principal
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenRouterResponse {

    private List<Choice> choices;

    // Ignoramos los campos que no nos interesan de cada "choice"
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {}

    public List<Choice> choices() { return choices; }
}
