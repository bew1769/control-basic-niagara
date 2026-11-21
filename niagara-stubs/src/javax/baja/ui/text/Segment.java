package javax.baja.ui.text;

public class Segment {
    public static final int TEXT = 0;
    public static final int NUMBER_LITERAL = 1;
    public static final int IDENTIFIER = 2;
    public static final int KEYWORD = 3;
    public static final int PREPROCESSOR = 4;
    public static final int STRING_DELIMITER = 5;
    public static final int LINE_COMMENT = 6;
    public static final int NEWLINE = 7;

    public Segment(int type, int offset, int length) {
    }
}
