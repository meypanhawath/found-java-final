// view/AccountView.java - Complete Enhanced Version
package view;

import model.entity.Account;
import util.AccountUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
     * Enhanced account creation with initial deposit and maturity date
     */
    public AccountCreationData getNewAccountData(List<String> availableTypes) {
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
                    currency = selectedType.contains("USD") ? "USD" : "KHR";
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

                // Get initial deposit
                BigDecimal initialDeposit = getInitialDeposit(currency);
                if (initialDeposit == null) {
                    return null; // User cancelled or invalid amount
                }

                // Get maturity date for Fixed accounts
                LocalDate maturityDate = null;
                if ("Fixed".equals(accountType)) {
                    maturityDate = getMaturityDate();
                    if (maturityDate == null) {
                        return null; // User cancelled
                    }
                }

                return new AccountCreationData(accountType, currency, initialDeposit, maturityDate);
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
     * Get initial deposit amount with validation
     */
    private BigDecimal getInitialDeposit(String currency) {
        BigDecimal minimumDeposit = "USD".equals(currency) ?
                new BigDecimal("5.00") : new BigDecimal("20000");

        String currencySymbol = AccountUtil.getCurrencySymbol(currency);
        String minimumFormatted = AccountUtil.formatCurrency(minimumDeposit, currency);

        System.out.println("\n" + "=".repeat(50));
        System.out.println("           INITIAL DEPOSIT REQUIRED");
        System.out.println("=".repeat(50));
        System.out.println("💰 Minimum initial deposit: " + minimumFormatted);
        System.out.println("💡 This amount will be your account's starting balance");

        while (true) {
            System.out.print("Enter initial deposit amount: " + currencySymbol);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                showErrorMessage("Amount cannot be empty!");
                continue;
            }

            try {
                BigDecimal amount = new BigDecimal(input);

                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    showErrorMessage("Amount must be greater than 0!");
                    continue;
                }

                if (amount.compareTo(minimumDeposit) < 0) {
                    showErrorMessage("Amount must be at least " + minimumFormatted + "!");
                    continue;
                }

                // For KHR, ensure it's a whole number
                if ("KHR".equals(currency) && amount.scale() > 0) {
                    showErrorMessage("KHR amount cannot have decimal places!");
                    continue;
                }

                System.out.println("✅ Initial deposit: " + AccountUtil.formatCurrency(amount, currency));
                return amount;

            } catch (NumberFormatException e) {
                showErrorMessage("Invalid amount format!");
            }
        }
    }

    /**
     * Get maturity date for Fixed accounts
     */
    private LocalDate getMaturityDate() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("           FIXED ACCOUNT TERM");
        System.out.println("=".repeat(50));
        System.out.println("Select fixed deposit term:");
        System.out.println("1. 6 Months");
        System.out.println("2. 1 Year");
        System.out.println("3. 2 Years");
        System.out.println("4. Custom Date");
        System.out.print("Choose term (1-4): ");

        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            LocalDate today = LocalDate.now();
            LocalDate maturityDate;

            switch (choice) {
                case 1:
                    maturityDate = today.plusMonths(6);
                    break;
                case 2:
                    maturityDate = today.plusYears(1);
                    break;
                case 3:
                    maturityDate = today.plusYears(2);
                    break;
                case 4:
                    maturityDate = getCustomMaturityDate();
                    if (maturityDate == null) return null;
                    break;
                default:
                    showErrorMessage("Invalid choice!");
                    return null;
            }

            System.out.println("✅ Maturity Date: " + maturityDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            System.out.println("⚠️ No withdrawals allowed until maturity date!");
            return maturityDate;

        } catch (NumberFormatException e) {
            showErrorMessage("Invalid selection!");
            return null;
        }
    }

    /**
     * Get custom maturity date
     */
    private LocalDate getCustomMaturityDate() {
        System.out.print("Enter maturity date (YYYY-MM-DD): ");
        String input = scanner.nextLine().trim();

        try {
            LocalDate maturityDate = LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate today = LocalDate.now();

            if (!maturityDate.isAfter(today)) {
                showErrorMessage("Maturity date must be in the future!");
                return null;
            }

            if (maturityDate.isAfter(today.plusYears(10))) {
                showErrorMessage("Maturity date cannot be more than 10 years from now!");
                return null;
            }

            return maturityDate;

        } catch (DateTimeParseException e) {
            showErrorMessage("Invalid date format! Please use YYYY-MM-DD format.");
            return null;
        }
    }

    /**
     * Confirm account creation with all details
     */
    public boolean confirmAccountCreation(AccountCreationData data) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("         CONFIRM ACCOUNT CREATION");
        System.out.println("=".repeat(50));
        System.out.println("Account Type    : " + data.getAccountType());
        System.out.println("Currency        : " + data.getCurrency());
        System.out.println("Initial Deposit : " +
                AccountUtil.formatCurrency(data.getInitialDeposit(), data.getCurrency()));

        if (data.getMaturityDate() != null) {
            System.out.println("Maturity Date   : " +
                    data.getMaturityDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            System.out.println("⚠️ Withdrawals restricted until maturity!");
        }

        System.out.println("=".repeat(50));
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
     * Show account creation success
     */
    public void showAccountCreationSuccess(String accountNumber, String accountName) {
        System.out.println("\n🎉 ACCOUNT CREATED SUCCESSFULLY! 🎉");
        System.out.println("=".repeat(50));
        System.out.println("Account Name: " + accountName);
        System.out.println("Account Number: " + accountNumber);
        System.out.println("Status: Active");
        System.out.println("=".repeat(50));
        System.out.println("💡 Your account is now ready for banking operations!");
    }

    /**
     * Data class for account creation
     */
    public static class AccountCreationData {
        private final String accountType;
        private final String currency;
        private final BigDecimal initialDeposit;
        private final LocalDate maturityDate;

        public AccountCreationData(String accountType, String currency,
                                   BigDecimal initialDeposit, LocalDate maturityDate) {
            this.accountType = accountType;
            this.currency = currency;
            this.initialDeposit = initialDeposit;
            this.maturityDate = maturityDate;
        }

        // Getters
        public String getAccountType() { return accountType; }
        public String getCurrency() { return currency; }
        public BigDecimal getInitialDeposit() { return initialDeposit; }
        public LocalDate getMaturityDate() { return maturityDate; }
    }
}