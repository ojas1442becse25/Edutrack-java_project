package exception;

/**
 * Exception thrown when domain validation rules are violated
 * (e.g., negative credits, invalid CGPA range, blank required fields).
 */
public class InvalidDataException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
