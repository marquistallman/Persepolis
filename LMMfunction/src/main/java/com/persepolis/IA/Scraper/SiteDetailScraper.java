package com.persepolis.IA.Scraper;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SiteDetailScraper {

    public Map<String, String> obtenerDetalles(String url, String sitio) {
        Map<String, String> detalles = new HashMap<>();
        
        try {
            // Conectamos al link original para extraer detalles profundos
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            if (sitio.equalsIgnoreCase("Wallpaper Waves")) {
                // Buscamos el video en la estructura específica de Wallpaper Waves
                // Selector basado en: <div class="player_responsive"> ... <video> ... <source src="...">
                Element videoSource = doc.selectFirst("div.player_responsive video source");
                
                if (videoSource != null) {
                    String videoUrl = videoSource.attr("src");
                    detalles.put("videoUrl", videoUrl);
                    detalles.put("tipoContenido", "video");
                }
            }
            
            // Aquí puedes agregar bloques 'else if' para otros sitios en el futuro
            
        } catch (Exception e) {
            System.err.println("Error obteniendo detalles de " + url + ": " + e.getMessage());
        }
        
        return detalles;
    }
}

