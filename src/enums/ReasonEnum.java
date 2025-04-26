package enums;

public enum ReasonEnum {
    
    UNKNOWN,
    REMOTE_REJECTION,
    SUPPLIER_REJECTION;


    @Override
    public String toString() {
        switch (this) {
            case REMOTE_REJECTION:
                return "00000001";
            case SUPPLIER_REJECTION:
                return "00000010";
            case UNKNOWN:
            default:
                return "";
        }
    }
}
