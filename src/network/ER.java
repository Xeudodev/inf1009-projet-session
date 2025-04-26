package network;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import core.FileManager;
import enums.ConnectionStateEnum;
import enums.ReasonEnum;
import packets.*;
import primitives.*;

public class ER extends Thread {
    
    // File d'attente des primitives à traiter
    private BlockingQueue<Primitive> primitivesQueue = new LinkedBlockingQueue<>();
    
    // Table des connexions gérées par ER
    private Map<Integer, NetworkConnection> connections = new HashMap<>();
    
    // Compteur pour générer les numéros de connexion réseau
    private int networkConnectionCounter = 0;
        
    // Flag d'arrêt du thread
    private boolean running = true;
    
    // Chemin du fichier de liaison
    private static final String L_ECR_PATH = "L_ecr";
    
    /**
     * Classe interne pour stocker les informations d'une connexion réseau
     */
    private class NetworkConnection {
        int networkConnectionId;  // Numéro de connexion réseau attribué par ER
        int transportConnectionId; // Identifiant fourni par ET
        int sourceAddress;        // Adresse de la station source
        int destinationAddress;   // Adresse de la station destination
        ConnectionStateEnum state; // État de la connexion
        
        NetworkConnection(int transportId, int sourceAddr, int destAddr) {
            this.networkConnectionId = generateNetworkConnectionId();
            this.transportConnectionId = transportId;
            this.sourceAddress = sourceAddr;
            this.destinationAddress = destAddr;
            this.state = ConnectionStateEnum.WAITING_CONFIRMATION;
        }
    }
    
    /**
     * Constructeur de l'entité réseau
     */
    public ER() {
        super("ER");
    }
    
    /**
     * Méthode principale du thread
     */
    @Override
    public void run() {
        log("Démarrage de l'entité réseau");
        
        while (running) {
            try {
                Primitive primitive = primitivesQueue.poll();
                if (primitive != null) {
                    processPrimitive(primitive);
                }
                sleep(50);
            } catch (InterruptedException e) {
                if (running) {
                    log("ER interrompu: " + e.getMessage());
                }
            }
        }
        
        log("Arrêt de l'entité réseau");
    }
    
    /**
     * Méthode pour recevoir une primitive de la couche transport
     */
    public void receivePrimitive(Primitive primitive) {
        primitivesQueue.add(primitive);
        log("Primitive reçue de ET: " + primitive.getClass().getSimpleName() + 
            " (ID:" + primitive.getIdentifier() + ")");
    }
    
    /**
     * Traitement des primitives reçues de la couche transport
     */
    private void processPrimitive(Primitive primitive) {
        if (primitive instanceof ConnectPrimitive) {
            processConnectRequest((ConnectPrimitive) primitive);
        } else if (primitive instanceof DataPrimitive) {
            processDataRequest((DataPrimitive) primitive);
        } else if (primitive instanceof DisconnectPrimitive) {
            processDisconnectRequest((DisconnectPrimitive) primitive);
        }
    }
    
    /**
     * Traitement d'une demande de connexion
     */
    private void processConnectRequest(ConnectPrimitive primitive) {
        int transportId = primitive.getIdentifier();
        int sourceAddr = ((ConnectPrimitive) primitive).getSourceAddress();
        int destAddr = ((ConnectPrimitive) primitive).getDestinationAddress();
        
        if (sourceAddr % 27 == 0) {
            log("Connexion refusée par le fournisseur pour ID:" + transportId + 
                " (source:" + sourceAddr + ")");
            
            DisconnectPrimitive disconnectPrim = new DisconnectPrimitive(
                transportId, 0, ReasonEnum.SUPPLIER_REJECTION);
            ET.receivePrimitive(disconnectPrim);
            return;
        }
        
        NetworkConnection connection = new NetworkConnection(transportId, sourceAddr, destAddr);
        connections.put(connection.networkConnectionId, connection);
        
        log("Connexion créée: Transport ID:" + transportId + 
            ", Réseau ID:" + connection.networkConnectionId + 
            " (source:" + sourceAddr + ", dest:" + destAddr + ")");
        
        CallPacket callPacket = new CallPacket(
            connection.networkConnectionId, sourceAddr, destAddr);
        writeToLink(callPacket.toString());
        
        if (sourceAddr % 19 == 0) {
            log("Aucune réponse reçue pour la connexion: " + connection.networkConnectionId);
            
            DisconnectPrimitive disconnectPrim = new DisconnectPrimitive(
                transportId, 0, ReasonEnum.REMOTE_REJECTION);
            ET.receivePrimitive(disconnectPrim);
            connections.remove(connection.networkConnectionId);
        } 
        else if (sourceAddr % 13 == 0) {
            log("Connexion refusée par le distant pour ID:" + connection.networkConnectionId);
            
            ReleasePacket releasePacket = new ReleasePacket(
                connection.networkConnectionId, destAddr, sourceAddr, ReasonEnum.REMOTE_REJECTION);
            
            writeToLinkLog(releasePacket.toString());
            
            DisconnectPrimitive disconnectPrim = new DisconnectPrimitive(
                transportId, 0, ReasonEnum.REMOTE_REJECTION);
            ET.receivePrimitive(disconnectPrim);
            
            connections.remove(connection.networkConnectionId);
        } 
        else {
            log("Connexion acceptée par le distant pour ID:" + connection.networkConnectionId);
            ConnectionEstablishedPacket establishedPacket = new ConnectionEstablishedPacket(
                connection.networkConnectionId, destAddr, sourceAddr);
            writeToLinkLog(establishedPacket.toString());
            connection.state = ConnectionStateEnum.ESTABLISHED;
            ConnectPrimitive connectConf = new ConnectPrimitive(transportId, 0);
            ET.receivePrimitive(connectConf);
        }
    }
    
