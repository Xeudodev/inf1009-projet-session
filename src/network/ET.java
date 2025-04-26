package network;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import core.FileManager;
import enums.ConnectionStateEnum;
import enums.ReasonEnum;
import primitives.*;

public class ET extends Thread {

    private static ArrayList<Connection> connections = new ArrayList<Connection>();
    private static final ReentrantLock fileLock = new ReentrantLock();
    
    // Référence à l'entité réseau
    private ER networkEntity;
    
    // File d'attente des primitives reçues de l'entité réseau
    private static BlockingQueue<Primitive> primitivesQueue = new LinkedBlockingQueue<>();
    
    /**
     * Constructeur avec référence à l'entité réseau
     */
    public ET(ER networkEntity) {
        super("ET");
        this.networkEntity = networkEntity;
    }
    
    /**
     * Initialisation des connexions à partir du fichier S_lec
     */
    public void init() {
        List<String> lines = FileManager.readLines("S_lec");
        
        for (int i = 0; i < lines.size(); i++) {
            String data = lines.get(i);
            Connection connection = new Connection(data);
            connections.add(connection);
        }
    }
    
    /**
     * Affichage de l'état des connexions
     */
    public void displayConnections() {
        if(connections.isEmpty()) {
            System.out.println("Aucune connexion à afficher.");
            return;
        }

        System.out.println("\n--- État des connexions ---");
        for (Connection connection : connections) {
            System.out.println(connection.toString());
        }
    }

    /**
     * Mise à jour de l'état des connexions
     */
    public void update() {
        Iterator<Connection> iterator = connections.iterator();
        boolean anyChanges = false;
        
        while (iterator.hasNext()) {
            Connection connection = iterator.next();
            
            if (connection.getEtatConnexion() == ConnectionStateEnum.WAITING_CONFIRMATION) {
                if (!connection.isProcessed()) {
                    sendConnectRequest(connection);
                    connection.setProcessed(true);
                    anyChanges = true;
                }
            } 
            else if (connection.getEtatConnexion() == ConnectionStateEnum.ESTABLISHED) {
                if (!connection.isDataSent()) {
                    sendDataRequest(connection);
                    connection.setDataSent(true);
                    anyChanges = true;
                }
                
                if (connection.isDataSent() && !connection.isReleaseRequested()) {
                    sendDisconnectRequest(connection);
                    connection.setReleaseRequested(true);
                    anyChanges = true;
                }
                
                if (connection.isReleaseConfirmed()) {
                    log("Suppression de la connexion " + connection.getIdentifier() + " de la liste");
                    iterator.remove();
                    anyChanges = true;
                }
            }
        }
        
        if (anyChanges) {
            displayConnections();
        }
    }

