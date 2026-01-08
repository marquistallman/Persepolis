package com.persepolis.IA.Scraper.sitios;

import com.persepolis.IA.Scraper.model.WallpaperDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public abstract class SitioBase {
    
    // Rotación de User-Agents para evitar bloqueos
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0"
    };
    
    public abstract String getNombre();
    public abstract String generarUrlBusqueda(String query, int page) throws Exception;
    public abstract String getUrlPopulares(int page); // Nuevo contrato para la página principal
    protected abstract Elements obtenerElementosRelevantes(Document doc);
    public abstract WallpaperDTO extraerDatos(Element elemento);
    
    // Método principal que orquesta la búsqueda y gestiona la memoria
    public List<WallpaperDTO> buscar(String query, int page) {
        List<WallpaperDTO> resultados = new ArrayList<>();
        try {
            String url;
            if ("popular".equalsIgnoreCase(query)) {
                url = getUrlPopulares(page);
            } else {
                url = generarUrlBusqueda(query, page);
            }
            // El Document solo vive dentro de este bloque try
            Document doc = crearConexion(url).get();
            Elements elementos = obtenerElementosRelevantes(doc);
            
            for (Element elemento : elementos) {
                try {
                    WallpaperDTO dto = extraerDatos(elemento);
                    if (dto != null) {
                        resultados.add(dto);
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando elemento en " + getNombre() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error en búsqueda " + getNombre() + ": " + e.getMessage());
        }
        return resultados;
    }

    public WallpaperDTO obtenerDetalles(String url) {
        return new WallpaperDTO();
    }

    protected Connection crearConexion(String url) {
        return org.jsoup.Jsoup.connect(url)
                .userAgent(USER_AGENTS[new Random().nextInt(USER_AGENTS.length)])
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