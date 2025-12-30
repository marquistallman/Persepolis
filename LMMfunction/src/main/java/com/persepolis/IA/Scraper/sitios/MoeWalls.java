package com.persepolis.IA.Scraper.sitios;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MoeWalls extends SitioBase {
    @Override
    public String getNombre() { return "Moe Walls"; }

    @Override
    public String generarUrlBusqueda(String query) throws Exception {
        return "https://moewalls.com/?s=" + URLEncoder.encode(query, "UTF-8");
    }

    @Override
    public Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "article.post");
    }

    @Override
    public Map<String, String> extraerDatos(Element elemento) {
        Map<String, String> datos = new HashMap<>();
        Element titleElement = elemento.selectFirst("h3.entry-title a");
        datos.put("titulo", (titleElement != null) ? titleElement.text() : "Moe Walls Wallpaper");
        datos.put("enlace", (titleElement != null) ? titleElement.attr("href") : "");
        
        Element img = elemento.selectFirst(".entry-featured-media img");
        datos.put("preview", (img != null) ? img.attr("src") : "");
        
        Element resElement = elemento.selectFirst(".entry-resolutions a");
        datos.put("resolucion", (resElement != null) ? resElement.text() : "");
        
        datos.put("tipo", "Moe Walls");
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

            // Buscamos el video y usamos abs:src para obtener la URL completa (incluyendo dominio)
            Element video = doc.selectFirst("video");
            if (video != null) {
                String src = video.attr("abs:src");
                if (src.isEmpty()) {
                    Element source = video.selectFirst("source");
                    if (source != null) src = source.attr("abs:src");
                }
                
                if (!src.isEmpty()) {
                    detalles.put("videoUrl", src);
                    detalles.put("tipoContenido", "video");
                }
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo detalles de Moewalls: " + e.getMessage());
        }
        return detalles;
    }
}