    /**
     * Écriture dans le fichier de log S_erc
     */
    public static void writeToFile(String data) {
        fileLock.lock();
        try {
            if (!data.endsWith("\n")) {
                data = data + "\n";
            }
            FileManager.appendToFile("S_erc", data);
            System.out.println("[ET] Écrit dans S_erc: " + data.trim());
        } catch (Exception e) {
            System.err.println("Erreur lors de l'écriture dans le fichier : " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }
    
    /**
     * Méthode principale du thread
     */
    @Override
    public void run() {
        log("Démarrage de l'entité transport");
        for (Connection connection : connections) {
            connection.start();
        }
        
        displayConnections();
        int unchangedCycles = 0;
        int previousSize = connections.size();
        
        // Boucle principale de traitement
        while (!connections.isEmpty() && unchangedCycles < 20) { 
            try {
                processPrimitives();
                update();
                if (connections.size() == previousSize) {
                    unchangedCycles++;
                    if (unchangedCycles % 5 == 0) {
                        log("Attention: Aucun changement depuis " + unchangedCycles + " cycles.");
                    }
                } else {
                    unchangedCycles = 0;
                    previousSize = connections.size();
                }
                
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        if (unchangedCycles >= 20) {
            log("Détection de blocage possible. Arrêt forcé du traitement.");
            Iterator<Connection> iterator = connections.iterator();
            while (iterator.hasNext()) {
                Connection connection = iterator.next();
                log("Libération forcée de la connexion " + connection.getIdentifier());
                writeToFile("Connexion " + connection.getIdentifier() + " libérée par force (timeout)");
                iterator.remove();
            }
        }
        
        log("Toutes les connexions ont été traitées.");
    }

    /**
     * Retourne la liste des connexions
     */
    public static ArrayList<Connection> getConnections() {
        return connections;
    }
    
    /**
     * Réception d'une primitive de l'entité réseau
     */
    public static void receivePrimitive(Primitive primitive) {
        primitivesQueue.add(primitive);
    }
    
    /**
     * Traitement des primitives reçues de l'entité réseau
     */
    private void processPrimitives() {
        try {
            Primitive primitive = primitivesQueue.poll();
            if (primitive == null) {
                return;
            }
            
            int connectionId = primitive.getIdentifier();
            Connection connection = findConnectionById(connectionId);
            
            if (connection == null) {
                log("Erreur: Connexion non trouvée pour ID:" + connectionId);
                return;
            }
            
            if (primitive instanceof ConnectPrimitive) {
                log("Connexion établie pour ID:" + connectionId);
                connection.setEtatConnexion(ConnectionStateEnum.ESTABLISHED);
                writeToFile("Connexion " + connectionId + " établie");
            } 
            else if (primitive instanceof DisconnectPrimitive) {
                DisconnectPrimitive disconnectPrim = (DisconnectPrimitive) primitive;
                
                if (disconnectPrim.getReason() == ReasonEnum.SUPPLIER_REJECTION) {
                    log("Connexion refusée par le fournisseur pour ID:" + connectionId);
                    writeToFile("Connexion " + connectionId + " refusée par le fournisseur");
                } else {
                    log("Connexion refusée par le distant pour ID:" + connectionId);
                    writeToFile("Connexion " + connectionId + " refusée par le distant");
                }
                
                connection.setReleaseConfirmed(true);
            }
        } catch (Exception e) {
            System.err.println("[ET] Erreur dans processPrimitives: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Envoi d'une demande de connexion à l'entité réseau
     */
    private void sendConnectRequest(Connection connection) {
        ConnectPrimitive primitive = new ConnectPrimitive(
            connection.getIdentifier(), 
            connection.getStationSource(), 
            connection.getStationDestination());
        
        log("Envoi d'une demande de connexion pour ID:" + connection.getIdentifier() + 
            " (source:" + connection.getStationSource() + 
            ", dest:" + connection.getStationDestination() + ")");
        
        networkEntity.receivePrimitive(primitive);
    }
    
    /**
     * Envoi d'une demande de transfert de données à l'entité réseau
     */
    private void sendDataRequest(Connection connection) {
        DataPrimitive primitive = new DataPrimitive(
            connection.getIdentifier(), 
            connection.getData());
        
        log("Envoi de données pour ID:" + connection.getIdentifier());
        
        networkEntity.receivePrimitive(primitive);
    }
    
    /**
     * Envoi d'une demande de libération de connexion à l'entité réseau
     */
    private void sendDisconnectRequest(Connection connection) {
        DisconnectPrimitive primitive = new DisconnectPrimitive(
            connection.getIdentifier(), 0);
        
        log("Envoi d'une demande de libération pour ID:" + connection.getIdentifier());
        
        networkEntity.receivePrimitive(primitive);
        connection.setReleaseConfirmed(true);
    }
    
    /**
     * Recherche d'une connexion par son identifiant
     */
    private Connection findConnectionById(int connectionId) {
        for (Connection connection : connections) {
            if (connection.getIdentifier() == connectionId) {
                return connection;
            }
        }
        return null;
    }
    
    /**
     * Journalisation
     */
    private void log(String message) {
        System.out.println("[ET] " + message);
    }
}