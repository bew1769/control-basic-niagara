package javax.baja.ui;

public class BLabel extends BWidget {
    private String text = "";

    public BLabel() {
    }

    public BLabel(String text) {
        this.text = text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
    }

    public String getText() {
        return text;
    }
}
