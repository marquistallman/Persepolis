package com.persepolis.IA.Scraper.sitios;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public abstract class SitioBase {
    
    public abstract String getNombre();
    public abstract String generarUrlBusqueda(String query) throws Exception;
    public abstract Elements obtenerElementosRelevantes(Document doc);
    public abstract Map<String, String> extraerDatos(Element elemento);
    
    // Implementación por defecto para sitios que no requieren scraping profundo
    public Map<String, String> obtenerDetalles(String url) {
        return new HashMap<>();
    }

    protected Elements buscarPorSelectores(Document doc, String... selectores) {
        Elements resultados = new Elements();
        for (String selector : selectores) {
            Elements encontrados = doc.select(selector);
            if (!encontrados.isEmpty()) {
                resultados.addAll(encontrados);
                break;
            }
        }
        return resultados;
    }
}