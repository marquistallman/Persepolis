package com.persepolis.IA.Scraper.model;

import java.util.ArrayList;
import java.util.List;

public class WallpaperDTO {
    private String titulo;
    private String enlace;
    private String preview;
    private String resolucion;
    private String tipo;
    private List<String> tags = new ArrayList<>();
    private boolean hasVideo;
    private String videoUrl;
    private String fullImageUrl;

    // Getters y Setters
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    
    public String getEnlace() { return enlace; }
    public void setEnlace(String enlace) { this.enlace = enlace; }
    
    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
    
    public String getResolucion() { return resolucion; }
    public void setResolucion(String resolucion) { this.resolucion = resolucion; }
    
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void addTag(String tag) { this.tags.add(tag); }
    
    public boolean isHasVideo() { return hasVideo; }
    public void setHasVideo(boolean hasVideo) { this.hasVideo = hasVideo; }
    
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    
    public String getFullImageUrl() { return fullImageUrl; }
    public void setFullImageUrl(String fullImageUrl) { this.fullImageUrl = fullImageUrl; }

    @Override
    public String toString() {
        return "WallpaperDTO{titulo=" + titulo + ", tipo=" + tipo + "}";
    }
}