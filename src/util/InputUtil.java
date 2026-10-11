package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The only place in the project that reads from the keyboard.
 * It reads System.in byte by byte, so no Scanner or BufferedReader is needed.
 */
public class InputUtil {

    private InputUtil() {
    }

    /** Reads one line. Exits cleanly if the input stream is closed. */
    public static String readLine() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boolean gotAny = false;
        try {
            int c;
            while ((c = System.in.read()) != -1) {
                gotAny = true;
                if (c == '\n') {
                    break;
                }
                if (c != '\r') {
                    buffer.write(c);
                }
            }
        } catch (IOException e) {
            gotAny = false;
        }
        if (!gotAny) {
            System.out.println("\nInput closed. Exiting.");
            System.exit(0);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    /** Prints a prompt and returns the trimmed answer. */
    public static String prompt(String message) {
        System.out.print(message);
        System.out.flush();
        return readLine().trim();
    }

    /** Reads lines until a line containing only END. Returns the lines joined with \n. */
    public static String readMultiLine() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        while (true) {
            String line = readLine();
            if (line.trim().equals("END")) {
                break;
            }
            if (!first) {
                sb.append('\n');
            }
            sb.append(line);
            first = false;
        }
        return sb.toString();
    }
}
