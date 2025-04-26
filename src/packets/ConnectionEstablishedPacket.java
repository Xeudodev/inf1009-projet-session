package packets;

import enums.PacketTypeEnum;

public class ConnectionEstablishedPacket extends Packet {
    private int sourceAddress;
    private int destinationAddress;
    
    public ConnectionEstablishedPacket(int connectionId, int sourceAddress, int destinationAddress) {
        super(connectionId, PacketTypeEnum.ConnectionEstablished);
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
    }
    
    public int getSourceAddress() {
        return sourceAddress;
    }
    
    public int getDestinationAddress() {
        return destinationAddress;
    }
    
    @Override
    public String toString() {
        // Format: type|connectionId|sourceAddress|destinationAddress
        return this.type.toString() + "|" + this.identifier + "|" + 
               this.sourceAddress + "|" + this.destinationAddress;
    }
    
    public static ConnectionEstablishedPacket fromString(String packetString) {
        String[] parts = packetString.split("\\|");
        if (parts.length < 4 || !parts[0].equals(PacketTypeEnum.ConnectionEstablished.toString())) {
            throw new IllegalArgumentException("Invalid ConnectionEstablished packet format");
        }
        
        int connectionId = Integer.parseInt(parts[1]);
        int sourceAddress = Integer.parseInt(parts[2]);
        int destinationAddress = Integer.parseInt(parts[3]);
        
        return new ConnectionEstablishedPacket(connectionId, sourceAddress, destinationAddress);
    }
}