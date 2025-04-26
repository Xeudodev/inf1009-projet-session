import core.GlobalContext;
import network.ER;
import network.ET;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Initialisation des fichiers...");
        GlobalContext.initializeFiles();
        System.out.println("Tous les fichiers sont prêts.");
        
        // Création et démarrage des entités réseau et transport
        ER er = new ER();
        ET et = new ET(er);
        
        er.start();
        
        // Initialisation des connexions
        et.init();
        et.start();
        
        System.out.println("Programme en cours d'exécution...");
        
        // Attendre la fin du traitement des connexions
        et.join();
        
        // Arrêter proprement l'entité réseau
        er.shutdown();
        er.join();
        
        System.out.println("Nettoyage des fichiers...");
        // On ne nettoie pas les fichiers pour pouvoir examiner les résultats
        // GlobalContext.cleanupFiles();
        
        System.out.println("Programme terminé");
    }
}