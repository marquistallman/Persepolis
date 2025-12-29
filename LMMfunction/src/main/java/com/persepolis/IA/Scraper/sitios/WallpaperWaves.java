package com.persepolis.IA.Scraper.sitios;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WallpaperWaves extends SitioBase {
    @Override
    public String getNombre() { return "Wallpaper Waves"; }

    @Override
    public String generarUrlBusqueda(String query) throws Exception {
        return "https://wallpaperwaves.com/?s=" + URLEncoder.encode(query, "UTF-8");
    }

    @Override
    public Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "article.jeg_post");
    }

    @Override
    public Map<String, String> extraerDatos(Element elemento) {
        Map<String, String> datos = new HashMap<>();
        Element titleElement = elemento.selectFirst("h3.jeg_post_title a");
        datos.put("titulo", (titleElement != null) ? titleElement.text() : "Wallpaper Waves");
        datos.put("enlace", (titleElement != null) ? titleElement.attr("href") : "");
        Element img = elemento.selectFirst(".jeg_thumb img");
        datos.put("preview", (img != null) ? img.attr("src") : "");
        datos.put("tipo", "WallpaperWaves");
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

            Element videoSource = doc.selectFirst("div.player_responsive video source");
            if (videoSource != null) {
                detalles.put("videoUrl", videoSource.attr("src"));
                detalles.put("tipoContenido", "video");
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo detalles de WallpaperWaves: " + e.getMessage());
        }
        return detalles;
    }
}