package exception;

/**
 * Exception thrown when attempting to insert or update a record that violates
 * a UNIQUE database constraint (e.g., duplicate email, roll number, or class code).
 * Typically mapped from SQLState "23000" or MySQL Error Code 1062.
 */
public class DuplicateRecordException extends Exception {
    private static final long serialVersionUID = 1L;

    public DuplicateRecordException(String message) {
        super(message);
    }

    public DuplicateRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}
