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
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
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
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0")
                        .timeout(10000)
                        .get();

                Elements elementos = sitio.obtenerElementosRelevantes(doc);

                for (Element elemento : elementos) {
                    Map<String, String> datos = sitio.extraerDatos(elemento);
                    datos.put("sitio", sitio.getNombre()); // Agregamos el origen
                    resultados.add(datos);
                }
            } catch (Exception e) {
                System.out.println("Error scraping web " + sitio.getNombre() + ": " + e.getMessage());
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
    
    
