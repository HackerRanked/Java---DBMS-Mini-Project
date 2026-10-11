package lfrlms.exception;

/**
 * Purpose: Base checked exception for errors the application can explain.
 * Concept: Exception inheritance.
 */
public class LfrlmsException extends Exception {
    private static final long serialVersionUID = 1L;

    public LfrlmsException() {
        super();
    }

    public LfrlmsException(String message) {
        super(message);
    }

    public LfrlmsException(String message, Throwable cause) {
        super(message, cause);
    }
}
