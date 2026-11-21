package javax.baja.sys;

public final class BFacets extends BValue {
    public static final BFacets DEFAULT = new BFacets();

    public static BFacets make(String name, BValue value) {
        return new BFacets();
    }

    public boolean getb(String name, boolean def) {
        return def;
    }
}
