package com.persepolis.IA.Scraper.sitios;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
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
}