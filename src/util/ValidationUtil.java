package util;

import exception.InvalidInputException;
import model.Item;

import java.util.HashSet;

/** Input checks done with plain String and Character methods. */
public class ValidationUtil {

    private ValidationUtil() {
    }

    /** Text must be non-blank and must not contain the file separators. */
    public static String requireText(String value, String fieldName) throws InvalidInputException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(fieldName + " cannot be empty.");
        }
        if (value.contains(Item.SEPARATOR) || value.contains(Item.NEWLINE_TOKEN)) {
            throw new InvalidInputException(fieldName + " contains a reserved sequence ("
                    + Item.SEPARATOR + " or " + Item.NEWLINE_TOKEN + ").");
        }
        return value.trim();
    }

    /** Parses an int and checks the range. */
    public static int parseInt(String text, String fieldName, int min, int max)
            throws InvalidInputException {
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new InvalidInputException(fieldName + " must be a whole number.");
        }
        if (value < min || value > max) {
            throw new InvalidInputException(fieldName + " must be between " + min + " and " + max + ".");
        }
        return value;
    }

    /** A tag may contain letters, digits, '-' and '_' only. */
    public static boolean isValidTag(String tag) {
        if (tag.isEmpty()) {
            return false;
        }
        for (int i = 0; i < tag.length(); i++) {
            char c = tag.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_')) {
                return false;
            }
        }
        return true;
    }

    /** Cleans "Loops, ARRAY , loops" into "loops,array". */
    public static String parseTags(String raw) throws InvalidInputException {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        String[] parts = raw.split(",");
        HashSet<String> seen = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            String tag = part.trim().toLowerCase();
            if (tag.isEmpty()) {
                continue;
            }
            if (!isValidTag(tag)) {
                throw new InvalidInputException("Invalid tag '" + tag
                        + "'. Use letters, digits, '-' or '_'.");
            }
            if (seen.add(tag)) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(tag);
            }
        }
        return sb.toString();
    }

    /** A file name must not try to leave its folder. */
    public static String requireFileName(String name) throws InvalidInputException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("File name cannot be empty.");
        }
        String n = name.trim();
        if (n.contains("..") || n.contains("/") || n.contains("\\")) {
            throw new InvalidInputException("File name cannot contain '..', '/' or '\\'.");
        }
        return n;
    }
}
