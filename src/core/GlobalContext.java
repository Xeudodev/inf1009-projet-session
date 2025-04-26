package core;
import java.io.File;
import java.io.IOException;

/**
 * Classe utilitaire qui gère les variables et compteurs globaux
 * utilisés dans l'application.
 */
public class GlobalContext {
    /**
     * Compteur d'identifiants de connexion.
     */
    private static int connectionIdCounter = 0;

    /**
     * Liste des fichiers utilisés par l'application.
     */
    public static final String[] FILES = {"S_lec", "S_erc", "L_lec", "L_ecr"};
    
    /**
     * Délai d'attente par défaut pour les connexions (en millisecondes).
     */
    public static final int DEFAULT_TIMEOUT = 5000;
    
    /**
     * Retourne un nouvel identifiant unique pour une connexion.
     */
    public static synchronized int getNextConnectionId() {
        return connectionIdCounter++;
    }
    
    /**
     * Réinitialise le compteur d'ID de connexion.
     */
    public static synchronized void resetConnectionIdCounter() {
        connectionIdCounter = 0;
    }
    
    /**
     * Initialise tous les fichiers utilisés par l'application.
     * Le fichier S_lec sera toujours initialisé avec un contenu par défaut s'il est vide.
     */
    public static void initializeFiles() {
        for (String file : FILES) {
            try {
                File f = new File(file);
                if (!f.exists()) {
                    f.createNewFile();
                }
                if (file.equals("S_lec") && f.length() == 0) {
                    String defaultContent = "bZ3mNa1KwPpO7xTuc9VfeX2hRyi0LMsZqtdbFJhAo4WIEvGd";
                    FileManager.writeToFile(file, defaultContent);
                } else if (!file.equals("S_lec")) {
                    // Réinitialiser les autres fichiers
                    FileManager.resetFile(file);
                }
            } catch (IOException e) {
                System.err.println("Erreur lors de l'initialisation du fichier " + file + " : " + e.getMessage());
            }
        }
    }
    
    /**
     * Nettoie tous les fichiers utilisés par l'application.
     */
    public static void cleanupFiles() {
        for (String file : FILES) {
            FileManager.resetFile(file);
        }
    }
}