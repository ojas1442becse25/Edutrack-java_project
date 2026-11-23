package exception;

/**
 * Exception thrown when an applicant or entity fails domain eligibility criteria
 * (e.g., student CGPA below minimum drive criteria, department mismatch, or suspension).
 */
public class EligibilityException extends Exception {
    private static final long serialVersionUID = 1L;

    public EligibilityException(String message) {
        super(message);
    }

    public EligibilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
