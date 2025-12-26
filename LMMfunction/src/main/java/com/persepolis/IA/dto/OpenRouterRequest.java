package com.persepolis.IA.dto;

import java.util.List;

public class OpenRouterRequest {

    private String model;
    private List<Message> messages;

    public OpenRouterRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public String getModel() {
        return model;
    }

    public List<Message> getMessages() {
        return messages;
    }
}

