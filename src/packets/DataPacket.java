package packets;

import enums.PacketTypeEnum;

public class DataPacket extends Packet {
    private String data;
    private int sendSequence; // p(s)
    private int receiveSequence; // p(r)
    private boolean moreData; // bit M
    private static final int MAX_DATA_SIZE = 128; // 128 octets maximum
    
    public DataPacket(int identifier, String data, int sendSequence, int receiveSequence, boolean moreData) {
        super(identifier, PacketTypeEnum.Data);
        
        if (data.length() > MAX_DATA_SIZE) {
            this.data = data.substring(0, MAX_DATA_SIZE);
        } else {
            this.data = data;
        }
        this.sendSequence = sendSequence % 8; 
        this.receiveSequence = receiveSequence % 8; 
        this.moreData = moreData;
    }
    
    public String getData() {
        return data;
    }
    
    public int getSendSequence() {
        return sendSequence;
    }
    
    public int getReceiveSequence() {
        return receiveSequence;
    }
    
    public boolean hasMoreData() {
        return moreData;
    }
    
    /**
     * Génère la représentation binaire du type de paquet selon le format spécifié.
     * Format: 0 p(r) M p(s) 0 
     * p(r) est sur les bits 6,5,4
     * M est sur le bit 3
     * p(s) est sur les bits 2,1,0
     */
    public String getBinaryType() {
        int typeValue = 0;
        typeValue |= (receiveSequence & 0x7) << 4; // p(r) - 3 bits
        if (moreData) {
            typeValue |= 1 << 3; // Bit M
        }
        typeValue |= (sendSequence & 0x7); // p(s)- 3 bits
        
        return String.format("%8s", Integer.toBinaryString(typeValue)).replace(' ', '0');
    }
    
    /**
     * Format spécial pour les paquets de données: binaryType|connectionId|data
     */
    @Override
    public String toString() {
        return this.getBinaryType() + "|" + this.identifier + "|" + this.data;
    }
    
    public static DataPacket fromString(String packetString) {
        String[] parts = packetString.split("\\|");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Format de paquet de données invalide");
        }
        
        String typeStr = parts[0];
        int connectionId = Integer.parseInt(parts[1]);
        String data = parts[2];
        
        // Décodage du format binaire
        int typeValue = Integer.parseInt(typeStr, 2);
        int receiveSequence = (typeValue >> 4) & 0x7; // Bits 6,5,4
        boolean moreData = ((typeValue >> 3) & 0x1) == 1; // Bit 3
        int sendSequence = typeValue & 0x7; // Bits 2,1,0
        
        return new DataPacket(connectionId, data, sendSequence, receiveSequence, moreData);
    }
    
    /**
     * Segmente les données en plusieurs paquets si nécessaire
     * @param connectionId L'identifiant de connexion
     * @param data Les données à segmenter
     * @param receiveSequence Le numéro de séquence attendu en réception
     * @return Une liste de paquets de données
     */
    public static DataPacket[] segmentData(int connectionId, String data, int receiveSequence) {
        if (data.length() <= MAX_DATA_SIZE) {
            return new DataPacket[] {
                new DataPacket(connectionId, data, 0, receiveSequence, false)
            };
        }
        
        int packetCount = (int) Math.ceil((double) data.length() / MAX_DATA_SIZE);
        DataPacket[] packets = new DataPacket[packetCount];
        
        for (int i = 0; i < packetCount; i++) {
            int startIndex = i * MAX_DATA_SIZE;
            int endIndex = Math.min(startIndex + MAX_DATA_SIZE, data.length());
            String segment = data.substring(startIndex, endIndex);
            boolean moreData = i < packetCount - 1;
            
            packets[i] = new DataPacket(connectionId, segment, i % 8, receiveSequence, moreData);
        }
        
        return packets;
    }
}