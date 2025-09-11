// view/AccountView.java
package view;

import model.entity.Account;
import model.entity.AccountType;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AccountView {
    private final Scanner scanner;

    public AccountView() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Show account management menu
     */
    public int showAccountManagementMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("           ACCOUNT MANAGEMENT");
        System.out.println("=".repeat(50));
        System.out.println("1. View My Accounts");
        System.out.println("2. Create New Account");
        System.out.println("3. Account Details");
        System.out.println("4. Freeze/Unfreeze Account");
        System.out.println("5. Account Limits Summary");
        System.out.println("0. Back to Main Menu");
        System.out.print("Choose an option: ");

        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1; // Invalid input
        }
    }

    /**
     * Display customer's accounts in a formatted list
     */
    public void displayAccountsList(List<Account> accounts) {
        if (accounts.isEmpty()) {
            System.out.println("\n📝 You don't have any accounts yet.");
            System.out.println("💡 Create your first account to start banking!");
            return;
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("                  YOUR ACCOUNTS");
        System.out.println("=".repeat(60));

        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            System.out.printf("%d. %s\n", i + 1, account.getAccountDisplay());
            System.out.printf("   Type: %s | Balance: %s | Status: %s\n",
                    account.getAccountTypeName(),
                    account.getFormattedBalance(),
                    account.getStatus());
            System.out.println("   " + "-".repeat(50));
        }
    }

    /**
     * Show detailed account information
     */
    public void displayAccountDetails(Account account) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                ACCOUNT DETAILS");
        System.out.println("=".repeat(60));
        System.out.println("Account Name    : " + account.getAccountName());
        System.out.println("Account Number  : " + account.getFormattedAccountNo());
        System.out.println("Account Type    : " + account.getAccountTypeName());
        System.out.println("Currency        : " + account.getAccountCurrency());
        System.out.println("Balance         : " + account.getFormattedBalance());
        System.out.println("Over Limit      : " + account.getAccountCurrency() + " " +
                String.format("%,.2f", account.getOverLimit()));
        System.out.println("Status          : " + account.getStatus());
        System.out.println("=".repeat(60));
    }

    /**
     * Get account selection from user
     */
    public int selectAccount(List<Account> accounts) {
        if (accounts.isEmpty()) {
            showInfoMessage("No accounts available!");
            return -1;
        }

        displayAccountsList(accounts);
        System.out.print("\nSelect account number (1-" + accounts.size() + "): ");

        try {
            int selection = Integer.parseInt(scanner.nextLine().trim());
            if (selection >= 1 && selection <= accounts.size()) {
                return selection - 1; // Convert to 0-based index
            }
        } catch (NumberFormatException e) {
            // Invalid input
        }

        showErrorMessage("Invalid account selection!");
        return -1;
    }

    /**
     * Show new account creation menu
     */
    public String[] getNewAccountData(List<String> availableTypes) {
        if (availableTypes.isEmpty()) {
            showInfoMessage("You have reached the maximum limit for all account types!");
            return null;
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("           CREATE NEW ACCOUNT");
        System.out.println("=".repeat(50));
        System.out.println("Available account types:");

        for (int i = 0; i < availableTypes.size(); i++) {
            System.out.println((i + 1) + ". " + availableTypes.get(i));
        }

        System.out.print("\nSelect account type (1-" + availableTypes.size() + "): ");

        try {
            int selection = Integer.parseInt(scanner.nextLine().trim());
            if (selection >= 1 && selection <= availableTypes.size()) {
                String selectedType = availableTypes.get(selection - 1);

                // Parse the selected type to extract account type and currency
                String accountType;
                String currency;

                if (selectedType.contains("Saving")) {
                    accountType = "Saving";
                    if (selectedType.contains("USD")) {
                        currency = "USD";
                    } else if (selectedType.contains("KHR")) {
                        currency = "KHR";
                    } else {
                        currency = "USD"; // Default
                    }
                } else if (selectedType.contains("Checking")) {
                    accountType = "Checking";
                    currency = getCurrencyForNonSavingAccount();
                } else if (selectedType.contains("Fixed")) {
                    accountType = "Fixed";
                    currency = getCurrencyForNonSavingAccount();
                } else {
                    showErrorMessage("Invalid account type!");
                    return null;
                }

                return new String[]{accountType, currency};
            }
        } catch (NumberFormatException e) {
            showErrorMessage("Invalid selection!");
        }

        return null;
    }

    /**
     * Get currency selection for non-saving accounts
     */
    private String getCurrencyForNonSavingAccount() {
        System.out.println("\nSelect currency:");
        System.out.println("1. USD (US Dollar)");
        System.out.println("2. KHR (Khmer Riel)");
        System.out.print("Choose currency (1-2): ");

        try {
            int currencyChoice = Integer.parseInt(scanner.nextLine().trim());
            if (currencyChoice == 1) {
                return "USD";
            } else if (currencyChoice == 2) {
                return "KHR";
            }
        } catch (NumberFormatException e) {
            // Invalid input
        }

        showInfoMessage("Invalid selection. Defaulting to USD.");
        return "USD";
    }

    /**
     * Confirm account creation
     */
    public boolean confirmAccountCreation(String accountType, String currency) {
        System.out.println("\n=== CONFIRM ACCOUNT CREATION ===");
        System.out.println("Account Type: " + accountType);
        System.out.println("Currency: " + currency);
        System.out.print("\nConfirm creation? (y/n): ");

        String response = scanner.nextLine().trim().toLowerCase();
        return response.equals("y") || response.equals("yes");
    }

    /**
     * Display account limits summary
     */
    public void displayAccountLimits(Map<String, String> limits) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("           ACCOUNT LIMITS SUMMARY");
        System.out.println("=".repeat(50));

        limits.forEach((accountType, limit) -> {
            String status = limit.equals("0/1") || limit.equals("0/2") ? "✅ Available" :
                    limit.endsWith("/1") && limit.startsWith("1") ? "❌ Limit reached" :
                            "❌ Limit reached";
            System.out.printf("%-15s: %s %s\n", accountType, limit, status);
        });

        System.out.println("=".repeat(50));
        System.out.println("💡 Limits: Saving (1 USD + 1 KHR), Checking (1), Fixed (1)");
    }

    /**
     * Get account number input from user
     */
    public String getAccountNumberInput() {
        System.out.print("Enter account number (xxx xxx xxx or xxxxxxxxx): ");
        return scanner.nextLine().trim();
    }

    /**
     * Show freeze/unfreeze menu
     */
    public boolean getFreezeAction() {
        System.out.println("\nAccount Freeze Options:");
        System.out.println("1. Freeze Account (Block all transactions)");
        System.out.println("2. Unfreeze Account (Activate account)");
        System.out.print("Choose action (1-2): ");

        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            return choice == 1; // true for freeze, false for unfreeze
        } catch (NumberFormatException e) {
            showErrorMessage("Invalid choice!");
            return false;
        }
    }

    /**
     * Confirm freeze action
     */
    public boolean confirmFreezeAction(String accountNumber, boolean isFreeze) {
        String action = isFreeze ? "FREEZE" : "UNFREEZE";
        System.out.println("\n=== CONFIRM " + action + " ACTION ===");
        System.out.println("Account: " + accountNumber);
        System.out.println("Action: " + action + " ACCOUNT");

        if (isFreeze) {
            System.out.println("⚠️ WARNING: Freezing will block ALL transactions on this account!");
        } else {
            System.out.println("✅ This will activate the account for normal operations.");
        }

        System.out.print("\nConfirm " + action.toLowerCase() + "? (y/n): ");
        String response = scanner.nextLine().trim().toLowerCase();
        return response.equals("y") || response.equals("yes");
    }

    /**
     * Display success message
     */
    public void showSuccessMessage(String message) {
        System.out.println("✅ " + message);
    }

    /**
     * Display error message
     */
    public void showErrorMessage(String message) {
        System.out.println("❌ " + message);
    }

    /**
     * Display info message
     */
    public void showInfoMessage(String message) {
        System.out.println("ℹ️ " + message);
    }

    /**
     * Display loading message
     */
    public void showLoading(String message) {
        System.out.println("⏳ " + message + "...");
    }

    /**
     * Wait for user to press Enter
     */
    public void waitForEnter() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Clear screen (simple version)
     */
    public void clearScreen() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    /**
     * Show account creation success
     */
    public void showAccountCreationSuccess(String accountNumber, String accountName) {
        System.out.println("\n🎉 ACCOUNT CREATED SUCCESSFULLY! 🎉");
        System.out.println("=".repeat(50));
        System.out.println("Account Name: " + accountName);
        System.out.println("Account Number: " + accountNumber);
        System.out.println("Status: Active");
        System.out.println("Balance: $0.00 / ៛0");
        System.out.println("=".repeat(50));
        System.out.println("💡 Your account is now ready for banking operations!");
    }
}