    /**
     * Traitement d'une demande de transfert de données
     */
    private void processDataRequest(DataPrimitive primitive) {
        int transportId = primitive.getIdentifier();
        String data = primitive.getData();
        
        NetworkConnection connection = findConnectionByTransportId(transportId);
        
        if (connection == null) {
            log("Erreur: Aucune connexion trouvée pour ID:" + transportId);
            log("Connexions actives: " + connections.keySet());
            return;
        }
        
        if (connection.state != ConnectionStateEnum.ESTABLISHED) {
            log("Erreur: La connexion n'est pas établie pour ID:" + transportId + 
                ", état actuel: " + connection.state);
            return;
        }
        
        DataPacket[] packets = DataPacket.segmentData(
            connection.networkConnectionId, data, 0);
        
        for (DataPacket packet : packets) {
            writeToLink(packet.toString());
            
            log("Aucun acquittement reçu pour le paquet " + packet.getSendSequence() + 
                " de la connexion " + connection.networkConnectionId);
            log("Réémission du paquet " + packet.getSendSequence());
            
            writeToLink(packet.toString());
        }
        
        log("Transfert de données terminé pour ID:" + transportId);
    }
    
    /**
     * Traitement d'une demande de libération de connexion
     */
    private void processDisconnectRequest(DisconnectPrimitive primitive) {
        int transportId = primitive.getIdentifier();
        
        NetworkConnection connection = findConnectionByTransportId(transportId);
        
        if (connection == null) {
            log("Erreur: Aucune connexion trouvée pour ID:" + transportId);
            return;
        }
        ReleasePacket releasePacket = new ReleasePacket(connection.networkConnectionId, connection.sourceAddress, connection.destinationAddress);
        writeToLink(releasePacket.toString());
        log("Connexion libérée: Transport ID:" + transportId + ", Réseau ID:" + connection.networkConnectionId);
        connections.remove(connection.networkConnectionId);
    }
        
    /**
     * Écriture dans le fichier de liaison
     */
    private void writeToLink(String data) {
        try {
            File f = new File(L_ECR_PATH);
            if (!f.exists()) {
                f.createNewFile();
            }
            
            FileManager.appendToFile(f.getAbsolutePath(), data);
            log("Écriture dans L_ecr: " + data.trim());
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to write to L_ecr: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Écriture dans le fichier de log de liaison (pour tracer les réponses simulées)
     */
    private void writeToLinkLog(String data) {
        FileManager.appendToFile("L_lec", data);
        log("Simulation réponse dans L_lec: " + data);
    }
    
    /**
     * Trouver une connexion par son identifiant de transport
     */
    private NetworkConnection findConnectionByTransportId(int transportId) {
        for (NetworkConnection conn : connections.values()) {
            if (conn.transportConnectionId == transportId) {
                return conn;
            }
        }
        return null;
    }
    
    /**
     * Génération d'un identifiant de connexion réseau
     */
    private int generateNetworkConnectionId() {
        return networkConnectionCounter++;
    }
    
    /**
     * Journalisation
     */
    private void log(String message) {
        System.out.println("[ER] " + message);
    }
    
    /**
     * Arrêt propre du thread
     */
    public void shutdown() {
        this.running = false;
        this.interrupt();
    }
}