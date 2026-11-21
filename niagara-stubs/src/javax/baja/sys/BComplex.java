package javax.baja.sys;

public class BComplex extends BValue {
    public Property[] getDynamicPropertiesArray() {
        return new Property[0];
    }

    public BValue get(Property property) {
        return null;
    }

    public BValue get(String name) {
        return null;
    }

    public void set(Property property, BValue value, Context cx) {
    }

    public BFacets getSlotFacets(Property property) {
        return BFacets.DEFAULT;
    }

    public void add(String name, BValue value, int flags, BFacets facets, Context cx) {
    }

    public void remove(String name) {
    }

    protected static Property newProperty(int flags, String defaultValue, BFacets facets) {
        return new Property();
    }

    protected static Property newProperty(int flags, int defaultValue, BFacets facets) {
        return new Property();
    }

    protected static Property newProperty(int flags, boolean defaultValue, BFacets facets) {
        return new Property();
    }

    protected static Property newProperty(int flags, BValue defaultValue, BFacets facets) {
        return new Property();
    }

    protected static Action newAction(int flags, BFacets facets) {
        return new Action();
    }

    public String getString(Property property) {
        return "";
    }

    public void setString(Property property, String value, Context cx) {
    }

    public int getInt(Property property) {
        return 0;
    }

    public void setInt(Property property, int value, Context cx) {
    }

    public boolean getBoolean(Property property) {
        return false;
    }

    public void setBoolean(Property property, boolean value, Context cx) {
    }

    public void invoke(Action action, BValue arg, Context cx) {
    }
}
