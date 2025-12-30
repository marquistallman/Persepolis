package com.persepolis.IA;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class IaApplication {

	@Value("${app.frontend.autostart:true}")
	private boolean autoStartFrontend;

	public static void main(String[] args) {
		SpringApplication.run(IaApplication.class, args);
	}

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager("default");
	}

	@Bean
	public CommandLineRunner init() {
		return args -> {
			System.out.println("--- Aplicación iniciada ---");
			System.out.println("Puerto: 8080");
			System.out.println("URL Principal:    http://localhost:8080/");
			System.out.println("--- Rutas de Prueba (TestController) ---");
			System.out.println("Verificar OpenRouter: http://localhost:8080/test/openrouter");
			System.out.println("Chat Interactivo: http://localhost:8080/test/chat?message=Hola");
			System.out.println("Historial Chat:   http://localhost:8080/test/chat/history");
			System.out.println("--- Rutas de Prueba (ScraperController) ---");
			System.out.println("Scraper:          http://localhost:8080/scraper");

			if (autoStartFrontend) {
				openBrowser("http://localhost:8080");
			} else {
				System.out.println("--- Modo VPS/Producción: Frontend autostart desactivado ---");
			}
		};
	}

	private void openBrowser(String url) {
		try {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win")) {
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
				System.out.println("--- Navegador abierto en: " + url + " ---");
			}
		} catch (Exception e) {
			System.err.println("--- No se pudo abrir el navegador: " + e.getMessage() + " ---");
		}
	}
}
