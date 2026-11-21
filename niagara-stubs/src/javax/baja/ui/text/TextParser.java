package javax.baja.ui.text;

/**
 * Compile-only stand-in. Niagara 4.15 colors a {@link BTextEditor} with a
 * {@code TextParser}, not {@code BSyntax}.
 */
public abstract class TextParser {
    public abstract Line[] parse(char[] buffer);
}
