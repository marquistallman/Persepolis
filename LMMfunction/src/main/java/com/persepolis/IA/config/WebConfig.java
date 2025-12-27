package com.persepolis.IA.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import java.io.File;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Buscamos la ruta absoluta correcta del frontend para evitar errores de ruta relativa
        String[] possiblePaths = { "../front/Front", "front/Front", "../front", "front" };
        String resourceLocation = null;

        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory() && new File(dir, "package.json").exists()) {
                // Usamos toURI() para generar una URL válida de archivo automáticamente
                resourceLocation = dir.toURI().toString();
                break;
            }
        }
        
        if (resourceLocation == null) {
            resourceLocation = "file:../front/Front/";
        } else if (!resourceLocation.endsWith("/")) {
            // Spring requiere que las ubicaciones de recursos terminen en /
            resourceLocation += "/";
        }

        System.out.println("--- WebConfig: Sirviendo archivos estáticos desde: " + resourceLocation + " ---");

        registry.addResourceHandler("/**")
                .addResourceLocations(resourceLocation);
    }
}