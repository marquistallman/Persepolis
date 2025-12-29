package com.persepolis.IA.Scraper.sitios;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.Connection;
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

    protected Connection crearConexion(String url) {
        return org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .referrer("https://www.google.com/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "cross-site")
                .header("Upgrade-Insecure-Requests", "1")
                .timeout(15000)
                .ignoreHttpErrors(true);
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