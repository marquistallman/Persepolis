package com.persepolis.IA.model;

import jakarta.persistence.*;

@Entity
public class WallpaperItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String htmlContent; // El HTML extraído

    @Column(unique = true, length = 2048) // Aumentamos el tamaño para soportar URLs largas
    private String url; // Identificador único para no repetir resultados

    private int downloadCount = 0;
    private int redirectCount = 0;

    public WallpaperItem() {
    }

    public WallpaperItem(String htmlContent, String url) {
        this.htmlContent = htmlContent;
        this.url = url;
    }

    // Lógica de Puntos: Descargas valen 5x, Redirecciones 1x
    public int getTotalScore() {
        return (this.downloadCount * 5) + this.redirectCount;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHtmlContent() { return htmlContent; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }

    public int getRedirectCount() { return redirectCount; }
    public void setRedirectCount(int redirectCount) { this.redirectCount = redirectCount; }

    public void incrementDownloads() { this.downloadCount++; }
    public void incrementRedirects() { this.redirectCount++; }
}
