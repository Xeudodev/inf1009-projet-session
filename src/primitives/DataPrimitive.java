package primitives;

public class DataPrimitive extends Primitive {
    private String data;

    public DataPrimitive(int identifier, String data) {
        super(identifier);
        this.data = data;
    }
    
    public String getData() {
        return data;
    }
}