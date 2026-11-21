package javax.baja.sys;

public class Slot {
    private final String name;

    public Slot() {
        this("");
    }

    public Slot(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
