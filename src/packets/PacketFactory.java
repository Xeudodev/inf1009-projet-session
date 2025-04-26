package packets;

import enums.PacketTypeEnum;

/**
 * Classe utilitaire pour créer des paquets à partir d'une chaîne de caractères
 */
public class PacketFactory {
    
    /**
     * Crée un paquet à partir d'une chaîne de caractères
     * @param packetString La chaîne représentant le paquet
     * @return Le paquet créé
     */
    public static Packet createFromString(String packetString) {
        if (packetString == null || packetString.isEmpty()) {
            throw new IllegalArgumentException("Empty packet string");
        }
        
        String[] parts = packetString.split("\\|");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid packet format");
        }
        
        String typeStr = parts[0];
        
        // Déterminer le type de paquet
        if (typeStr.equals(PacketTypeEnum.Call.toString())) {
            return CallPacket.fromString(packetString);
        } else if (typeStr.equals(PacketTypeEnum.ConnectionEstablished.toString())) {
            return ConnectionEstablishedPacket.fromString(packetString);
        } else if (typeStr.equals(PacketTypeEnum.Release.toString())) {
            return ReleasePacket.fromString(packetString);
        } else if (typeStr.equals(PacketTypeEnum.PositiveAck.toString()) || 
                  typeStr.equals(PacketTypeEnum.NegativeAck.toString())) {
            return AcknowledgementPacket.fromString(packetString);
        } else {
            // Par défaut, on suppose que c'est un paquet de données
            return DataPacket.fromString(packetString);
        }
    }
}