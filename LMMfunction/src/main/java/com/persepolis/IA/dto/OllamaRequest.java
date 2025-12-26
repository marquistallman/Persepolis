package com.persepolis.IA.dto;

public class OllamaRequest {

    private String model;
    private String prompt;
    private int max_tokens;
    private boolean stream;

    public OllamaRequest(String model, String prompt, int max_tokens, boolean stream) {
        this.model = model;
        this.prompt = prompt;
        this.max_tokens = max_tokens;
        this.stream = stream;
    }

    public String getModel() {
        return model;
    }

    public String getPrompt() {
        return prompt;
    }

    public int getMax_tokens() {
        return max_tokens;
    }

    public boolean isStream() {
        return stream;
    }
}
