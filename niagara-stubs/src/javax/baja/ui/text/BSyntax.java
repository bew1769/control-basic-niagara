package javax.baja.ui.text;

public abstract class BSyntax extends javax.baja.sys.BObject {
    public static final int STYLE_PLAIN = 0;
    public static final int STYLE_KEYWORD = 1;
    public static final int STYLE_COMMENT = 2;
    public static final int STYLE_STRING = 3;
    public static final int STYLE_NUMBER = 4;
    public static final int STYLE_ERROR = 5;

    public interface StyleSink {
        void run(int start, int end, int style);
    }

    public abstract void colorLine(int lineIndex, String lineText, StyleSink sink);
}
