package exception;

/** Thrown when user input or file data fails validation. */
public class InvalidInputException extends Exception {
    public InvalidInputException(String message) {
        super(message);
    }
}
