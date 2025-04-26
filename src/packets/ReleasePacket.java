package packets;

import enums.PacketTypeEnum;
import enums.ReasonEnum;

public class ReleasePacket extends Packet {
    private int sourceAddress;
    private int destinationAddress;
    private ReasonEnum reason;
    
    public ReleasePacket(int connectionId, int sourceAddress, int destinationAddress) {
        super(connectionId, PacketTypeEnum.Release);
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.reason = null;
    }
    
    public ReleasePacket(int connectionId, int sourceAddress, int destinationAddress, ReasonEnum reason) {
        super(connectionId, PacketTypeEnum.Release);
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.reason = reason;
    }
    
    public int getSourceAddress() {
        return sourceAddress;
    }
    
    public int getDestinationAddress() {
        return destinationAddress;
    }
    
    public ReasonEnum getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        // Format: type|connectionId|sourceAddress|destinationAddress|reason
        StringBuilder sb = new StringBuilder();
        sb.append(this.type.toString())
          .append("|").append(this.identifier)
          .append("|").append(this.sourceAddress)
          .append("|").append(this.destinationAddress);
        
        if (reason != null) {
            sb.append("|").append(reason.toString());
        }
        
        return sb.toString();
    }
    
    public static ReleasePacket fromString(String packetString) {
        String[] parts = packetString.split("\\|");
        if (parts.length < 4 || !parts[0].equals(PacketTypeEnum.Release.toString())) {
            throw new IllegalArgumentException("Invalid Release packet format");
        }
        
        int connectionId = Integer.parseInt(parts[1]);
        int sourceAddress = Integer.parseInt(parts[2]);
        int destinationAddress = Integer.parseInt(parts[3]);
        
        ReleasePacket packet;
        if (parts.length > 4) {
            String reasonCode = parts[4];
            ReasonEnum reason = null;
            
            if (reasonCode.equals(ReasonEnum.REMOTE_REJECTION.toString())) {
                reason = ReasonEnum.REMOTE_REJECTION;
            } else if (reasonCode.equals(ReasonEnum.SUPPLIER_REJECTION.toString())) {
                reason = ReasonEnum.SUPPLIER_REJECTION;
            }
            
            packet = new ReleasePacket(connectionId, sourceAddress, destinationAddress, reason);
        } else {
            packet = new ReleasePacket(connectionId, sourceAddress, destinationAddress);
        }
        
        return packet;
    }
}