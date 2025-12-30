package com.persepolis.IA.Scraper.sitios;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Wallhaven extends SitioBase {
    @Override
    public String getNombre() { return "Wallhaven"; }

    @Override
    public String generarUrlBusqueda(String query) throws Exception {
        return "https://wallhaven.cc/search?q=" + URLEncoder.encode(query, "UTF-8");
    }

    @Override
    public Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "figure.thumb");
    }

    @Override
    public Map<String, String> extraerDatos(Element elemento) {
        Map<String, String> datos = new HashMap<>();
        Element linkElement = elemento.selectFirst("a.preview");
        String enlace = (linkElement != null) ? linkElement.attr("href") : "";
        if (!enlace.startsWith("http") && !enlace.isEmpty()) enlace = "https://wallhaven.cc" + (enlace.startsWith("/") ? enlace : "/" + enlace);

        String wallpaperId = elemento.attr("data-wallpaper-id");
        Element img = elemento.selectFirst("img");
        String preview = (img != null) ? img.attr("data-src") : "";
        if (preview.isEmpty() && img != null) preview = img.attr("src");
        
        Element resInfo = elemento.selectFirst(".wall-res");
        datos.put("titulo", "Wallhaven " + wallpaperId);
        datos.put("enlace", enlace);
        datos.put("preview", preview);
        datos.put("resolucion", (resInfo != null) ? resInfo.text() : "");
        datos.put("tipo", "Wallhaven");
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
            
            Element img = doc.selectFirst("img#wallpaper");
            if (img != null) {
                detalles.put("fullImageUrl", img.attr("src"));
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo detalles de Wallhaven: " + e.getMessage());
        }
        return detalles;
    }
}