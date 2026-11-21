package com.controlbasic.ui;

import java.util.ArrayList;
import java.util.List;

import javax.baja.ui.text.Line;
import javax.baja.ui.text.Segment;
import javax.baja.ui.text.TextParser;

import com.controlbasic.core.Highlight;

/**
 * Colors Control Basic in a {@link javax.baja.ui.text.BTextEditor}.
 *
 * Niagara 4.15 does not have {@code BSyntax}. Editors take a {@link TextParser},
 * the same hook {@code javax.baja.ui.text.parsers.JavaParser} uses. Keyword,
 * comment, number, and string runs come from the core lexer. An error line is
 * marked with {@link Segment#PREPROCESSOR} because {@link Segment} has no
 * error style; the view also prints the diagnostic under the editor.
 */
public class BControlBasicSyntax extends TextParser {
  private int errorLine = -1;

  public void setErrorLine(int errorLine) {
    this.errorLine = errorLine;
  }

  public static BControlBasicSyntax make() {
    return new BControlBasicSyntax();
  }

  @Override
  public synchronized Line[] parse(char[] buffer) {
    if (buffer == null) {
      buffer = new char[0];
    }
    List<Line> lines = new ArrayList<Line>();
    int start = 0;
    int lineNumber = 1;
    int i = 0;
    while (i <= buffer.length) {
      boolean atEnd = i == buffer.length;
      if (!atEnd && buffer[i] != '\n' && buffer[i] != '\r') {
        i++;
        continue;
      }
      int length = i - start;
      if (!atEnd && buffer[i] == '\r' && i + 1 < buffer.length && buffer[i + 1] == '\n') {
        length += 2;
        i++;
      } else if (!atEnd) {
        length += 1;
      }
      lines.add(line(buffer, start, length, lineNumber));
      if (atEnd) {
        break;
      }
      i++;
      start = i;
      lineNumber++;
    }
    if (lines.isEmpty()) {
      lines.add(new Line(new char[0], new Segment[] {
          new Segment(Segment.TEXT, 0, 0)
      }));
    }
    return lines.toArray(new Line[lines.size()]);
  }

  private Line line(char[] buffer, int start, int length, int lineNumber) {
    if (length < 0) {
      length = 0;
    }
    String raw = new String(buffer, start, length);
    boolean newline = raw.endsWith("\n") || raw.endsWith("\r");
    String text = raw;
    if (raw.endsWith("\r\n")) {
      text = raw.substring(0, raw.length() - 2);
    } else if (newline) {
      text = raw.substring(0, raw.length() - 1);
    }
    List<Highlight.Run> runs = Highlight.colorLine(text, lineNumber, errorLine);
    List<Segment> segments = new ArrayList<Segment>();
    if (lineNumber == errorLine && errorLine > 0) {
      segments.add(new Segment(Segment.PREPROCESSOR, 0, text.length()));
    } else {
      int cursor = 0;
      for (int r = 0; r < runs.size(); r++) {
        Highlight.Run run = runs.get(r);
        if (run.start > cursor) {
          segments.add(new Segment(Segment.TEXT, cursor, run.start - cursor));
        }
        int type = segmentType(run.style);
        int segLen = Math.max(0, run.end - run.start);
        if (segLen > 0) {
          segments.add(new Segment(type, run.start, segLen));
        }
        cursor = Math.max(cursor, run.end);
      }
      if (cursor < text.length()) {
        segments.add(new Segment(Segment.TEXT, cursor, text.length() - cursor));
      }
    }
    if (newline) {
      int nlLen = raw.endsWith("\r\n") ? 2 : 1;
      segments.add(new Segment(Segment.NEWLINE, text.length(), nlLen));
    }
    if (segments.isEmpty()) {
      segments.add(new Segment(Segment.TEXT, 0, 0));
    }
    return new Line(buffer, start, length, segments);
  }

  private static int segmentType(int style) {
    if (style == Highlight.KEYWORD) {
      return Segment.KEYWORD;
    }
    if (style == Highlight.COMMENT) {
      return Segment.LINE_COMMENT;
    }
    if (style == Highlight.STRING) {
      return Segment.STRING_DELIMITER;
    }
    if (style == Highlight.NUMBER) {
      return Segment.NUMBER_LITERAL;
    }
    if (style == Highlight.ERROR) {
      return Segment.PREPROCESSOR;
    }
    return Segment.IDENTIFIER;
  }
}
