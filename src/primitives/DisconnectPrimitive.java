package primitives;

import enums.ReasonEnum;

public class DisconnectPrimitive extends Primitive {
    private int responseAddress;
    private ReasonEnum reason;

    public DisconnectPrimitive(int identifier, int responseAddress, ReasonEnum reason) {
        super(identifier);
        this.responseAddress = responseAddress;
        this.reason = reason;
    }

    public DisconnectPrimitive(int identifier, int responseAddress) {
        this(identifier, responseAddress, null);
    }
    
    public int getResponseAddress() {
        return responseAddress;
    }
    
    public ReasonEnum getReason() {
        return reason;
    }
}