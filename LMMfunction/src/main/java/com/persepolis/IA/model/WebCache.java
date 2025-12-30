package com.persepolis.IA.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "web_cache")
public class WebCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mantener compatibilidad con esquemas previos que usan CACHE_KEY y/o URL_OR_QUERY
    @Column(name = "CACHE_KEY", unique = true, nullable = false)
    private String cacheKey; // Clave única: URL o término de búsqueda

    @Column(name = "URL_OR_QUERY")
    private String urlOrQuery; // Duplicado para compatibilidad con esquemas antiguos

    @Lob // Large Object para guardar HTML o JSON extenso
    @Column(columnDefinition = "CLOB") // Asegura compatibilidad con H2 para textos largos
    private String content;

    private LocalDateTime lastUpdated;

    public WebCache() {
    }

    public WebCache(String cacheKey, String content, LocalDateTime lastUpdated) {
        this.cacheKey = cacheKey;
        this.urlOrQuery = cacheKey;
        this.content = content;
        this.lastUpdated = lastUpdated;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; this.urlOrQuery = cacheKey; }

    public String getUrlOrQuery() { return urlOrQuery; }
    public void setUrlOrQuery(String urlOrQuery) { this.urlOrQuery = urlOrQuery; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}