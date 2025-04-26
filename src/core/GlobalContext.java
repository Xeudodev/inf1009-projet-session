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
            File f = new File(file);
            if (file.equals("S_lec") && (!f.exists() || f.length() == 0)) {
                try {
                    if (!f.exists()) f.createNewFile();
                    String defaultContent = 
                    "bZ3mNa1KwPpO7xTuc9VfeX2hRyi0LMsZqtdbFJhAo4WIEvGd\n" +
                    "T9YpUr2oMszAWqX3fKHJ0bNdt4ZRx81CgOLvMeSVFDu7ikPh\n" +
                    "fRp4cTYujObVa2qNKLd6bW1XZlmtsDFG3YiJpoEwh0RM7Vqg\n" +
                    "KXy31DZsqnFTuoRPb0OvgaVm9MeYJLQikXwBzCt47lA8HWRd\n" +
                    "eJZVsU3rbKDfw8PqoTXYmvAl14iMOCpx2zFNhtWRyLEg70dB\n" +
                    "xPwrLTud2zF9giyqCYB4MnOVF7jAoXsHJlQb1MKpetWD3Rv0\n" +
                    "O7VCiNFbXdMyptqZRAj1KHgs9LEUwvOTxyPQlrBmD30FW48z\n" +
                    "VX9iKPmLyWdoOJzphMUrqT1A4bCFLQb7Ef2WgXtRNDc53svy\n" +
                    "M4FLrvZCXdQTpoYJ9bOiwlpgUmANKte0Bs2x7yEWhf3VdRqG\n" +
                    "lTZDybvF7xOWAPCUgqXNJKtom49VpsEhrL2YdMic0BQaf3wR";
                    FileManager.writeToFile(file, defaultContent);
                } catch (IOException e) {
                    System.err.println("Erreur lors de l'initialisation du fichier " + file + " : " + e.getMessage());
                }
            } else {
                FileManager.createFile(file);
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