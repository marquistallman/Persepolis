package com.persepolis.IA.Scraper;

import com.persepolis.IA.Scraper.sitios.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



public class CScrap {
    
    private List<SitioBase> sitios; 
    
    public CScrap(){
    
        inicializarSitios(); 
    
}
    
    private void inicializarSitios(){
        
        sitios = new ArrayList<>();
        sitios.add(new WallpaperWaves());
        sitios.add(new MotionBackgrounds());
        sitios.add(new MoeWalls());
        sitios.add(new Wallhaven());
        sitios.add(new WallpaperFlare());
        sitios.add(new Artvee());
        sitios.add(new WallpaperAbyss());
        sitios.add(new Peakpx());
    }
    
 
    public void buscarEnMultiplesSitios(String busqueda, JTable tablaResultados) {
   
        DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"#", "Sitio", "Título", "Enlace", "Info"}, 
            0
        );
        tablaResultados.setModel(modelo);
        
        int contador = 1;
        
        for (SitioBase sitio : sitios) {
            try {
                String url = sitio.generarUrlBusqueda(busqueda);
                System.out.println("Buscando en " + sitio.getNombre() + ": " + url);
                
                Document doc = Jsoup.connect(url)
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
                    .ignoreHttpErrors(true)
                    .get();
                
            
                Elements elementos = sitio.obtenerElementosRelevantes(doc);
                System.out.println("  Encontrados: " + elementos.size());
                
          
                for (Element elemento : elementos) {
                    Map<String, String> datos = sitio.extraerDatos(elemento);
                    
                    modelo.addRow(new Object[]{
                        contador++,
                        sitio.getNombre(),
                        datos.get("titulo"),
                        datos.get("enlace"),
                        datos.get("resolucion")
                    });
                }
                
                Thread.sleep(2000);
                
            } catch (Exception e) {
                modelo.addRow(new Object[]{
                    contador++, 
                    sitio.getNombre(), 
                    "ERROR", 
                    e.getMessage(), 
                    ""
                });
            }
        }
    }

    /**
     * Método optimizado para uso Web/API que retorna una lista de datos.
     */
    public List<Map<String, String>> buscarWeb(String busqueda) {
        List<Map<String, String>> resultados = new ArrayList<>();
        
        for (SitioBase sitio : sitios) {
            try {
                String url = sitio.generarUrlBusqueda(busqueda);
                System.out.println("--- Scraping site: " + sitio.getNombre() + " -> " + url);

                org.jsoup.Connection connection = Jsoup.connect(url)
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
                        .timeout(20000)
                        .ignoreHttpErrors(true)
                        .followRedirects(true);

                org.jsoup.Connection.Response response = connection.execute();
                System.out.println("  HTTP status for " + sitio.getNombre() + ": " + response.statusCode());

                if (response.statusCode() >= 400) {
                    System.out.println("  Skipping " + sitio.getNombre() + " due to HTTP error: " + response.statusCode());
                    continue;
                }

                Document doc = response.parse();
                if (doc == null) {
                    System.out.println("  Document parsing returned null for " + sitio.getNombre());
                    continue;
                }

                Elements elementos = sitio.obtenerElementosRelevantes(doc);
                System.out.println("  Found " + elementos.size() + " elements on " + sitio.getNombre());

                for (Element elemento : elementos) {
                    try {
                        Map<String, String> datos = sitio.extraerDatos(elemento);
                        datos.put("sitio", sitio.getNombre()); // Agregamos el origen
                        resultados.add(datos);
                    } catch (Exception ex) {
                        System.out.println("  Error extracting data from element on " + sitio.getNombre() + ": " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error scraping web " + sitio.getNombre() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return resultados;
    }

    public Map<String, String> obtenerDetalles(String url, String nombreSitio) {
        for (SitioBase sitio : sitios) {
            if (sitio.getNombre().equalsIgnoreCase(nombreSitio)) {
                return sitio.obtenerDetalles(url);
            }
        }
        return new HashMap<>();
    }
}
    
    
