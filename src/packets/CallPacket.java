package packets;

import enums.PacketTypeEnum;

public class CallPacket extends Packet {
    private int sourceAddress;
    private int destinationAddress;
    
    public CallPacket(int connectionId, int sourceAddress, int destinationAddress) {
        super(connectionId, PacketTypeEnum.Call);
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
    
    public static CallPacket fromString(String packetString) {
        String[] parts = packetString.split("\\|");
        if (parts.length < 4 || !parts[0].equals(PacketTypeEnum.Call.toString())) {
            throw new IllegalArgumentException("Invalid Call packet format");
        }
        
        int connectionId = Integer.parseInt(parts[1]);
        int sourceAddress = Integer.parseInt(parts[2]);
        int destinationAddress = Integer.parseInt(parts[3]);
        
        return new CallPacket(connectionId, sourceAddress, destinationAddress);
    }
}