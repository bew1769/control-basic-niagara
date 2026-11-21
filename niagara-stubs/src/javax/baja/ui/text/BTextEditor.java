package javax.baja.ui.text;

import javax.baja.ui.BWidget;

public class BTextEditor extends BWidget {
    private String text;
    private BSyntax syntax;

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

    public void setSyntax(BSyntax syntax) {
        this.syntax = syntax;
    }

    public BSyntax getSyntax() {
        return syntax;
    }

    public void setEditable(boolean editable) {
    }
}
