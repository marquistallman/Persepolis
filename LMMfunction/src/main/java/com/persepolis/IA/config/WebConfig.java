package com.persepolis.IA.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Servir archivos estáticos desde /static/Pages/ bajo /pages/
        registry.addResourceHandler("/pages/**")
                .addResourceLocations("classpath:/static/Pages/")
                .setCachePeriod(3600); // Cache por 1 hora
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirige la raíz "/" al index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        
        // View controllers para páginas individuales
        registry.addViewController("/pages/homepage").setViewName("forward:/Pages/homepage.html");
        registry.addViewController("/pages/browse").setViewName("forward:/Pages/browse.html");
        registry.addViewController("/pages/search_results").setViewName("forward:/Pages/search_results.html");
        registry.addViewController("/pages/chat").setViewName("forward:/Pages/chat.html");
        registry.addViewController("/pages/about").setViewName("forward:/Pages/about.html");
        registry.addViewController("/pages/contact").setViewName("forward:/Pages/contact.html");
        registry.addViewController("/pages/faq").setViewName("forward:/Pages/faq.html");
        registry.addViewController("/pages/premium").setViewName("forward:/Pages/premium.html");
        registry.addViewController("/pages/privacy").setViewName("forward:/Pages/privacy.html");
        registry.addViewController("/pages/terms").setViewName("forward:/Pages/terms.html");
    }
}