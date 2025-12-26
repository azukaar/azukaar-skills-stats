package com.azukaar.ass;

import java.util.ArrayList;
import java.util.List;

public class Utils {
  public static String toDisplayString(String raw) {
    // Simplify attribute name for display
    String displayAttribute = raw.substring(raw.lastIndexOf(':') + 1)
        .replace('_', ' ')
        .replace('.', ' ');

    // capitalize first letter of each word
    displayAttribute = displayAttribute.substring(0, 1).toUpperCase() + displayAttribute.substring(1);

    return displayAttribute;
  }

  /**
   * Wrap text to fit within a maximum character width per line
   */
  public static List<String> wrapText(String text, int maxWidth) {
    List<String> lines = new ArrayList<>();
    if (text == null || text.isEmpty()) {
      return lines;
    }

    String[] words = text.split(" ");
    StringBuilder currentLine = new StringBuilder();

    for (String word : words) {
      if (currentLine.length() == 0) {
        currentLine.append(word);
      } else if (currentLine.length() + 1 + word.length() <= maxWidth) {
        currentLine.append(" ").append(word);
      } else {
        lines.add(currentLine.toString());
        currentLine = new StringBuilder(word);
      }
    }

    if (currentLine.length() > 0) {
      lines.add(currentLine.toString());
    }

    return lines;
  }
}
