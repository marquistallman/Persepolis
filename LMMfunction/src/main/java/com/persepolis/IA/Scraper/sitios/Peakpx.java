package com.persepolis.IA.Scraper.sitios;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Peakpx extends SitioBase {
    @Override
    public String getNombre() { return "Peakpx"; }

    @Override
    public String generarUrlBusqueda(String query) throws Exception {
        return "https://www.peakpx.com/en/search?q=" + URLEncoder.encode(query, "UTF-8");
    }

    @Override
    public Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "#search-list li");
    }

    @Override
    public Map<String, String> extraerDatos(Element elemento) {
        Map<String, String> datos = new HashMap<>();
        Element link = elemento.selectFirst("a[itemprop='url']");
        String enlace = (link != null) ? link.attr("href") : "";
        if (!enlace.startsWith("http") && !enlace.isEmpty()) enlace = "https://www.peakpx.com" + enlace;
        Element img = elemento.selectFirst("img");
        String preview = (img != null) ? (img.hasAttr("data-src") ? img.attr("data-src") : img.attr("src")) : "";
        Element caption = elemento.selectFirst("figcaption");
        Element resSpan = elemento.selectFirst("span.res");
        datos.put("titulo", (caption != null) ? caption.text() : "Peakpx Wallpaper");
        datos.put("enlace", enlace);
        datos.put("preview", preview);
        datos.put("resolucion", (resSpan != null) ? resSpan.text() : "");
        datos.put("tipo", "Peakpx");
        return datos;
    }
}