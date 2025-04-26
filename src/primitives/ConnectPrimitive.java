package primitives;

public class ConnectPrimitive extends Primitive {
    private int sourceAddress;
    private int destinationAddress;
    private int responseAddress;

    public ConnectPrimitive(int identifier, int sourceAddress, int destinationAddress) {
        super(identifier);
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
    }

    public ConnectPrimitive(int identifier, int responseAddress) {
        super(identifier);
        this.responseAddress = responseAddress;
    }
    
    public int getSourceAddress() {
        return sourceAddress;
    }
    
    public int getDestinationAddress() {
        return destinationAddress;
    }
    
    public int getResponseAddress() {
        return responseAddress;
    }
}