package com.persepolis.IA;

import java.util.List;
import java.util.Map;

public class ChatData {
    private DecisionTree decisionTree;
    private Map<String, String> slangMapping;
    private List<String> siteKeywords;
    private List<String> stopWords;
    private List<String> greetingPatterns;
    private List<String> standardRequestKeywords;
    private List<String> unsureKeywords;
    private Map<String, String> responses;

    // Getters y Setters
    public DecisionTree getDecisionTree() {
        return decisionTree;
    }

    public void setDecisionTree(DecisionTree decisionTree) {
        this.decisionTree = decisionTree;
    }

    public Map<String, String> getSlangMapping() {
        return slangMapping;
    }

    public void setSlangMapping(Map<String, String> slangMapping) {
        this.slangMapping = slangMapping;
    }

    public List<String> getSiteKeywords() {
        return siteKeywords;
    }

    public void setSiteKeywords(List<String> siteKeywords) {
        this.siteKeywords = siteKeywords;
    }

    public List<String> getStopWords() {
        return stopWords;
    }

    public void setStopWords(List<String> stopWords) {
        this.stopWords = stopWords;
    }

    public List<String> getGreetingPatterns() {
        return greetingPatterns;
    }

    public void setGreetingPatterns(List<String> greetingPatterns) {
        this.greetingPatterns = greetingPatterns;
    }

    public List<String> getStandardRequestKeywords() {
        return standardRequestKeywords;
    }

    public void setStandardRequestKeywords(List<String> standardRequestKeywords) {
        this.standardRequestKeywords = standardRequestKeywords;
    }

    public List<String> getUnsureKeywords() {
        return unsureKeywords;
    }

    public void setUnsureKeywords(List<String> unsureKeywords) {
        this.unsureKeywords = unsureKeywords;
    }

    public Map<String, String> getResponses() {
        return responses;
    }

    public void setResponses(Map<String, String> responses) {
        this.responses = responses;
    }
    
    public String getResponse(String key) {
        return responses != null ? responses.getOrDefault(key, "") : "";
    }
    
    public String getResponse(String key, Object... args) {
        return String.format(getResponse(key), args);
    }

    // Clases internas para el Árbol de Decisión
    public static class DecisionTree {
        private Node root;
        private Map<String, Node> nodes;

        public Node getRoot() { return root; }
        public void setRoot(Node root) { this.root = root; }
        public Map<String, Node> getNodes() { return nodes; }
        public void setNodes(Map<String, Node> nodes) { this.nodes = nodes; }
    }

    public static class Node {
        private String id;
        private String text;
        private String type; // "choice" (default) or "open"
        private Map<String, Option> options;
        private String next; // Para flujo lineal o preguntas abiertas

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Option> getOptions() { return options; }
        public void setOptions(Map<String, Option> options) { this.options = options; }
        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }
    }

    public static class Option {
        private String keyword;
        private String next;

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }
    }
}