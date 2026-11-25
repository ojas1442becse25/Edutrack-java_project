package exception;

public class EligibilityException extends Exception {
    private static final long serialVersionUID = 1L;

    public EligibilityException(String message) {
        super(message);
    }

    public EligibilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
