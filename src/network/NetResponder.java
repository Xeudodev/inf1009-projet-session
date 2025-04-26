package network;
import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import core.FileManager;
import enums.PacketTypeEnum;
import enums.ReasonEnum;

public class NetResponder extends Thread {

    private static final String LINK_INPUT_FILE = "L_lec"; // Était probablement "L_ec"
    private static final String LINK_OUTPUT_FILE = "L_ecr";
    
    private long lastReadPosition = 0;
    private Random random = new Random();
    private boolean isRunning = true;
    
    private static final ReentrantLock fileLock = new ReentrantLock();
    
    /**
     * Constructeur
     */
    public NetResponder() {
        super("NetResponder");
    }
    
    /**
     * Enregistre un message dans le fichier de log S_erc
     * @param message Le message à enregistrer
     */
    private void log(String message) {
        ET.writeToFile(message);
    }
    
    /**
     * Lit les nouvelles lignes du fichier de liaison d'entrée
     */
    private void readLink() {
        List<String> lines = FileManager.readLines(LINK_INPUT_FILE);
        
        if (lines != null && lines.size() > lastReadPosition) {
            for (int i = (int)lastReadPosition; i < lines.size(); i++) {
                String line = lines.get(i);
                try {
                    processPacket(line);
                } catch (IOException e) {
                    log("Erreur lors du traitement du paquet: " + e.getMessage());
                }
            }
            lastReadPosition = lines.size();
        }
    }
    
    /**
     * Écrit des données dans le fichier de liaison de sortie
     */
    private void writeLink(String data) throws IOException {
        fileLock.lock();
        try {
            FileManager.appendToFile(LINK_OUTPUT_FILE, data);
        } finally {
            fileLock.unlock();
        }
    }
    
    /**
     * Traite un paquet reçu du fichier de liaison
     */
    private void processPacket(String packetData) throws IOException {
        if (packetData == null || packetData.isEmpty()) {
            return;
        }
        
        String[] parts = packetData.split("\\|");
        if (parts.length < 3) {
            log("Format de paquet invalide: " + packetData);
            return;
        }
        
        String packetType = parts[0];
        String connectionNumber = parts[1];
        
        // Vérifier s'il s'agit d'un paquet de données ou de contrôle
        if (isDataPacket(packetType)) {
            if (parts.length < 4) {
                log("Format de paquet de données invalide: " + packetData);
                return;
            }
            
            String sourceAddr = parts[2];
            String destAddr = parts[3];
            String payload = parts.length > 4 ? parts[4] : "";
            
            String result = verifyRemote(connectionNumber, sourceAddr, destAddr);
            processAcknowledgement(result, connectionNumber, result.equals("ACK"));
            
            if (result.equals("ACK")) {
                log("Données reçues pour connexion " + connectionNumber + ": " + payload);
            }
        } else {
            handleControlPacket(packetType, connectionNumber, parts);
        }
    }
    
    /**
     * Gère les paquets de contrôle
     */
    private void handleControlPacket(String packetType, String connectionNumber, String[] parts) throws IOException {
        if (packetType.equals(PacketTypeEnum.Call.toString())) {
            boolean acceptConnection = random.nextInt(10) > 2; // 80% de chances d'accepter
            
            if (acceptConnection) {
                processAcknowledgement("ACK", connectionNumber, true);
                log("Connexion " + connectionNumber + " acceptée");
            } else {
                processAcknowledgement("NACK", connectionNumber, false);
                log("Connexion " + connectionNumber + " rejetée");
            }
        } 
        else if (packetType.equals(PacketTypeEnum.Release.toString())) {
            processAcknowledgement("ACK", connectionNumber, true);
            log("Connexion " + connectionNumber + " terminée");
        }
        else if (packetType.equals(PacketTypeEnum.ConnectionEstablished.toString())) {
            log("Connexion " + connectionNumber + " établie");
        }
    }
    
    /**
     * Envoie un accusé de réception
     */
    private void processAcknowledgement(String packetType, String connectionNumber, boolean status) throws IOException {
        String response;
        
        if (status) {
            response = PacketTypeEnum.PositiveAck.toString() + "|" + connectionNumber;
        } else {
            response = PacketTypeEnum.NegativeAck.toString() + "|" + connectionNumber + "|" + ReasonEnum.REMOTE_REJECTION;
        }
        
        writeLink(response);
        log("Envoi " + (status ? "ACK" : "NACK") + " pour connexion " + connectionNumber);
    }
    
    /**
     * Vérifie si le type de paquet est un paquet de données
     */
    private boolean isDataPacket(String type) {
        return !type.equals(PacketTypeEnum.Call.toString()) && 
               !type.equals(PacketTypeEnum.ConnectionEstablished.toString()) && 
               !type.equals(PacketTypeEnum.Release.toString()) && 
               !type.equals(PacketTypeEnum.PositiveAck.toString()) && 
               !type.equals(PacketTypeEnum.NegativeAck.toString());
    }
    
    /**
     * Vérifie si la connexion distante est valide
     */
    private String verifyRemote(String connectionNum, String sourceAddr, String destAddr) {
        try {
            int connId = Integer.parseInt(connectionNum);
            int srcAddr = Integer.parseInt(sourceAddr);
            int dstAddr = Integer.parseInt(destAddr);
            List<Connection> connections = ET.getConnections();
            for (Connection conn : connections) {
                if (conn.getIdentifier() == connId && 
                    conn.getStationSource() == srcAddr && 
                    conn.getStationDestination() == dstAddr) {
                    return "ACK";
                }
            }
        } catch (NumberFormatException e) {
            log("Format d'adresse invalide: " + e.getMessage());
        }
        
        return "NACK";
    }
    
    /**
     * Arrête proprement le thread
     */
    public void shutdown() {
        this.isRunning = false;
        this.interrupt();
    }
    
    @Override
    public void run() {
        log("NetResponder démarré");
        
        while (isRunning) {
            try {
                readLink();
                sleep(500);
            } catch (InterruptedException e) {
                if (isRunning) {
                    log("NetResponder interrompu: " + e.getMessage());
                }
            }
        }
        
        log("NetResponder arrêté");
    }
}