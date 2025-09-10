// controller/CustomerController.java
package controller;

import model.entity.Customer;
import model.entity.User;
import model.service.CustomerService;
import view.CustomerView;

import java.util.Optional;

public class CustomerController {
    private final CustomerService customerService;
    private final CustomerView customerView;
    private Customer currentCustomer;

    public CustomerController() {
        this.customerService = new CustomerService();
        this.customerView = new CustomerView();
        this.currentCustomer = null;
    }

    /**
     * Handle customer flow after login
     * @param user The logged-in user
     * @param authController Reference to auth controller for linking
     * @return true to continue session, false to logout
     */
    public boolean handleCustomerFlow(User user, AuthController authController) {
        try {
            // Step 1: Check if customer profile exists
            if (user.getCustomerId() != null) {
                // Customer ID exists, load customer profile
                Optional<Customer> customerOpt = customerService.findCustomerById(user.getCustomerId());

                if (customerOpt.isPresent()) {
                    currentCustomer = customerOpt.get();
                    customerView.showWelcomeBack(currentCustomer);
                    customerView.waitForEnter();
                } else {
                    // Customer ID exists but customer not found (data integrity issue)
                    customerView.showErrorMessage("Customer profile data not found. Please contact support.");
                    return false;
                }
            } else {
                // No customer profile, need to create one
                customerView.showInfoMessage("Welcome! To use banking services, please complete your customer profile.");
                customerView.waitForEnter();

                currentCustomer = createCustomerProfile();
                if (currentCustomer == null) {
                    customerView.showErrorMessage("Failed to create customer profile!");
                    return false;
                }

                // Link the user to the customer
                boolean linked = authController.linkUserToCustomer(currentCustomer.getId());
                if (!linked) {
                    customerView.showErrorMessage("Failed to link profile to your account!");
                    return false;
                }

                customerView.showSuccessMessage("Profile linked successfully!");
                customerView.waitForEnter();
            }

            // Step 2: Show customer main menu
            return handleCustomerMainMenu();

        } catch (Exception e) {
            customerView.showErrorMessage("An error occurred: " + e.getMessage());
            e.printStackTrace();
            customerView.waitForEnter();
            return false;
        }
    }

    /**
     * Handle customer main menu
     */
    private boolean handleCustomerMainMenu() {
        while (true) {
            try {
                int choice = customerView.showCustomerMainMenu(currentCustomer);

                switch (choice) {
                    case 0:
                        // Logout
                        return false;
                    case 1:
                        handleAccountManagement();
                        break;
                    case 2:
                        handleMakeTransaction();
                        break;
                    case 3:
                        handlePayBills();
                        break;
                    case 4:
                        handleTransactionHistory();
                        break;
                    case 5:
                        handleAccountFreeze();
                        break;
                    case 6:
                        handleLoanApplication();
                        break;
                    case 7:
                        handleManageFavorites();
                        break;
                    case 8:
                        handleUpdateProfile();
                        break;
                    default:
                        customerView.showErrorMessage("Invalid choice! Please try again.");
                        customerView.waitForEnter();
                }

            } catch (Exception e) {
                customerView.showErrorMessage("An error occurred: " + e.getMessage());
                customerView.waitForEnter();
            }
        }
    }

    /**
     * Create customer profile
     */
    private Customer createCustomerProfile() {
        try {
            Customer customerData = customerView.getCustomerProfileData();
            if (customerData == null) {
                return null;
            }

            // Show profile summary and confirm
            if (!customerView.confirmProfileUpdate(customerData)) {
                customerView.showInfoMessage("Profile creation cancelled.");
                return null;
            }

            customerView.showLoading("Creating customer profile");
            boolean success = customerService.createCustomer(customerData);

            if (success) {
                customerView.showSuccessMessage("Customer profile created successfully!");
                return customerData;
            } else {
                customerView.showErrorMessage("Failed to create customer profile!");
                return null;
            }

        } catch (Exception e) {
            customerView.showErrorMessage("Error creating customer profile: " + e.getMessage());
            return null;
        }
    }

    // ========== MENU HANDLERS (Placeholders for future phases) ==========

    private void handleAccountManagement() {
        customerView.showInfoMessage("Account Management - Coming in Phase 3!");
        customerView.showInfoMessage("Here you will be able to:");
        customerView.showInfoMessage("• View your accounts");
        customerView.showInfoMessage("• Create new accounts");
        customerView.showInfoMessage("• Manage account settings");
        customerView.waitForEnter();
    }

    private void handleMakeTransaction() {
        customerView.showInfoMessage("Make Transaction - Coming in Phase 4!");
        customerView.showInfoMessage("Here you will be able to:");
        customerView.showInfoMessage("• Transfer money between accounts");
        customerView.showInfoMessage("• Send money to other accounts");
        customerView.showInfoMessage("• Deposit and withdraw funds");
        customerView.waitForEnter();
    }

    private void handlePayBills() {
        customerView.showInfoMessage("Pay Bills - Coming in Phase 5!");
        customerView.showInfoMessage("Here you will be able to:");
        customerView.showInfoMessage("• Pay utility bills");
        customerView.showInfoMessage("• Pay subscriptions");
        customerView.showInfoMessage("• Schedule recurring payments");
        customerView.waitForEnter();
    }

    private void handleTransactionHistory() {
        customerView.showInfoMessage("Transaction History - Coming in Phase 4!");
        customerView.showInfoMessage("Here you will be able to:");
        customerView.showInfoMessage("• View all your transactions");
        customerView.showInfoMessage("• Filter by date and type");
        customerView.showInfoMessage("• Export transaction reports");
        customerView.waitForEnter();
    }

    private void handleAccountFreeze() {
        customerView.showInfoMessage("Account Freeze - Coming in Phase 4!");
        customerView.showInfoMessage("Here you will be able to:");
        customerView.showInfoMessage("• Request to freeze your accounts");
        customerView.showInfoMessage("• View freeze requests status");
        customerView.showInfoMessage("• Emergency account protection");
        customerView.waitForEnter();
    }

    private void handleLoanApplication() {
        customerView.showInfoMessage("Loan Application - Coming in Phase 6!");
        customerView.showInfoMessage("Here you will be able to:");
        customerView.showInfoMessage("• Apply for personal loans");
        customerView.showInfoMessage("• View loan status");
        customerView.showInfoMessage("• Make loan payments");
        customerView.waitForEnter();
    }

    private void handleManageFavorites() {
        customerView.showInfoMessage("Manage Favorites - Coming in Phase 5!");
        customerView.showInfoMessage("Here you will be able to:");
        customerView.showInfoMessage("• Add favorite accounts");
        customerView.showInfoMessage("• Manage favorite payees");
        customerView.showInfoMessage("• Quick transfer to favorites");
        customerView.waitForEnter();
    }

    private void handleUpdateProfile() {
        try {
            customerView.showInfoMessage("Current profile:");
            customerView.displayCustomerProfile(currentCustomer);

            customerView.showInfoMessage("Profile update feature - Coming soon!");
            customerView.showInfoMessage("For now, please contact support to update your profile.");
            customerView.waitForEnter();

        } catch (Exception e) {
            customerView.showErrorMessage("Error accessing profile: " + e.getMessage());
            customerView.waitForEnter();
        }
    }

    // ========== GETTERS ==========

    public Customer getCurrentCustomer() {
        return currentCustomer;
    }

    public void clearCurrentCustomer() {
        this.currentCustomer = null;
    }
}