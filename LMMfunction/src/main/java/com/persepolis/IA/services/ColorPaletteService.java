package com.persepolis.IA.services;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class ColorPaletteService {

    /**
     * Extrae una paleta de colores de una imagen dada por URL.
     * Utiliza K-Means nativo para evitar dependencias externas como OpenCV.
     */
    public List<String> extractPalette(String imageUrl, int k) {
        System.out.println("--- PALETTE SERVICE: Procesando URL: " + imageUrl);
        try {
            // Sanitizar URL (espacios) para URI.create sin usar el obsoleto new URL()
            URL url;
            try {
                url = URI.create(imageUrl.replace(" ", "%20")).toURL();
            } catch (Exception e) {
                System.err.println("--- PALETTE ERROR: URL malformada o inválida: " + imageUrl);
                return new ArrayList<>();
            }
            
            // Usar HttpURLConnection para añadir User-Agent y evitar bloqueos (403 Forbidden)
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
            connection.setRequestProperty("Referer", "https://www.google.com/");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            String contentType = connection.getContentType();
            if (contentType != null && contentType.contains("text/html")) {
                System.err.println("--- PALETTE SKIP: Se ignoró URL porque es HTML: " + imageUrl);
                return new ArrayList<>();
            }
            
            BufferedImage original = ImageIO.read(connection.getInputStream());
            if (original == null) {
                // ImageIO devuelve null si el formato no es soportado (ej. WebP en JDKs antiguos sin plugins)
                System.err.println("--- PALETTE ERROR: ImageIO devolvió NULL (¿Es un HTML en vez de imagen?): " + imageUrl);
                
                // Intentar formatos alternativos (jpg, jpeg, png) si es .webp
                if (imageUrl.toLowerCase().endsWith(".webp")) {
                    System.out.println("--- PALETTE: Intentando formatos alternativos para WebP");
                    String[] extensions = {".jpg", ".jpeg", ".png"};
                    for (String ext : extensions) {
                        String altUrl = imageUrl.substring(0, imageUrl.length() - 5) + ext; // Reemplazar .webp
                        System.out.println("--- PALETTE: Probando URL alternativa: " + altUrl);
                        try {
                            URL altURL = URI.create(altUrl.replace(" ", "%20")).toURL();
                            HttpURLConnection altConn = (HttpURLConnection) altURL.openConnection();
                            altConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                            altConn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
                            altConn.setRequestProperty("Referer", "https://www.google.com/");
                            altConn.setConnectTimeout(10000);
                            altConn.setReadTimeout(10000);
                            
                            String altContentType = altConn.getContentType();
                            if (altContentType != null && altContentType.contains("text/html")) {
                                System.out.println("--- PALETTE: Saltando URL alternativa porque es HTML: " + altUrl);
                                continue;
                            }
                            
                            original = ImageIO.read(altConn.getInputStream());
                            if (original != null) {
                                System.out.println("--- PALETTE SUCCESS: Usando formato alternativo: " + altUrl);
                                break;
                            } else {
                                System.out.println("--- PALETTE: Formato alternativo también falló: " + altUrl);
                            }
                        } catch (Exception e2) {
                            System.out.println("--- PALETTE: Excepción en alternativa " + altUrl + ": " + e2.getMessage());
                        }
                    }
                }
                
                if (original == null) {
                    System.out.println("--- PALETTE: No se pudo cargar imagen en ningún formato");
                    return new ArrayList<>();
                }
            }

            // 1. Redimensionar para velocidad (max 100px)
            BufferedImage image = resize(original, 100);

            // 2. Obtener píxeles
            List<int[]> pixels = new ArrayList<>();
            int width = image.getWidth();
            int height = image.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    pixels.add(new int[]{
                        (rgb >> 16) & 0xFF, // R
                        (rgb >> 8) & 0xFF,  // G
                        (rgb) & 0xFF        // B
                    });
                }
            }

            // 3. K-Means simple
            List<int[]> centroids = calculateKMeans(pixels, k);

            // 4. Convertir a Hex
            return centroids.stream()
                    .map(c -> String.format("#%02X%02X%02X", c[0], c[1], c[2]))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("--- PALETTE EXCEPTION: " + e.getMessage() + " procesando " + imageUrl);
            return new ArrayList<>();
        }
    }

    private BufferedImage resize(BufferedImage src, int targetSize) {
        if (src.getWidth() <= targetSize && src.getHeight() <= targetSize) return src;
        int width = src.getWidth();
        int height = src.getHeight();
        double ratio = (double) width / height;
        if (width > height) {
            width = targetSize;
            height = (int) (width / ratio);
        } else {
            height = targetSize;
            width = (int) (height * ratio);
        }
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(src, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    private List<int[]> calculateKMeans(List<int[]> pixels, int k) {
        Random random = new Random();
        List<int[]> centroids = new ArrayList<>();
        for (int i = 0; i < k; i++) centroids.add(pixels.get(random.nextInt(pixels.size())));

        for (int iter = 0; iter < 10; iter++) { // Max 10 iteraciones
            List<List<int[]>> clusters = new ArrayList<>();
            for (int i = 0; i < k; i++) clusters.add(new ArrayList<>());

            for (int[] pixel : pixels) {
                int nearest = 0;
                double minDist = Double.MAX_VALUE;
                for (int i = 0; i < k; i++) {
                    double dist = Math.pow(pixel[0] - centroids.get(i)[0], 2) +
                                  Math.pow(pixel[1] - centroids.get(i)[1], 2) +
                                  Math.pow(pixel[2] - centroids.get(i)[2], 2);
                    if (dist < minDist) { minDist = dist; nearest = i; }
                }
                clusters.get(nearest).add(pixel);
            }

            boolean changed = false;
            for (int i = 0; i < k; i++) {
                List<int[]> cluster = clusters.get(i);
                if (cluster.isEmpty()) continue;
                long r = 0, g = 0, b = 0;
                for (int[] p : cluster) { r += p[0]; g += p[1]; b += p[2]; }
                int[] newC = new int[]{(int)(r/cluster.size()), (int)(g/cluster.size()), (int)(b/cluster.size())};
                if (Math.abs(newC[0]-centroids.get(i)[0]) > 1) { centroids.set(i, newC); changed = true; }
            }
            if (!changed) break;
        }
        return centroids;
    }
}
