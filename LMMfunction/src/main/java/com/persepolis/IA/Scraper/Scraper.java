package com.persepolis.IA.Scraper;

import javax.swing.UIManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase principal que actúa como punto de entrada y salida para el módulo de Scraping.
 */
public class Scraper {

    private static final Logger logger = Logger.getLogger(Scraper.class.getName());

    public static void main(String[] args) {
        iniciar();
    }

    public static void iniciar() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> {
            new FormScraping().setVisible(true);
        });
    }
}