package lfrlms.model;

import java.math.BigDecimal;

/**
 * Purpose: Gives contracts a common way to calculate their charge.
 * Concept: Interface and polymorphism.
 */
public interface Billable {
    BigDecimal calculateCharge();
}
