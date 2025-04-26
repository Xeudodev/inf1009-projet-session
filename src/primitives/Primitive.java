package primitives;
public abstract class Primitive {
    protected int identifier;

    public Primitive(int identifier) {
        this.identifier = identifier;
    }

    public int getIdentifier() {
        return identifier;
    }

}
