// controller/AccountController.java - Enhanced with Initial Deposit
package controller;

import model.entity.Account;
import model.entity.Customer;
import model.service.AccountService;
import view.AccountView;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AccountController {
    private final AccountService accountService;
    private final AccountView accountView;

    public AccountController() {
        this.accountService = new AccountService();
        this.accountView = new AccountView();
    }

    /**
     * Main account management handler
     * @param customer Current customer
     * @return true to continue, false to go back
     */
    public boolean handleAccountManagement(Customer customer) {
        if (customer == null) {
            accountView.showErrorMessage("Customer information not found!");
            return false;
        }

        while (true) {
            try {
                int choice = accountView.showAccountManagementMenu();

                switch (choice) {
                    case 0:
                        // Back to main menu
                        return true;
                    case 1:
                        handleViewAccounts(customer);
                        break;
                    case 2:
                        handleCreateAccountWithInitialDeposit(customer);
                        break;
                    case 3:
                        handleAccountDetails(customer);
                        break;
                    case 4:
                        handleFreezeUnfreeze(customer);
                        break;
                    case 5:
                        handleAccountLimits(customer);
                        break;
                    default:
                        accountView.showErrorMessage("Invalid choice! Please try again.");
                        accountView.waitForEnter();
                }

            } catch (Exception e) {
                accountView.showErrorMessage("An error occurred: " + e.getMessage());
                accountView.waitForEnter();
            }
        }
    }

    /**
     * Handle view customer's accounts
     */
    private void handleViewAccounts(Customer customer) {
        try {
            accountView.showLoading("Loading your accounts");
            List<Account> accounts = accountService.getCustomerAccounts(customer.getId());

            accountView.displayAccountsList(accounts);

            if (!accounts.isEmpty()) {
                // Show total summary
                int activeAccounts = (int) accounts.stream().filter(Account::isActive).count();
                int frozenAccounts = (int) accounts.stream().filter(Account::isFreeze).count();

                System.out.println("\n📊 Account Summary:");
                System.out.println("Total Accounts: " + accounts.size());
                System.out.println("Active Accounts: " + activeAccounts);
                System.out.println("Frozen Accounts: " + frozenAccounts);
            }

            accountView.waitForEnter();

        } catch (Exception e) {
            accountView.showErrorMessage("Error loading accounts: " + e.getMessage());
            accountView.waitForEnter();
        }
    }

    /**
     * Enhanced handle create new account with initial deposit
     */
    private void handleCreateAccountWithInitialDeposit(Customer customer) {
        try {
            // Get available account types
            List<String> availableTypes = accountService.getAvailableAccountTypes(customer.getId());

            if (availableTypes.isEmpty()) {
                accountView.showInfoMessage("You have reached the maximum limit for all account types!");
                accountView.showInfoMessage("Current limits: Saving (1 USD + 1 KHR), Checking (1), Fixed (1)");
                accountView.waitForEnter();
                return;
            }

            // Get account creation data with initial deposit
            AccountView.AccountCreationData accountData = accountView.getNewAccountData(availableTypes);
            if (accountData == null) {
                return; // User cancelled or invalid input
            }

            // Confirm creation
            if (!accountView.confirmAccountCreation(accountData)) {
                accountView.showInfoMessage("Account creation cancelled.");
                accountView.waitForEnter();
                return;
            }

            // Create account with initial deposit
            accountView.showLoading("Creating your " + accountData.getAccountType() + " account");
            boolean success = accountService.createAccountWithInitialDeposit(
                    customer.getId(),
                    accountData.getAccountType(),
                    accountData.getCurrency(),
                    accountData.getInitialDeposit(),
                    accountData.getMaturityDate()
            );

            if (success) {
                accountView.showSuccessMessage("Account created successfully!");

                // Show the newly created account
                List<Account> accounts = accountService.getCustomerAccounts(customer.getId());
                if (!accounts.isEmpty()) {
                    Account newAccount = accounts.get(accounts.size() - 1); // Get the last created account
                    accountView.showAccountCreationSuccess(
                            newAccount.getFormattedAccountNo(),
                            newAccount.getAccountName()
                    );

                    // Show additional info for Fixed accounts
                    if (newAccount.getMaturityDate() != null) {
                        System.out.println("🗓️ Maturity Date: " + newAccount.getMaturityDate());
                        System.out.println("⚠️ No withdrawals allowed until maturity!");
                    }
                }
            } else {
                accountView.showErrorMessage("Failed to create account. Please try again.");
            }

            accountView.waitForEnter();

        } catch (Exception e) {
            accountView.showErrorMessage("Error creating account: " + e.getMessage());
            accountView.waitForEnter();
        }
    }

    /**
     * Handle view account details
     */
    private void handleAccountDetails(Customer customer) {
        try {
            List<Account> accounts = accountService.getActiveAccounts(customer.getId());

            if (accounts.isEmpty()) {
                accountView.showInfoMessage("You don't have any active accounts.");
                accountView.waitForEnter();
                return;
            }

            // Let user select account
            int selectedIndex = accountView.selectAccount(accounts);
            if (selectedIndex == -1) {
                return; // Invalid selection
            }

            Account selectedAccount = accounts.get(selectedIndex);
            accountView.displayAccountDetails(selectedAccount);

            // Show additional Fixed account info if applicable
            if (selectedAccount.getMaturityDate() != null) {
                System.out.println("\n💰 Fixed Account Information:");
                System.out.println("Maturity Date   : " + selectedAccount.getMaturityDate());
                System.out.println("Can Withdraw    : " + (selectedAccount.isMatured() ? "✅ Yes" : "❌ No"));
                if (!selectedAccount.isMatured()) {
                    System.out.println("Days Until Maturity: " +
                            java.time.temporal.ChronoUnit.DAYS.between(
                                    java.time.LocalDate.now(),
                                    selectedAccount.getMaturityDate()
                            ) + " days");
                }
            }

            accountView.waitForEnter();

        } catch (Exception e) {
            accountView.showErrorMessage("Error loading account details: " + e.getMessage());
            accountView.waitForEnter();
        }
    }

    /**
     * Handle freeze/unfreeze account
     */
    private void handleFreezeUnfreeze(Customer customer) {
        try {
            List<Account> accounts = accountService.getCustomerAccounts(customer.getId());

            if (accounts.isEmpty()) {
                accountView.showInfoMessage("You don't have any accounts to freeze/unfreeze.");
                accountView.waitForEnter();
                return;
            }

            // Show accounts and let user select
            int selectedIndex = accountView.selectAccount(accounts);
            if (selectedIndex == -1) {
                return; // Invalid selection
            }

            Account selectedAccount = accounts.get(selectedIndex);

            // Check if account is deleted
            if (selectedAccount.isDeleted()) {
                accountView.showErrorMessage("Cannot modify deleted account!");
                accountView.waitForEnter();
                return;
            }

            // Get freeze action
            boolean freezeAction = accountView.getFreezeAction();

            // Check current status
            if (selectedAccount.isFreeze() && freezeAction) {
                accountView.showInfoMessage("Account is already frozen!");
                accountView.waitForEnter();
                return;
            }

            if (!selectedAccount.isFreeze() && !freezeAction) {
                accountView.showInfoMessage("Account is already active!");
                accountView.waitForEnter();
                return;
            }

            // Confirm action
            if (!accountView.confirmFreezeAction(selectedAccount.getFormattedAccountNo(), freezeAction)) {
                accountView.showInfoMessage("Action cancelled.");
                accountView.waitForEnter();
                return;
            }

            // Perform freeze/unfreeze
            accountView.showLoading("Updating account status");
            boolean success = accountService.updateFreezeStatus(selectedAccount.getAccountNo(), freezeAction);

            if (success) {
                String action = freezeAction ? "frozen" : "unfrozen";
                accountView.showSuccessMessage("Account has been " + action + " successfully!");
            } else {
                accountView.showErrorMessage("Failed to update account status. Please try again.");
            }

            accountView.waitForEnter();

        } catch (Exception e) {
            accountView.showErrorMessage("Error updating account status: " + e.getMessage());
            accountView.waitForEnter();
        }
    }

    /**
     * Handle account limits display
     */
    private void handleAccountLimits(Customer customer) {
        try {
            accountView.showLoading("Loading account limits");
            Map<String, String> limits = accountService.getAccountLimitsSummary(customer.getId());

            accountView.displayAccountLimits(limits);

            // Show additional info
            List<String> availableTypes = accountService.getAvailableAccountTypes(customer.getId());

            if (!availableTypes.isEmpty()) {
                System.out.println("\n✅ You can still create:");
                for (String type : availableTypes) {
                    System.out.println("   • " + type);
                }
            } else {
                System.out.println("\n❌ You have reached all account limits.");
            }

            // Show minimum deposit requirements
            System.out.println("\n💰 Minimum Initial Deposit Requirements:");
            System.out.println("   • USD Accounts: $5.00");
            System.out.println("   • KHR Accounts: ៛20,000");

            accountView.waitForEnter();

        } catch (Exception e) {
            accountView.showErrorMessage("Error loading account limits: " + e.getMessage());
            accountView.waitForEnter();
        }
    }

    /**
     * Quick method to check if customer has accounts (for other controllers)
     */
    public boolean customerHasAccounts(Integer customerId) {
        return accountService.customerHasAccounts(customerId);
    }

    /**
     * Get customer's accounts (for other controllers)
     */
    public List<Account> getCustomerAccounts(Integer customerId) {
        return accountService.getCustomerAccounts(customerId);
    }

    /**
     * Get active accounts only (for other controllers)
     */
    public List<Account> getActiveAccounts(Integer customerId) {
        return accountService.getActiveAccounts(customerId);
    }

    /**
     * Find account by number (for other controllers)
     */
    public Optional<Account> findAccountByNumber(String accountNumber) {
        return accountService.findAccountByNumber(accountNumber);
    }

    /**
     * Get account service (for advanced operations in other controllers)
     */
    public AccountService getAccountService() {
        return accountService;
    }
}