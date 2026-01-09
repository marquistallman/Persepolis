package com.persepolis.IA.Scraper.sitios;

import com.persepolis.IA.Scraper.model.WallpaperDTO;
import java.net.URLEncoder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Artvee extends SitioBase {
    @Override
    public String getNombre() { return "Artvee"; }

    @Override
    public String generarUrlBusqueda(String query, int page) throws Exception {
        if (page > 1) return "https://artvee.com/main/page/" + page + "/?s=" + URLEncoder.encode(query, "UTF-8");
        return "https://artvee.com/main/?s=" + URLEncoder.encode(query, "UTF-8");
    }

    @Override
    public String getUrlPopulares(int page) {
        if (page > 1) return "https://artvee.com/page/" + page + "/";
        return "https://artvee.com/";
    }

    @Override
    protected Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "div.product-grid-item");
    }

    @Override
    public WallpaperDTO extraerDatos(Element elemento) {
        WallpaperDTO dto = new WallpaperDTO();
        Element titleElement = elemento.selectFirst("h3.product-title a");
        Element img = elemento.selectFirst(".product-element-top img");
        String resolucion = "";
        Element dataDiv = elemento.selectFirst(".product-element-top");
        if (dataDiv != null && dataDiv.hasAttr("data-sk")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"hdlimagesize\":\"([^\"]+)\"").matcher(dataDiv.attr("data-sk"));
            if (m.find()) resolucion = m.group(1);
        }
        dto.setTitulo((titleElement != null) ? titleElement.text().trim() : "Artvee Artwork");
        dto.setEnlace((titleElement != null) ? titleElement.attr("href") : "");
        dto.setPreview((img != null) ? img.attr("src") : "");
        dto.setResolucion(resolucion);
        
        // Extracción de Tags (Categorías)
        Elements catTags = elemento.select(".woodmart-product-cats a");
        for (Element tag : catTags) {
            dto.addTag(tag.text());
        }
        
        // Extracción de Artista (lo añadimos como tag para facilitar la búsqueda)
        Element artist = elemento.selectFirst(".woodmart-product-brands-links a");
        if (artist != null) {
            dto.addTag(artist.text());
        }
        
        dto.setTipo("Artvee");
        return dto;
    }

    @Override
    public WallpaperDTO obtenerDetalles(String url) {
        WallpaperDTO detalles = new WallpaperDTO();
        try {
            Document doc = crearConexion(url).get();
            Element img = doc.selectFirst(".product-image-summary img");
            if (img != null) {
                detalles.setFullImageUrl(img.attr("src"));
            }

            
        } catch (Exception e) {
            System.err.println("Error obteniendo detalles de Artvee: " + e.getMessage());
        }
        return detalles;
    }
}