package javax.baja.ui.text;

import javax.baja.ui.BWidget;

public class BTextEditor extends BWidget {
    private String text;
    private TextParser parser;

    public BTextEditor() {
        this("", true);
    }

    public BTextEditor(String text, boolean editable) {
        this.text = text == null ? "" : text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
    }

    public void setParser(TextParser parser) {
        this.parser = parser;
    }

    public TextParser getParser() {
        return parser;
    }

    public Position getCaretPosition() {
        return new Position();
    }

    public void moveCaretPosition(Position position) {
    }

    public void setEditable(boolean editable) {
    }
}
