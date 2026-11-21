package com.controlbasic.ui;

import javax.baja.ui.text.BSyntax;

import com.controlbasic.core.Highlight;

/**
 * Syntax coloring for {@link javax.baja.ui.text.BTextEditor}.
 *
 * <p>This follows the same extension point as {@code javax.baja.ui.text.BJavaSyntax}:
 * a {@link BSyntax} installed with {@code BTextEditor.setSyntax}. Niagara 4.13 and
 * later color a line at a time. If a local {@code bajaui} declares a different abstract
 * method on {@code BSyntax}, change only {@link #colorLine} so it delegates to
 * {@link Highlight}, which is what the runtime parser uses.
 */
public class BControlBasicSyntax extends BSyntax {
  private int errorLine = -1;

  public void setErrorLine(int errorLine) {
    this.errorLine = errorLine;
  }

  @Override
  public void colorLine(int lineIndex, String lineText, StyleSink sink) {
    int lineNumber = lineIndex + 1;
    java.util.List<Highlight.Run> runs = Highlight.colorLine(lineText, lineNumber, errorLine);
    for (int i = 0; i < runs.size(); i++) {
      Highlight.Run run = runs.get(i);
      int style = STYLE_PLAIN;
      if (run.style == Highlight.KEYWORD) {
        style = STYLE_KEYWORD;
      } else if (run.style == Highlight.COMMENT) {
        style = STYLE_COMMENT;
      } else if (run.style == Highlight.STRING) {
        style = STYLE_STRING;
      } else if (run.style == Highlight.NUMBER) {
        style = STYLE_NUMBER;
      } else if (run.style == Highlight.ERROR) {
        style = STYLE_ERROR;
      }
      sink.run(run.start, run.end, style);
    }
  }

  public static BControlBasicSyntax make() {
    return new BControlBasicSyntax();
  }
}
