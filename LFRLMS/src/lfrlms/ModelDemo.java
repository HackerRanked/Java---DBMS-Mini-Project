package lfrlms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import lfrlms.model.Billable;
import lfrlms.model.CorporateCustomer;
import lfrlms.model.Customer;
import lfrlms.model.IndividualCustomer;
import lfrlms.model.Lease;
import lfrlms.model.Rental;
import lfrlms.util.Constants;

/**
 * Purpose: Demonstrates customer inheritance and common billable behavior.
 * Concept: ArrayList, inheritance, abstract classes and polymorphism.
 */
public class ModelDemo {
    public static void main(String[] args) {
        ArrayList<Customer> customers = new ArrayList<Customer>();
        customers.add(new IndividualCustomer(1, "Aarav", "Mehta",
                "aarav@example.com", "9876543210", "MG Road", "Pune", "411001",
                LocalDate.now().minusYears(30), "MH1420110012345",
                LocalDate.now().plusYears(2)));
        customers.add(new CorporateCustomer(2, "Mira", "Shah",
                "mira@example.com", "9876543211", "Park Street", "Mumbai", "400001",
                "Aster Mobility Pvt Ltd", "27ABCDE1234F1Z5"));

        System.out.println("Customer polymorphism:");
        for (Customer customer : customers) {
            System.out.println(customer.getCustomerType() + ": "
                    + customer.getDisplayName());
        }

        ArrayList<Billable> billables = new ArrayList<Billable>();
        LocalDateTime pickupTime = LocalDateTime.now().minusHours(2);
        billables.add(new Rental(11, 1, 1, null, pickupTime, pickupTime.plusDays(2),
                pickupTime, null, Integer.valueOf(12000), null, "Clean", null,
                new BigDecimal("12000.00"), null, Constants.RENTAL_ACTIVE));
        billables.add(new Lease(3, 2, 2, LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(5), new BigDecimal("250000.00"),
                new BigDecimal("50000.00"), Constants.LEASE_ACTIVE));

        System.out.println();
        System.out.println("Billable polymorphism:");
        for (Billable billable : billables) {
            System.out.println("Calculated charge: " + billable.calculateCharge());
        }
    }
}
