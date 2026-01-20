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
     * Utiliza K-Means en espacio de color CIELAB para mejor percepción humana.
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

            // 2. Obtener píxeles y convertir a CIELAB
            List<double[]> labPixels = new ArrayList<>();
            int width = image.getWidth();
            int height = image.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = (rgb) & 0xFF;
                    labPixels.add(rgbToLab(r, g, b));
                }
            }

            // 3. K-Means en espacio Lab
            List<double[]> centroids = calculateKMeans(labPixels, k);

            // 4. Convertir a Hex (Lab -> RGB -> Hex)
            return centroids.stream()
                    .map(c -> {
                        int[] rgb = labToRgb(c[0], c[1], c[2]);
                        return String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
                    })
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

    private List<double[]> calculateKMeans(List<double[]> pixels, int k) {
        Random random = new Random();
        List<double[]> centroids = new ArrayList<>();
        for (int i = 0; i < k; i++) centroids.add(pixels.get(random.nextInt(pixels.size())));

        for (int iter = 0; iter < 10; iter++) { // Max 10 iteraciones
            List<List<double[]>> clusters = new ArrayList<>();
            for (int i = 0; i < k; i++) clusters.add(new ArrayList<>());

            for (double[] pixel : pixels) {
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
                List<double[]> cluster = clusters.get(i);
                if (cluster.isEmpty()) continue;
                
                double sumL = 0, sumA = 0, sumB = 0;
                for (double[] p : cluster) { sumL += p[0]; sumA += p[1]; sumB += p[2]; }
                
                double[] newC = new double[]{ sumL/cluster.size(), sumA/cluster.size(), sumB/cluster.size() };
                
                double distChange = Math.pow(newC[0]-centroids.get(i)[0], 2) + 
                                    Math.pow(newC[1]-centroids.get(i)[1], 2) + 
                                    Math.pow(newC[2]-centroids.get(i)[2], 2);
                                    
                if (distChange > 0.1) { centroids.set(i, newC); changed = true; }
            }
            if (!changed) break;
        }
        return centroids;
    }

    // --- Conversión de Espacios de Color (RGB <-> CIELAB) ---

    private double[] rgbToLab(int r, int g, int b) {
        // RGB to XYZ
        double var_R = (r / 255.0);
        double var_G = (g / 255.0);
        double var_B = (b / 255.0);

        if (var_R > 0.04045) var_R = Math.pow(((var_R + 0.055) / 1.055), 2.4); else var_R = var_R / 12.92;
        if (var_G > 0.04045) var_G = Math.pow(((var_G + 0.055) / 1.055), 2.4); else var_G = var_G / 12.92;
        if (var_B > 0.04045) var_B = Math.pow(((var_B + 0.055) / 1.055), 2.4); else var_B = var_B / 12.92;

        var_R = var_R * 100; var_G = var_G * 100; var_B = var_B * 100;

        double X = var_R * 0.4124 + var_G * 0.3576 + var_B * 0.1805;
        double Y = var_R * 0.2126 + var_G * 0.7152 + var_B * 0.0722;
        double Z = var_R * 0.0193 + var_G * 0.1192 + var_B * 0.9505;

        // XYZ to Lab
        double var_X = X / 95.047;
        double var_Y = Y / 100.000;
        double var_Z = Z / 108.883;

        if (var_X > 0.008856) var_X = Math.pow(var_X, (1.0 / 3)); else var_X = (7.787 * var_X) + (16.0 / 116);
        if (var_Y > 0.008856) var_Y = Math.pow(var_Y, (1.0 / 3)); else var_Y = (7.787 * var_Y) + (16.0 / 116);
        if (var_Z > 0.008856) var_Z = Math.pow(var_Z, (1.0 / 3)); else var_Z = (7.787 * var_Z) + (16.0 / 116);

        double L = (116 * var_Y) - 16;
        double a = 500 * (var_X - var_Y);
        double bb = 200 * (var_Y - var_Z);

        return new double[]{L, a, bb};
    }

    private int[] labToRgb(double L, double a, double b) {
        double var_Y = (L + 16) / 116;
        double var_X = a / 500 + var_Y;
        double var_Z = var_Y - b / 200;

        if (Math.pow(var_Y, 3) > 0.008856) var_Y = Math.pow(var_Y, 3); else var_Y = (var_Y - 16.0 / 116) / 7.787;
        if (Math.pow(var_X, 3) > 0.008856) var_X = Math.pow(var_X, 3); else var_X = (var_X - 16.0 / 116) / 7.787;
        if (Math.pow(var_Z, 3) > 0.008856) var_Z = Math.pow(var_Z, 3); else var_Z = (var_Z - 16.0 / 116) / 7.787;

        double X = 95.047 * var_X;
        double Y = 100.000 * var_Y;
        double Z = 108.883 * var_Z;

        double var_R = X * 3.2406 + Y * -1.5372 + Z * -0.4986;
        double var_G = X * -0.9689 + Y * 1.8758 + Z * 0.0415;
        double var_B = X * 0.0557 + Y * -0.2040 + Z * 1.0570;

        var_R = var_R / 100; var_G = var_G / 100; var_B = var_B / 100;

        if (var_R > 0.0031308) var_R = 1.055 * Math.pow(var_R, (1 / 2.4)) - 0.055; else var_R = 12.92 * var_R;
        if (var_G > 0.0031308) var_G = 1.055 * Math.pow(var_G, (1 / 2.4)) - 0.055; else var_G = 12.92 * var_G;
        if (var_B > 0.0031308) var_B = 1.055 * Math.pow(var_B, (1 / 2.4)) - 0.055; else var_B = 12.92 * var_B;

        int r = (int) Math.round(var_R * 255);
        int g = (int) Math.round(var_G * 255);
        int bb = (int) Math.round(var_B * 255);

        return new int[]{
            Math.min(255, Math.max(0, r)),
            Math.min(255, Math.max(0, g)),
            Math.min(255, Math.max(0, bb))
        };
    }
}
