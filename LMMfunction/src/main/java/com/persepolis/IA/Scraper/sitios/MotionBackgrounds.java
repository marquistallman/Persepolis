package com.persepolis.IA.Scraper.sitios;

import com.persepolis.IA.Scraper.model.WallpaperDTO;
import java.net.URLEncoder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MotionBackgrounds extends SitioBase {
    @Override
    public String getNombre() { return "Motion Backgrounds"; }

    @Override
    public String generarUrlBusqueda(String query, int page) throws Exception {
        if (page > 1) return "https://motionbgs.com/page/" + page + "/?s=" + URLEncoder.encode(query, "UTF-8");
        return "https://motionbgs.com/?s=" + URLEncoder.encode(query, "UTF-8");
    }

    @Override
    public String getUrlPopulares(int page) {
        if (page > 1) return "https://motionbgs.com/page/" + page + "/";
        return "https://motionbgs.com/";
    }

    @Override
    protected Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "a[title$='live wallpaper download']", "figure a[href^='/']", ".grid-item a");
    }

    @Override
    public WallpaperDTO extraerDatos(Element elemento) {
        WallpaperDTO dto = new WallpaperDTO();
        Element ttlSpan = elemento.selectFirst("span.ttl");
        String titulo = (ttlSpan != null) ? ttlSpan.text() : elemento.attr("title");
        if (titulo.contains(" live wallpaper download")) titulo = titulo.replace(" live wallpaper download", "");
        
        String enlace = elemento.attr("href");
        if (!enlace.startsWith("http")) enlace = "https://motionbgs.com" + enlace;
        
        Element img = elemento.selectFirst("img");
        String preview = (img != null) ? img.attr("src") : "";
        if (!preview.startsWith("http") && !preview.isEmpty()) preview = "https://motionbgs.com" + preview;
        
        Element frmSpan = elemento.selectFirst("span.frm");
        dto.setTitulo(titulo.trim());
        dto.setEnlace(enlace);
        dto.setPreview(preview);
        dto.setResolucion((frmSpan != null) ? frmSpan.text() : "");
        dto.setTipo("Motion Backgrounds");
        dto.setHasVideo(true);
        return dto;
    }

    @Override
    public WallpaperDTO obtenerDetalles(String url) {
        WallpaperDTO detalles = new WallpaperDTO();
        try {
            Document doc = crearConexion(url).get();

            Element video = doc.selectFirst("video");
            if (video != null) {
                String poster = video.attr("poster");
                if (poster != null && !poster.isEmpty()) {
                    if (!poster.startsWith("http")) {
                        poster = "https://motionbgs.com" + poster;
                    }
                    detalles.setPreview(poster);
                    detalles.setFullImageUrl(poster); // Usar poster como imagen para paleta
                }
                
                Element videoSource = video.selectFirst("source[type='video/mp4']");
                if (videoSource != null) {
                    String videoUrl = videoSource.attr("src");
                    if (!videoUrl.startsWith("http")) {
                        videoUrl = "https://motionbgs.com" + videoUrl;
                    }
                    detalles.setVideoUrl(videoUrl);
                }
            }
            
            // Si no hay poster, buscar una imagen
            if (detalles.getPreview() == null || detalles.getPreview().isEmpty()) {
                Element img = doc.selectFirst("img");
                if (img != null) {
                    String imgSrc = img.attr("src");
                    if (!imgSrc.startsWith("http") && !imgSrc.isEmpty()) {
                        imgSrc = "https://motionbgs.com" + imgSrc;
                    }
                    detalles.setPreview(imgSrc);
                    detalles.setFullImageUrl(imgSrc);
                }
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo detalles de MotionBackgrounds: " + e.getMessage());
        }
        return detalles;
    }
}