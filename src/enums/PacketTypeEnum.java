package enums;

/**
 * Contains the set of binary sequences indicating the packet types
 */
public enum PacketTypeEnum {
    Call, ConnectionEstablished, Release, PositiveAck, NegativeAck, Data;

    @Override
    public String toString() {
        switch (this) {
        case Call:
            return "00001011";
        case ConnectionEstablished:
            return "00001111";
        case Release:
            return "00010011";
        case NegativeAck:
            return "01001";
        case PositiveAck:
            return "00001";
        case Data:
            return "Data"; 
        default:
            return "";
        }
    }
}
