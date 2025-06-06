package com.azukaar.ass;

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
}
