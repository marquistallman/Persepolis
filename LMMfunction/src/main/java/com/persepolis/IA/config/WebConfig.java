package com.persepolis.IA.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // Configuración vacía por ahora.
    // La redirección de la raíz "/" se maneja explícitamente en HomeController.java
}