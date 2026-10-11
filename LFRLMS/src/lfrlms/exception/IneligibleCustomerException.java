package lfrlms.exception;

/**
 * Purpose: Reports that a customer does not meet rental or lease eligibility rules.
 * Concept: Checked exception inheritance.
 */
public class IneligibleCustomerException extends LfrlmsException {
    private static final long serialVersionUID = 1L;

    public IneligibleCustomerException() {
        super();
    }

    public IneligibleCustomerException(String message) {
        super(message);
    }

    public IneligibleCustomerException(String message, Throwable cause) {
        super(message, cause);
    }
}
