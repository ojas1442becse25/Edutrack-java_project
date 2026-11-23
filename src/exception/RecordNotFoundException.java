package exception;

/**
 * Exception thrown when a requested database record is not found
 * during lookup, update, or deletion operations.
 */
public class RecordNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;

    public RecordNotFoundException(String message) {
        super(message);
    }

    public RecordNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
