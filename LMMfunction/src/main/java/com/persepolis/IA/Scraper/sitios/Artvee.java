package com.persepolis.IA.Scraper.sitios;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Artvee extends SitioBase {
    @Override
    public String getNombre() { return "Artvee"; }

    @Override
    public String generarUrlBusqueda(String query) throws Exception {
        return "https://artvee.com/main/?s=" + URLEncoder.encode(query, "UTF-8");
    }

    @Override
    public Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "div.product-grid-item");
    }

    @Override
    public Map<String, String> extraerDatos(Element elemento) {
        Map<String, String> datos = new HashMap<>();
        Element titleElement = elemento.selectFirst("h3.product-title a");
        Element img = elemento.selectFirst(".product-element-top img");
        String resolucion = "";
        Element dataDiv = elemento.selectFirst(".product-element-top");
        if (dataDiv != null && dataDiv.hasAttr("data-sk")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"hdlimagesize\":\"([^\"]+)\"").matcher(dataDiv.attr("data-sk"));
            if (m.find()) resolucion = m.group(1);
        }
        datos.put("titulo", (titleElement != null) ? titleElement.text().trim() : "Artvee Artwork");
        datos.put("enlace", (titleElement != null) ? titleElement.attr("href") : "");
        datos.put("preview", (img != null) ? img.attr("src") : "");
        datos.put("resolucion", resolucion);
        datos.put("tipo", "Artvee");
        return datos;
    }
}