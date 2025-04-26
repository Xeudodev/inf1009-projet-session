package network;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    
    // Générateur de nombres aléatoires
    private Random random = new Random();
    
    // Flag d'arrêt du thread
    private boolean running = true;
    
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
                // Traiter les primitives en attente
                Primitive primitive = primitivesQueue.poll();
                if (primitive != null) {
                    processPrimitive(primitive);
                }
                
                // Vérifier les réponses de la couche liaison
                checkLinkResponses();
                
                // Pause courte pour économiser les ressources CPU
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
        
        // Déterminer si le fournisseur de service réseau accepte la connexion
        if (sourceAddr % 27 == 0) {
            // Refus de connexion par le fournisseur
            log("Connexion refusée par le fournisseur pour ID:" + transportId + 
                " (source:" + sourceAddr + ")");
            
            // Envoyer la primitive N_DISCONNECT.ind à ET
            DisconnectPrimitive disconnectPrim = new DisconnectPrimitive(
                transportId, 0, ReasonEnum.SUPPLIER_REJECTION);
            ET.receivePrimitive(disconnectPrim);
            return;
        }
        
        // Création de la connexion réseau
        NetworkConnection connection = new NetworkConnection(transportId, sourceAddr, destAddr);
        connections.put(connection.networkConnectionId, connection);
        
        log("Connexion créée: Transport ID:" + transportId + 
            ", Réseau ID:" + connection.networkConnectionId + 
            " (source:" + sourceAddr + ", dest:" + destAddr + ")");
        
        // Création et envoi du paquet d'appel
        CallPacket callPacket = new CallPacket(
            connection.networkConnectionId, sourceAddr, destAddr);
        
        // Écrire dans le fichier de liaison
        writeToLink(callPacket.toString());
        
        // Simuler la réponse du distant selon les règles de l'énoncé
        if (sourceAddr % 19 == 0) {
            // Pas de réponse si la source est multiple de 19
            log("Aucune réponse reçue pour la connexion: " + connection.networkConnectionId);
            
            // Envoyer la primitive N_DISCONNECT.ind à ET
            DisconnectPrimitive disconnectPrim = new DisconnectPrimitive(
                transportId, 0, ReasonEnum.REMOTE_REJECTION);
            ET.receivePrimitive(disconnectPrim);
            
            // Supprimer la connexion
            connections.remove(connection.networkConnectionId);
        } 
        else if (sourceAddr % 13 == 0) {
            // Refus de connexion si la source est multiple de 13
            log("Connexion refusée par le distant pour ID:" + connection.networkConnectionId);
            
            // Simuler la réception d'un paquet de refus
            ReleasePacket releasePacket = new ReleasePacket(
                connection.networkConnectionId, destAddr, sourceAddr, ReasonEnum.REMOTE_REJECTION);
            
            // Écrire la réponse dans L_lec pour trace
            writeToLinkLog(releasePacket.toString());
            
            // Envoyer la primitive N_DISCONNECT.ind à ET
            DisconnectPrimitive disconnectPrim = new DisconnectPrimitive(
                transportId, 0, ReasonEnum.REMOTE_REJECTION);
            ET.receivePrimitive(disconnectPrim);
            
            // Supprimer la connexion
            connections.remove(connection.networkConnectionId);
        } 
        else {
            // Acceptation de la connexion dans les autres cas
            log("Connexion acceptée par le distant pour ID:" + connection.networkConnectionId);
            
            // Simuler la réception d'un paquet d'acceptation
            ConnectionEstablishedPacket establishedPacket = new ConnectionEstablishedPacket(
                connection.networkConnectionId, destAddr, sourceAddr);
            
            // Écrire la réponse dans L_lec pour trace
            writeToLinkLog(establishedPacket.toString());
            
            // Mettre à jour l'état de la connexion
            connection.state = ConnectionStateEnum.ESTABLISHED;
            
            // Envoyer la primitive N_CONNECT.conf à ET
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
        
        // Trouver la connexion associée à cet identifiant de transport
        NetworkConnection connection = findConnectionByTransportId(transportId);
        
        if (connection == null || connection.state != ConnectionStateEnum.ESTABLISHED) {
            log("Erreur: Aucune connexion établie trouvée pour ID:" + transportId);
            return;
        }
        
        // Segmenter les données si nécessaire
        DataPacket[] packets = DataPacket.segmentData(
            connection.networkConnectionId, data, 0);
        
        // Envoyer chaque paquet et attendre l'acquittement avant d'envoyer le suivant
        for (int i = 0; i < packets.length; i++) {
            DataPacket packet = packets[i];
            
            // Écrire dans le fichier de liaison
            writeToLink(packet.toString());
            
            // Simuler la réponse selon les règles de l'énoncé
            if (connection.sourceAddress % 15 == 0) {
                // Pas d'acquittement si la source est multiple de 15
                log("Aucun acquittement reçu pour le paquet " + i + " de la connexion " + 
                    connection.networkConnectionId);
                
                // Réémission du paquet (une seule tentative)
                log("Réémission du paquet " + i);
                writeToLink(packet.toString());
                
                // Si toujours pas d'acquittement, on continue avec le paquet suivant
            } 
            else {
                // Tirage aléatoire pour déterminer si l'acquittement est positif ou négatif
                int randomSeq = random.nextInt(8);
                
                if (packet.getSendSequence() == randomSeq) {
                    // Acquittement négatif
                    log("Acquittement négatif reçu pour le paquet " + i + " de la connexion " + 
                        connection.networkConnectionId);
                    
                    // Simuler la réception d'un acquittement négatif
                    AcknowledgementPacket nackPacket = new AcknowledgementPacket(
                        connection.networkConnectionId, false, (packet.getSendSequence() + 1) % 8);
                    
                    // Écrire la réponse dans L_lec pour trace
                    writeToLinkLog(nackPacket.toString());
                    
                    // Réémission du paquet (une seule tentative)
                    log("Réémission du paquet " + i);
                    writeToLink(packet.toString());
                    
                    // Simuler un acquittement positif pour la réémission
                    AcknowledgementPacket ackPacket = new AcknowledgementPacket(
                        connection.networkConnectionId, true, (packet.getSendSequence() + 1) % 8);
                    writeToLinkLog(ackPacket.toString());
                } 
                else {
                    // Acquittement positif
                    log("Acquittement positif reçu pour le paquet " + i + " de la connexion " + 
                        connection.networkConnectionId);
                    
                    // Simuler la réception d'un acquittement positif
                    AcknowledgementPacket ackPacket = new AcknowledgementPacket(
                        connection.networkConnectionId, true, (packet.getSendSequence() + 1) % 8);
                    
                    // Écrire la réponse dans L_lec pour trace
                    writeToLinkLog(ackPacket.toString());
                }
            }
        }
        
        log("Transfert de données terminé pour ID:" + transportId);
    }
    
    /**
     * Traitement d'une demande de libération de connexion
     */
    private void processDisconnectRequest(DisconnectPrimitive primitive) {
        int transportId = primitive.getIdentifier();
        
        // Trouver la connexion associée à cet identifiant de transport
        NetworkConnection connection = findConnectionByTransportId(transportId);
        
        if (connection == null) {
            log("Erreur: Aucune connexion trouvée pour ID:" + transportId);
            return;
        }
        
        // Création et envoi du paquet de libération
        ReleasePacket releasePacket = new ReleasePacket(
            connection.networkConnectionId, connection.sourceAddress, connection.destinationAddress);
        
        // Écrire dans le fichier de liaison
        writeToLink(releasePacket.toString());
        
        log("Connexion libérée: Transport ID:" + transportId + 
            ", Réseau ID:" + connection.networkConnectionId);
        
        // Supprimer la connexion
        connections.remove(connection.networkConnectionId);
    }
    
    /**
     * Vérification des réponses de la couche liaison
     */
    private void checkLinkResponses() {
        // Dans notre simulation, les réponses sont générées et traitées directement
        // dans les méthodes de traitement des primitives
    }
    
    /**
     * Écriture dans le fichier de liaison
     */
    private void writeToLink(String data) {
        FileManager.appendToFile("L_ecr", data);
        log("Écriture dans L_ecr: " + data);
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