package packets;

import enums.PacketTypeEnum;
import enums.ReasonEnum;

public class AcknowledgementPacket extends Packet {
    private boolean positive;
    private int receiveSequence;
    private ReasonEnum reason;
    
    public AcknowledgementPacket(int connectionId, boolean positive, int receiveSequence) {
        super(connectionId, positive ? PacketTypeEnum.PositiveAck : PacketTypeEnum.NegativeAck);
        this.positive = positive;
        this.receiveSequence = receiveSequence % 8;
        this.reason = null;
    }
    
    public AcknowledgementPacket(int connectionId, int receiveSequence, ReasonEnum reason) {
        super(connectionId, PacketTypeEnum.NegativeAck);
        this.positive = false;
        this.receiveSequence = receiveSequence % 8;
        this.reason = reason;
    }
    
    public boolean isPositive() {
        return positive;
    }
    
    public int getReceiveSequence() {
        return receiveSequence;
    }
    
    public ReasonEnum getReason() {
        return reason;
    }
    
    public String getPacketType() {
        String base = positive ? "00001" : "01001";
        return String.format("%3s", Integer.toBinaryString(receiveSequence)).replace(' ', '0') + base;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getPacketType())
          .append("|").append(this.identifier);
        
        if (!positive && reason != null) {
            sb.append("|").append(reason.toString());
        }
        
        return sb.toString();
    }
    
    public static AcknowledgementPacket fromString(String packetString) {
        String[] parts = packetString.split("\\|");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid Acknowledgement packet format");
        }
        
        String typeStr = parts[0];
        int connectionId = Integer.parseInt(parts[1]);
        int receiveSequence = Integer.parseInt(typeStr.substring(0, 3), 2);
        boolean positive = typeStr.endsWith("00001");
        
        if (positive) {
            return new AcknowledgementPacket(connectionId, true, receiveSequence);
        } else {
            ReasonEnum reason = null;
            if (parts.length > 2) {
                String reasonCode = parts[2];
                if (reasonCode.equals(ReasonEnum.REMOTE_REJECTION.toString())) {
                    reason = ReasonEnum.REMOTE_REJECTION;
                } else if (reasonCode.equals(ReasonEnum.SUPPLIER_REJECTION.toString())) {
                    reason = ReasonEnum.SUPPLIER_REJECTION;
                }
            }
            return new AcknowledgementPacket(connectionId, receiveSequence, reason);
        }
    }
}