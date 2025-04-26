package core;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    /**
     * Crée un fichier avec le nom spécifié ou réinitialise son contenu s'il existe.
     */
    public static void createFile(String fileName) {
        try {
            File file = new File(fileName);
            if (file.exists()) {
                resetFile(fileName);
            } else {
                file.createNewFile();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du fichier " + fileName + " : " + e.getMessage());
        }
    }

    /**
     * Supprime le fichier avec le nom spécifié.
     */
    public static void deleteFile(String fileName) {
        File file = new File(fileName);
        if (!file.delete() && file.exists()) {
            System.err.println("Impossible de supprimer le fichier : " + fileName);
        }
    }

    /**
     * Réinitialise le fichier avec le nom spécifié en écrivant une chaîne vide.
     */
    public static void resetFile(String fileName) {
        try (FileWriter writer = new FileWriter(fileName, false)) {
            writer.write("");
        } catch (IOException e) {
            System.err.println("Erreur lors de la réinitialisation du fichier " + fileName + " : " + e.getMessage());
        }
    }

    /**
     * Lit le contenu d'un fichier et retourne les lignes sous forme de liste.
     * @param fileName Le nom du fichier à lire
     * @return Une liste de chaînes contenant chaque ligne du fichier
     */
    public static List<String> readLines(String fileName) {
        List<String> lines = new ArrayList<>();
        try {
            if (new File(fileName).exists()) {
                lines = Files.readAllLines(Paths.get(fileName));
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier " + fileName + " : " + e.getMessage());
        }
        return lines;
    }

    /**
     * Écrit le contenu fourni dans le fichier spécifié.
     * @param fileName Le nom du fichier dans lequel écrire
     * @param content Le contenu à écrire
     */
    public static void writeToFile(String fileName, String content) {
        try (FileWriter writer = new FileWriter(fileName, false)) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture dans le fichier " + fileName + " : " + e.getMessage());
        }
    }

    /**
     * Ajoute le contenu fourni à la fin du fichier spécifié.
     * @param fileName Le nom du fichier auquel ajouter du contenu
     * @param content Le contenu à ajouter
     */
    public static void appendToFile(String fileName, String content) {
        try (FileWriter writer = new FileWriter(fileName, true)) {
            writer.write(content);
            writer.write(System.lineSeparator()); 
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ajout au fichier " + fileName + " : " + e.getMessage());
        }
    }
}