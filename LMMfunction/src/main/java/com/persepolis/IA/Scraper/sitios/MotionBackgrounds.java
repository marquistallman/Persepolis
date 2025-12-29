package com.persepolis.IA.Scraper.sitios;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MotionBackgrounds extends SitioBase {
    @Override
    public String getNombre() { return "Motion Backgrounds"; }

    @Override
    public String generarUrlBusqueda(String query) {
        return "https://motionbgs.com/tag:" + query.toLowerCase().replace(" ", "-") + "/";
    }

    @Override
    public Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "a[title$='live wallpaper download']", "figure a[href^='/']", ".grid-item a");
    }

    @Override
    public Map<String, String> extraerDatos(Element elemento) {
        Map<String, String> datos = new HashMap<>();
        Element ttlSpan = elemento.selectFirst("span.ttl");
        String titulo = (ttlSpan != null) ? ttlSpan.text() : elemento.attr("title");
        if (titulo.contains(" live wallpaper download")) titulo = titulo.replace(" live wallpaper download", "");
        
        String enlace = elemento.attr("href");
        if (!enlace.startsWith("http")) enlace = "https://motionbgs.com" + enlace;
        
        Element img = elemento.selectFirst("img");
        String preview = (img != null) ? img.attr("src") : "";
        if (!preview.startsWith("http") && !preview.isEmpty()) preview = "https://motionbgs.com" + preview;
        
        Element frmSpan = elemento.selectFirst("span.frm");
        datos.put("titulo", titulo.trim());
        datos.put("enlace", enlace);
        datos.put("preview", preview);
        datos.put("resolucion", (frmSpan != null) ? frmSpan.text() : "");
        datos.put("tipo", "Motion Backgrounds");
        datos.put("hasVideo", "true");
        return datos;
    }

    @Override
    public Map<String, String> obtenerDetalles(String url) {
        Map<String, String> detalles = new HashMap<>();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            Element videoSource = doc.selectFirst("video source[type='video/mp4']");
            if (videoSource != null) {
                String videoUrl = videoSource.attr("src");
                if (!videoUrl.startsWith("http")) {
                    videoUrl = "https://motionbgs.com" + videoUrl;
                }
                detalles.put("videoUrl", videoUrl);
                detalles.put("tipoContenido", "video");
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo detalles de MotionBackgrounds: " + e.getMessage());
        }
        return detalles;
    }
}