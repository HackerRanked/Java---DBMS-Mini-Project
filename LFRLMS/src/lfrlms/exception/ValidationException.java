package lfrlms.exception;

/**
 * Purpose: Reports input that does not satisfy a business validation rule.
 * Concept: Checked exception inheritance.
 */
public class ValidationException extends LfrlmsException {
    private static final long serialVersionUID = 1L;

    public ValidationException() {
        super();
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
