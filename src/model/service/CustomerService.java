// model/service/CustomerService.java
package model.service;

import model.entity.Customer;
import model.repository.CustomerRepository;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService() {
        this.customerRepository = new CustomerRepository();
    }

    /**
     * Create a new customer profile
     */
    public boolean createCustomer(Customer customer) {
        // Validate customer data
        String validationError = validateCustomerData(customer);
        if (validationError != null) {
            System.out.println("❌ Validation Error: " + validationError);
            return false;
        }

        // Check if phone number already exists
        if (customerRepository.phoneNumberExists(customer.getPhoneNumber(), null)) {
            System.out.println("❌ Phone number already exists!");
            return false;
        }

        // Check if email already exists
        if (customerRepository.emailExists(customer.getEmail(), null)) {
            System.out.println("❌ Email already exists!");
            return false;
        }

        // Create customer
        boolean success = customerRepository.create(customer);
        if (success) {
            System.out.println("✅ Customer profile created successfully!");
            return true;
        } else {
            System.out.println("❌ Failed to create customer profile!");
            return false;
        }
    }

    /**
     * Find customer by ID
     */
    public Optional<Customer> findCustomerById(Integer id) {
        return customerRepository.findById(id);
    }

    /**
     * Find customer by phone number
     */
    public Optional<Customer> findCustomerByPhoneNumber(String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber);
    }

    /**
     * Find customer by email
     */
    public Optional<Customer> findCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    /**
     * Update customer profile
     */
    public boolean updateCustomer(Customer customer) {
        // Validate customer data
        String validationError = validateCustomerData(customer);
        if (validationError != null) {
            System.out.println("❌ Validation Error: " + validationError);
            return false;
        }

        // Check if phone number already exists (excluding current customer)
        if (customerRepository.phoneNumberExists(customer.getPhoneNumber(), customer.getId())) {
            System.out.println("❌ Phone number already exists!");
            return false;
        }

        // Check if email already exists (excluding current customer)
        if (customerRepository.emailExists(customer.getEmail(), customer.getId())) {
            System.out.println("❌ Email already exists!");
            return false;
        }

        // Update customer
        boolean success = customerRepository.update(customer);
        if (success) {
            System.out.println("✅ Customer profile updated successfully!");
            return true;
        } else {
            System.out.println("❌ Failed to update customer profile!");
            return false;
        }
    }

    /**
     * Get all customers (for admin use)
     */
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    /**
     * Delete customer (soft delete)
     */
    public boolean deleteCustomer(Integer customerId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isEmpty()) {
            System.out.println("❌ Customer not found!");
            return false;
        }

        boolean success = customerRepository.delete(customerId);
        if (success) {
            System.out.println("✅ Customer deleted successfully!");
            return true;
        } else {
            System.out.println("❌ Failed to delete customer!");
            return false;
        }
    }

    /**
     * Check if customer exists
     */
    public boolean customerExists(Integer customerId) {
        return customerRepository.findById(customerId).isPresent();
    }

    /**
     * Validate PIN format
     */
    public boolean validatePin(String pin) {
        if (pin == null || pin.length() != 6) {
            return false;
        }

        // Check if all characters are digits
        for (char c : pin.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        // Check if not all same digits (weak PIN)
        char firstDigit = pin.charAt(0);
        boolean allSame = true;
        for (char c : pin.toCharArray()) {
            if (c != firstDigit) {
                allSame = false;
                break;
            }
        }

        return !allSame; // Return false if all digits are the same
    }

    /**
     * Validate phone number format
     */
    public boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // Remove spaces and common separators
        String cleanPhone = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");

        // Check if it's digits only and reasonable length
        return cleanPhone.matches("\\d{8,15}");
    }

    /**
     * Validate email format
     */
    public boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Basic email validation
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Check if customer is adult (18+ years old)
     */
    public boolean isAdult(LocalDate dob) {
        if (dob == null) {
            return false;
        }

        return Period.between(dob, LocalDate.now()).getYears() >= 18;
    }

    /**
     * Validate customer data
     */
    private String validateCustomerData(Customer customer) {
        // Required fields validation
        if (customer.getFullName() == null || customer.getFullName().trim().isEmpty()) {
            return "Full name is required";
        }

        if (customer.getDob() == null) {
            return "Date of birth is required";
        }

        if (!isAdult(customer.getDob())) {
            return "Customer must be at least 18 years old";
        }

        if (customer.getPhoneNumber() == null || customer.getPhoneNumber().trim().isEmpty()) {
            return "Phone number is required";
        }

        if (!validatePhoneNumber(customer.getPhoneNumber())) {
            return "Invalid phone number format";
        }

        if (customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
            return "Email is required";
        }

        if (!validateEmail(customer.getEmail())) {
            return "Invalid email format";
        }

        if (customer.getPin() == null || customer.getPin().trim().isEmpty()) {
            return "PIN is required";
        }

        if (!validatePin(customer.getPin())) {
            return "PIN must be 6 digits and not all same number";
        }

        // Optional fields validation
        if (customer.getAddress() != null && customer.getAddress().trim().length() > 150) {
            return "Address is too long (max 150 characters)";
        }

        if (customer.getFullName().trim().length() > 100) {
            return "Full name is too long (max 100 characters)";
        }

        return null; // No validation errors
    }

    /**
     * Get customer display summary
     */
    public String getCustomerSummary(Customer customer) {
        if (customer == null) {
            return "No customer information";
        }

        return customer.getSummary();
    }
}