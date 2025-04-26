package packets;

import enums.PacketTypeEnum;

public abstract class Packet {
    
    protected int identifier;
    protected PacketTypeEnum type;

    public Packet(int identifier) {
        this.identifier = identifier;
    }

    public Packet(int identifier, PacketTypeEnum type) {
        this.identifier = identifier;
        this.type = type;
    }
}


