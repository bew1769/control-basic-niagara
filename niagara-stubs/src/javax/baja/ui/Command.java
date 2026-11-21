package javax.baja.ui;

public abstract class Command {
    private final BWidget owner;
    private final String label;

    public Command(BWidget owner, String label) {
        this.owner = owner;
        this.label = label;
    }

    public BWidget getOwner() {
        return owner;
    }

    public String getLabel() {
        return label;
    }

    public abstract CommandArtifact doInvoke() throws Exception;
}
